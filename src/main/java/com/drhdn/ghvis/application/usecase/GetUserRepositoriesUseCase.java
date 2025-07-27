package com.drhdn.ghvis.application.usecase;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.event.UserRepositoriesRequestedEvent;
import com.drhdn.ghvis.domain.event.UserRepositoriesRetrievedEvent;
import com.drhdn.ghvis.domain.event.UserRepositoriesRetrievalFailedEvent;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.EventPublisher;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Collections;

/**
 * Use case para obtener repositorios de un usuario.
 * Maneja cache, rate limiting y eventos de dominio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetUserRepositoriesUseCase {
    
    private final RepositoryRepository repositoryRepository;
    private final CacheService cacheService;
    private final EventPublisher eventPublisher;
    
    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${app.cache.ttl-seconds:3600}")
    private int cacheTtlSeconds;
    
    /**
     * Obtiene todos los repositorios de un usuario.
     * 
     * @param username El nombre de usuario (ignorado, se usa principal)
     * @param principal El principal del usuario autenticado
     * @return Flux con los repositorios del usuario
     */
    public Flux<Repository> execute(String username, Principal principal) {
        String requestId = java.util.UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        log.info("🔍 Iniciando obtención de repositorios para usuario: {} (RequestId: {})", 
            username, requestId);
        
        // Publicar evento de solicitud
        UserRepositoriesRequestedEvent requestEvent = new UserRepositoriesRequestedEvent(
            requestId, username, principal.getName(), startTime, false, "all");
        eventPublisher.publish(requestEvent).subscribe();
        
        String cacheKey = buildCacheKey(username, "all");
        
        if (cacheEnabled) {
            return cacheService.getOrFetch(cacheKey, () -> {
                log.info("🌐 Consultando repositorios frescos para usuario: {}", username);
                return Mono.just(Collections.emptyList())
                    .flatMap(ignored -> repositoryRepository.findByUser(principal).collectList());
            })
            .flatMapMany(Flux::fromIterable)
            .doOnComplete(() -> {
                log.info("✅ Repositorios obtenidos exitosamente para usuario: {} (RequestId: {})", 
                    username, requestId);
                publishSuccessEvent(requestId, username, principal, startTime, "all");
            })
            .doOnError(error -> {
                log.error("❌ Error obteniendo repositorios para usuario: {} (RequestId: {}): {}", 
                    username, requestId, error.getMessage());
                publishFailureEvent(requestId, username, principal, error, startTime, "all");
            });
        } else {
            return repositoryRepository.findByUser(principal)
                .doOnComplete(() -> {
                    log.info("✅ Repositorios obtenidos exitosamente para usuario: {} (RequestId: {})", 
                        username, requestId);
                    publishSuccessEvent(requestId, username, principal, startTime, "all");
                })
                .doOnError(error -> {
                    log.error("❌ Error obteniendo repositorios para usuario: {} (RequestId: {}): {}", 
                        username, requestId, error.getMessage());
                    publishFailureEvent(requestId, username, principal, error, startTime, "all");
                });
        }
    }
    
    /**
     * Obtiene repositorios públicos de un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @return Flux con los repositorios públicos del usuario
     */
    public Flux<Repository> executePublic(String username, Principal principal) {
        String requestId = java.util.UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        log.info("🔍 Iniciando obtención de repositorios públicos para usuario: {} (RequestId: {})", 
            username, requestId);
        
        // Publicar evento de solicitud
        UserRepositoriesRequestedEvent requestEvent = new UserRepositoriesRequestedEvent(
            requestId, username, principal.getName(), startTime, false, "public");
        eventPublisher.publish(requestEvent).subscribe();
        
        String cacheKey = buildCacheKey(username, "public");
        
        if (cacheEnabled) {
            return cacheService.getOrFetch(cacheKey, () -> {
                log.info("🌐 Consultando repositorios públicos frescos para usuario: {}", username);
                // Filtrar solo repositorios públicos
                return repositoryRepository.findByUser(principal)
                    .filter(repo -> !repo.isPrivate())
                    .collectList();
            })
            .flatMapMany(Flux::fromIterable)
            .doOnComplete(() -> {
                log.info("✅ Repositorios públicos obtenidos exitosamente para usuario: {} (RequestId: {})", 
                    username, requestId);
                publishSuccessEvent(requestId, username, principal, startTime, "public");
            })
            .doOnError(error -> {
                log.error("❌ Error obteniendo repositorios públicos para usuario: {} (RequestId: {}): {}", 
                    username, requestId, error.getMessage());
                publishFailureEvent(requestId, username, principal, error, startTime, "public");
            });
        } else {
            return repositoryRepository.findByUser(principal)
                .filter(repo -> !repo.isPrivate())
                .doOnComplete(() -> {
                    log.info("✅ Repositorios públicos obtenidos exitosamente para usuario: {} (RequestId: {})", 
                        username, requestId);
                    publishSuccessEvent(requestId, username, principal, startTime, "public");
                })
                .doOnError(error -> {
                    log.error("❌ Error obteniendo repositorios públicos para usuario: {} (RequestId: {}): {}", 
                        username, requestId, error.getMessage());
                    publishFailureEvent(requestId, username, principal, error, startTime, "public");
                });
        }
    }
    
    /**
     * Obtiene repositorios con detalles completos de un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @return Flux con los repositorios detallados del usuario
     */
    public Flux<Repository> executeWithDetails(String username, Principal principal) {
        String requestId = java.util.UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        log.info("🔍 Iniciando obtención de repositorios detallados para usuario: {} (RequestId: {})", 
            username, requestId);
        
        // Publicar evento de solicitud
        UserRepositoriesRequestedEvent requestEvent = new UserRepositoriesRequestedEvent(
            requestId, username, principal.getName(), startTime, true, "detailed");
        eventPublisher.publish(requestEvent).subscribe();
        
        String cacheKey = buildCacheKey(username, "detailed");
        
        if (cacheEnabled) {
            return cacheService.getOrFetch(cacheKey, () -> {
                log.info("🌐 Consultando repositorios detallados frescos para usuario: {}", username);
                // TODO: Implementar enriquecimiento de repositorios con detalles
                return repositoryRepository.findByUser(principal).collectList();
            })
            .flatMapMany(Flux::fromIterable)
            .doOnComplete(() -> {
                log.info("✅ Repositorios detallados obtenidos exitosamente para usuario: {} (RequestId: {})", 
                    username, requestId);
                publishSuccessEvent(requestId, username, principal, startTime, "detailed");
            })
            .doOnError(error -> {
                log.error("❌ Error obteniendo repositorios detallados para usuario: {} (RequestId: {}): {}", 
                    username, requestId, error.getMessage());
                publishFailureEvent(requestId, username, principal, error, startTime, "detailed");
            });
        } else {
            return repositoryRepository.findByUser(principal)
                .doOnComplete(() -> {
                    log.info("✅ Repositorios detallados obtenidos exitosamente para usuario: {} (RequestId: {})", 
                        username, requestId);
                    publishSuccessEvent(requestId, username, principal, startTime, "detailed");
                })
                .doOnError(error -> {
                    log.error("❌ Error obteniendo repositorios detallados para usuario: {} (RequestId: {}): {}", 
                        username, requestId, error.getMessage());
                    publishFailureEvent(requestId, username, principal, error, startTime, "detailed");
                });
        }
    }
    
    /**
     * Limpia el cache de repositorios de un usuario.
     * 
     * @param username El nombre de usuario
     */
    public void clearCache(String username) {
        if (cacheEnabled) {
            String[] cacheKeys = {
                buildCacheKey(username, "all"),
                buildCacheKey(username, "public"),
                buildCacheKey(username, "detailed")
            };
            
            for (String cacheKey : cacheKeys) {
                cacheService.clear(cacheKey);
            }
            
            log.info("🧹 Cache limpiado para usuario: {}", username);
        }
    }
    
    private String buildCacheKey(String username, String type) {
        return String.format("user:%s:repositories:%s", username, type);
    }
    
    private void publishSuccessEvent(String requestId, String username, Principal principal, 
                                   Instant startTime, String type) {
        UserRepositoriesRetrievedEvent successEvent = new UserRepositoriesRetrievedEvent(
            requestId, username, principal.getName(), 
            java.time.Duration.between(startTime, Instant.now()).toMillis(),
            type, false, "success");
        eventPublisher.publish(successEvent).subscribe();
    }
    
    private void publishFailureEvent(String requestId, String username, Principal principal, 
                                   Throwable error, Instant startTime, String type) {
        UserRepositoriesRetrievalFailedEvent failureEvent = new UserRepositoriesRetrievalFailedEvent(
            requestId, username, principal.getName(), 
            java.time.Duration.between(startTime, Instant.now()).toMillis(),
            type, error.getMessage(), false, "error");
        eventPublisher.publish(failureEvent).subscribe();
    }
} 