package com.drhdn.ghvis.service;

import com.drhdn.ghvis.model.Commit;
import com.drhdn.ghvis.model.Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Servicio de caching reactivo moderno para GitStellarPrism.
 * 
 * Características:
 * - Cache en memoria con TTL configurable
 * - Manejo de backpressure
 * - Integración con ReactiveErrorHandler
 * - Estadísticas de cache en tiempo real
 * - Cache warming automático
 * - Eviction policies inteligentes
 * 
 * @author GitStellarPrism Team
 * @version 2.0.0
 */
@Service
@Slf4j
public class ReactiveCacheService {

    private final GithubService githubService;
    private final ReactiveErrorHandler errorHandler;
    
    // Cache en memoria con TTL
    private final Map<String, CacheEntry<Object>> cache = new ConcurrentHashMap<>();
    
    // Configuración desde properties
    @Value("${app.cache.ttl.minutes:30}")
    private long cacheTtlMinutes;
    
    @Value("${app.cache.max-size:1000}")
    private int maxCacheSize;
    
    @Value("${app.cache.enable-stats:true}")
    private boolean enableStats;
    
    // Estadísticas de cache
    private final CacheStats stats = new CacheStats();

    public ReactiveCacheService(GithubService githubService, ReactiveErrorHandler errorHandler) {
        this.githubService = githubService;
        this.errorHandler = errorHandler;
        
        // Limpieza automática de cache expirado
        scheduleCacheCleanup();
    }

    /**
     * Obtiene commits con cache reactivo inteligente.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Flux de commits con cache
     */
    public Flux<Commit> getCachedCommits(String owner, String repo, Principal principal) {
        String cacheKey = buildCacheKey("commits", owner, repo, principal.getName());
        
        return getFromCacheOrFetch(
            cacheKey,
            () -> githubService.getCommits(owner, repo, principal)
                .collectList()
                .map(commits -> {
                    log.debug("Commits obtenidos desde GitHub para {}/{} ({} commits)", 
                             owner, repo, commits.size());
                    return commits;
                }),
            Duration.ofMinutes(cacheTtlMinutes)
        ).flatMapMany(Flux::fromIterable);
    }

    /**
     * Obtiene repositorios del usuario con cache reactivo.
     * 
     * @param principal Usuario autenticado
     * @param detailed Si obtener información detallada
     * @return Flux de repositorios con cache
     */
    public Flux<Repository> getCachedUserRepositories(Principal principal, boolean detailed) {
        String cacheKey = buildCacheKey("repos", principal.getName(), detailed ? "detailed" : "basic");
        
        return getFromCacheOrFetch(
            cacheKey,
            () -> {
                Flux<Repository> repos = detailed ? 
                    githubService.getUserRepositoriesWithDetails(principal) :
                    githubService.getUserRepositories(principal);
                
                return repos.collectList()
                    .map(repositories -> {
                        log.debug("Repositorios obtenidos desde GitHub para {} ({} repos, detailed: {})", 
                                 principal.getName(), repositories.size(), detailed);
                        return repositories;
                    });
            },
            Duration.ofMinutes(cacheTtlMinutes)
        ).flatMapMany(Flux::fromIterable);
    }

    /**
     * Obtiene un commit específico con cache.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param commitSha SHA del commit
     * @param principal Usuario autenticado
     * @return Mono del commit con cache
     */
    public Mono<Commit> getCachedCommit(String owner, String repo, String commitSha, Principal principal) {
        String cacheKey = buildCacheKey("commit", owner, repo, commitSha, principal.getName());
        
        return getFromCacheOrFetch(
            cacheKey,
            () -> githubService.getCommitDetail(owner, repo, commitSha, principal)
                .doOnNext(commit -> log.debug("Commit {} obtenido desde GitHub para {}/{}", 
                                             commitSha, owner, repo)),
            Duration.ofMinutes(cacheTtlMinutes)
        );
    }

    /**
     * Implementación genérica de cache reactivo con TTL.
     * 
     * @param cacheKey Clave del cache
     * @param fetchFunction Función para obtener datos si no están en cache
     * @param ttl Tiempo de vida del cache
     * @param <T> Tipo de datos
     * @return Mono con los datos (desde cache o fetch)
     */
    private <T> Mono<T> getFromCacheOrFetch(String cacheKey, 
                                           java.util.function.Supplier<Mono<T>> fetchFunction, 
                                           Duration ttl) {
        
        return Mono.defer(() -> {
            CacheEntry<T> entry = (CacheEntry<T>) cache.get(cacheKey);
            
            if (entry != null && !entry.isExpired()) {
                // Cache hit
                stats.incrementHits();
                log.debug("Cache HIT para key: {}", cacheKey);
                return Mono.just(entry.getData());
            }
            
            // Cache miss - obtener datos
            stats.incrementMisses();
            log.debug("Cache MISS para key: {}", cacheKey);
            
            return fetchFunction.get()
                .doOnNext(data -> {
                    // Guardar en cache
                    CacheEntry<T> newEntry = new CacheEntry<>(data, Instant.now().plus(ttl));
                    cache.put(cacheKey, (CacheEntry<Object>) newEntry);
                    
                    // Verificar límite de tamaño
                    if (cache.size() > maxCacheSize) {
                        evictOldestEntries();
                    }
                })
                .onErrorResume(errorHandler.handleGithubError());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Limpia el cache para un repositorio específico.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Mono completado
     */
    public Mono<Void> clearRepositoryCache(String owner, String repo) {
        return Mono.fromRunnable(() -> {
            String pattern = buildCacheKey("commits", owner, repo, "");
            cache.entrySet().removeIf(entry -> entry.getKey().startsWith(pattern));
            log.info("Cache limpiado para repositorio: {}/{}", owner, repo);
        });
    }

    /**
     * Limpia todo el cache.
     * 
     * @return Mono completado
     */
    public Mono<Void> clearAllCache() {
        return Mono.fromRunnable(() -> {
            int size = cache.size();
            cache.clear();
            log.info("Todo el cache limpiado ({} entradas)", size);
        });
    }

    /**
     * Obtiene estadísticas del cache.
     * 
     * @return Mono con estadísticas
     */
    public Mono<CacheStats> getCacheStats() {
        return Mono.fromCallable(() -> {
            long validEntries = cache.values().stream()
                .filter(entry -> !entry.isExpired())
                .count();
            
            stats.setTotalEntries(cache.size());
            stats.setValidEntries(validEntries);
            stats.setExpiredEntries(cache.size() - validEntries);
            
            return stats;
        });
    }

    /**
     * Warm-up del cache para repositorios populares.
     * 
     * @param principal Usuario autenticado
     * @return Mono completado
     */
    public Mono<Void> warmUpCache(Principal principal) {
        return getCachedUserRepositories(principal, false)
            .take(5) // Solo los primeros 5 repositorios
            .flatMap(repo -> {
                // Construir el nombre completo usando owner y name
                String fullName = repo.getOwner() + "/" + repo.getName();
                return getCachedCommits(repo.getOwner(), repo.getName(), principal)
                    .then();
            })
            .then()
            .doOnSuccess(v -> log.info("Cache warm-up completado para usuario: {}", principal.getName()));
    }

    /**
     * Construye una clave de cache consistente.
     */
    private String buildCacheKey(String type, String... parts) {
        return String.join(":", type, String.join(":", parts));
    }

    /**
     * Programa la limpieza automática del cache.
     */
    private void scheduleCacheCleanup() {
        Mono.delay(Duration.ofMinutes(5))
            .repeat()
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                tick -> cleanupExpiredEntries(),
                error -> log.error("Error en limpieza automática del cache", error)
            );
    }

    /**
     * Limpia entradas expiradas del cache.
     */
    private void cleanupExpiredEntries() {
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = cache.size();
        
        if (beforeSize != afterSize) {
            log.debug("Limpieza automática: {} entradas expiradas removidas", beforeSize - afterSize);
        }
    }

    /**
     * Evicta las entradas más antiguas cuando se excede el límite.
     */
    private void evictOldestEntries() {
        int toEvict = cache.size() - maxCacheSize + 100; // Mantener 100 entradas de margen
        
        cache.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().getExpiresAt().compareTo(e2.getValue().getExpiresAt()))
            .limit(toEvict)
            .forEach(entry -> cache.remove(entry.getKey()));
        
        log.debug("Eviction: {} entradas removidas del cache", toEvict);
    }

    /**
     * Entrada del cache con TTL.
     */
    private static class CacheEntry<T> {
        private final T data;
        private final Instant expiresAt;

        public CacheEntry(T data, Instant expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }

        public T getData() {
            return data;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Estadísticas del cache.
     */
    public static class CacheStats {
        private long hits;
        private long misses;
        private long totalEntries;
        private long validEntries;
        private long expiredEntries;

        public double getHitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }

        public void incrementHits() {
            hits++;
        }

        public void incrementMisses() {
            misses++;
        }

        // Getters y setters
        public long getHits() { return hits; }
        public void setHits(long hits) { this.hits = hits; }
        
        public long getMisses() { return misses; }
        public void setMisses(long misses) { this.misses = misses; }
        
        public long getTotalEntries() { return totalEntries; }
        public void setTotalEntries(long totalEntries) { this.totalEntries = totalEntries; }
        
        public long getValidEntries() { return validEntries; }
        public void setValidEntries(long validEntries) { this.validEntries = validEntries; }
        
        public long getExpiredEntries() { return expiredEntries; }
        public void setExpiredEntries(long expiredEntries) { this.expiredEntries = expiredEntries; }
    }
} 