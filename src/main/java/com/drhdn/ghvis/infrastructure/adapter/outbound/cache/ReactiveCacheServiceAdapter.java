package com.drhdn.ghvis.infrastructure.adapter.outbound.cache;

import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.infrastructure.adapter.outbound.error.ReactiveErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Adapter de infraestructura para CacheService con implementación reactiva completa.
 * 
 * Implementa el puerto CacheService con cache en memoria, TTL configurable,
 * estadísticas y manejo de errores reactivo.
 * 
 * @author GitStellarPrism Team
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReactiveCacheServiceAdapter implements CacheService {
    
    private final ReactiveErrorHandler errorHandler;
    
    // Cache en memoria con TTL
    private final Map<String, CacheEntry<Object>> cache = new ConcurrentHashMap<>();
    
    // Configuración desde properties
    @Value("${app.cache.ttl.minutes:30}")
    private long defaultTtlMinutes;
    
    @Value("${app.cache.max-size:1000}")
    private int maxCacheSize;
    
    // Estadísticas de cache
    private final CacheStats stats = new CacheStats();

    @Override
    public <T> Mono<T> getOrFetch(String key, Supplier<Mono<T>> fetchFunction) {
        return getOrFetch(key, fetchFunction, Duration.ofMinutes(defaultTtlMinutes).getSeconds());
    }
    
    @Override
    @SuppressWarnings("unchecked") // Generic cache storage requires unchecked cast
    public <T> Mono<T> getOrFetch(String key, Supplier<Mono<T>> fetchFunction, long ttlSeconds) {
        return Mono.defer(() -> {
            CacheEntry<T> entry = (CacheEntry<T>) cache.get(key);
            
            if (entry != null && !entry.isExpired()) {
                // Cache hit
                stats.incrementHits();
                log.debug("Cache HIT para key: {}", key);
                return Mono.just(entry.getData());
            }
            
            // Cache miss - obtener datos
            stats.incrementMisses();
            log.debug("Cache MISS para key: {}", key);
            
            return fetchFunction.get()
                .doOnNext(data -> {
                    // Guardar en cache
                    CacheEntry<T> newEntry = new CacheEntry<>(data, Instant.now().plusSeconds(ttlSeconds));
                    cache.put(key, (CacheEntry<Object>) newEntry);
                    
                    // Verificar límite de tamaño
                    if (cache.size() > maxCacheSize) {
                        evictOldestEntries();
                    }
                })
                .onErrorResume(errorHandler.handleCacheError())
                .subscribeOn(Schedulers.boundedElastic());
        });
    }
    
    @Override
    public Mono<Void> clear(String pattern) {
        return Mono.fromRunnable(() -> {
            int beforeSize = cache.size();
            cache.entrySet().removeIf(entry -> entry.getKey().contains(pattern));
            int afterSize = cache.size();
            
            log.info("Cache limpiado con patrón '{}': {} entradas removidas", pattern, beforeSize - afterSize);
        });
    }
    
    @Override
    public Mono<Void> clearAll() {
        return Mono.fromRunnable(() -> {
            int size = cache.size();
            cache.clear();
            log.info("Todo el cache limpiado ({} entradas)", size);
        });
    }
    
    @Override
    public Mono<CacheService.CacheStats> getStats() {
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
    
    @Override
    public Mono<Void> warmUp(String owner, String repo, Principal principal) {
        // Implementación de warm-up específica para repositorios
        return Mono.fromRunnable(() -> {
            log.info("Cache warm-up iniciado para {}/{} (usuario: {})", owner, repo, principal.getName());
        })
        .then()
        .doOnSuccess(v -> log.info("Cache warm-up completado para {}/{}", owner, repo))
        .doOnError(error -> log.error("Error en cache warm-up para {}/{}: {}", owner, repo, error.getMessage()));
    }
    
    /**
     * Construye una clave de cache consistente.
     */

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
     * Implementación de CacheStats.
     */
    private static class CacheStats implements CacheService.CacheStats {
        private long hits;
        private long misses;
        private long totalEntries;
        private long validEntries;
        private long expiredEntries;

        public void incrementHits() {
            hits++;
        }

        public void incrementMisses() {
            misses++;
        }

        // Getters y setters
        @Override
        public long getTotalEntries() { return totalEntries; }
        public void setTotalEntries(long totalEntries) { this.totalEntries = totalEntries; }
        
        @Override
        public long getHitCount() { return hits; }
        // Setter removed - not used locally
        
        @Override
        public long getMissCount() { return misses; }
        // Setter removed - not used locally
        
        @Override
        public double getHitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }
        
        @Override
        public long getEvictionCount() { return expiredEntries; }
        // Setter removed - not used locally
        
        @Override
        public long getSize() { return validEntries; }
        // Setter removed - not used locally
        
        @Override
        public long getMaxSize() { return 1000; } // Valor por defecto
        
        // Setters adicionales para compatibilidad
        public void setValidEntries(long validEntries) { this.validEntries = validEntries; }
        public void setExpiredEntries(long expiredEntries) { this.expiredEntries = expiredEntries; }
    }
} 