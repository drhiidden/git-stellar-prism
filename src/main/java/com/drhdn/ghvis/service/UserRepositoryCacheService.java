package com.drhdn.ghvis.service;

import com.drhdn.ghvis.entity.UserRepoCache;
import com.drhdn.ghvis.model.Repository;
import com.drhdn.ghvis.repository.UserRepoCacheRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Servicio de caché para repositorios del usuario con funcionalidades avanzadas.
 * 
 * Características:
 * - Caché con TTL configurable
 * - Soporte para caché básico y detallado
 * - Limpieza automática de caché expirado
 * - Estadísticas de uso de caché
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryCacheService {

    private final GithubService githubService;
    private final UserRepoCacheRepository userRepoCacheRepository;
    private final ObjectMapper objectMapper;

    // TTL del caché - 30 minutos para repositorios (cambian menos que commits)
    private static final Duration TTL = Duration.ofMinutes(30);
    
    // Tipos de caché
    private static final String CACHE_TYPE_BASIC = "basic";
    private static final String CACHE_TYPE_DETAILED = "detailed";

    /**
     * Obtiene repositorios básicos del usuario con caché.
     */
    public Flux<Repository> getUserRepositories(Principal principal) {
        return getUserRepositoriesWithCache(principal, CACHE_TYPE_BASIC, false);
    }

    /**
     * Obtiene repositorios detallados del usuario con caché.
     */
    public Flux<Repository> getUserRepositoriesDetailed(Principal principal) {
        return getUserRepositoriesWithCache(principal, CACHE_TYPE_DETAILED, true);
    }

    /**
     * Obtiene repositorios sin caché - directamente desde GitHub.
     */
    public Flux<Repository> getUserRepositoriesNoCache(Principal principal, boolean detailed) {
        if (detailed) {
            return githubService.getUserRepositoriesWithDetails(principal);
        } else {
            return githubService.getUserRepositories(principal);
        }
    }

    /**
     * Implementación interna que maneja el caché.
     */
    private Flux<Repository> getUserRepositoriesWithCache(Principal principal, String cacheType, boolean detailed) {
        if (principal == null) {
            return Flux.empty();
        }

        String username = principal.getName();
        
        // Buscar en caché
        UserRepoCache cache = userRepoCacheRepository
            .findByUsernameAndCacheType(username, cacheType)
            .orElse(null);

        if (cache != null && cache.getUpdatedAt() != null &&
                cache.getUpdatedAt().isAfter(Instant.now().minus(TTL))) {
            // Devolver desde caché
            try {
                List<Repository> repositories = objectMapper.readValue(
                    cache.getRepositoriesJson(), 
                    new TypeReference<List<Repository>>() {}
                );
                log.debug("Repositorios {} obtenidos desde caché para usuario: {} (total: {})", 
                         cacheType, username, repositories.size());
                return Flux.fromIterable(repositories);
            } catch (Exception e) {
                log.error("Error al deserializar repositorios del caché para usuario: {}", username, e);
                // Continuar para refrescar desde GitHub
            }
        }

        // Obtener desde GitHub y guardar en caché
        Flux<Repository> repositoriesFlux = detailed ? 
            githubService.getUserRepositoriesWithDetails(principal) :
            githubService.getUserRepositories(principal);

        return repositoriesFlux
            .collectList()
            .doOnNext(repositories -> saveToCache(username, repositories, cacheType))
            .flatMapMany(Flux::fromIterable);
    }

    /**
     * Guarda repositorios en caché.
     */
    private void saveToCache(String username, List<Repository> repositories, String cacheType) {
        try {
            String json = objectMapper.writeValueAsString(repositories);
            
            UserRepoCache cache = UserRepoCache.builder()
                .username(username)
                .repositoriesJson(json)
                .totalCount(repositories.size())
                .cacheType(cacheType)
                .updatedAt(Instant.now())
                .build();
                
            userRepoCacheRepository.save(cache);
            log.debug("Repositorios {} guardados en caché para usuario: {} (total: {})", 
                     cacheType, username, repositories.size());
        } catch (Exception e) {
            log.error("Error guardando repositorios en caché para usuario: {}", username, e);
        }
    }

    /**
     * Limpia el caché de repositorios para un usuario específico.
     */
    public void clearUserCache(String username) {
        try {
            userRepoCacheRepository.deleteById(username);
            log.info("Caché de repositorios limpiado para usuario: {}", username);
        } catch (Exception e) {
            log.error("Error limpiando caché para usuario: {}", username, e);
        }
    }

    /**
     * Limpia todo el caché de repositorios.
     */
    public void clearAllCache() {
        try {
            long count = userRepoCacheRepository.count();
            userRepoCacheRepository.deleteAll();
            log.info("Todo el caché de repositorios limpiado ({} entradas)", count);
        } catch (Exception e) {
            log.error("Error limpiando todo el caché de repositorios", e);
        }
    }

    /**
     * Obtiene estadísticas del caché.
     */
    public Mono<CacheStats> getCacheStats() {
        return Mono.fromCallable(() -> {
            Instant validSince = Instant.now().minus(TTL);
            long validEntries = userRepoCacheRepository.countValidCacheEntries(validSince);
            long totalEntries = userRepoCacheRepository.count();
            List<String> activeUsers = userRepoCacheRepository.findActiveUsers(validSince);
            
            return CacheStats.builder()
                .totalEntries(totalEntries)
                .validEntries(validEntries)
                .expiredEntries(totalEntries - validEntries)
                .activeUsers(activeUsers.size())
                .ttlMinutes(TTL.toMinutes())
                .build();
        });
    }

    /**
     * Limpieza automática de caché expirado cada hora.
     */
    @Scheduled(fixedRate = 3600000) // 1 hora
    public void cleanupExpiredCache() {
        try {
            Instant expiredBefore = Instant.now().minus(TTL.multipliedBy(2)); // TTL * 2 para ser conservador
            userRepoCacheRepository.deleteByUpdatedAtBefore(expiredBefore);
            log.debug("Limpieza automática de caché expirado completada");
        } catch (Exception e) {
            log.error("Error en limpieza automática de caché", e);
        }
    }

    /**
     * Clase para estadísticas de caché.
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheStats {
        private long totalEntries;
        private long validEntries;
        private long expiredEntries;
        private long activeUsers;
        private long ttlMinutes;
    }
} 