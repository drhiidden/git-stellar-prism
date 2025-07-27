package com.drhdn.ghvis.application.usecase;

import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.event.CommitRetrievalCompletedEvent;
import com.drhdn.ghvis.domain.event.CommitRetrievalFailedEvent;
import com.drhdn.ghvis.domain.event.CommitRetrievalRequestedEvent;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.CircuitBreakerService;
import com.drhdn.ghvis.domain.port.CommitRepository;
import com.drhdn.ghvis.domain.port.EventPublisher;
import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.domain.service.CommitAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

/**
 * Use Case unificado para obtener commits de un repositorio.
 * 
 * Implementa Event-Driven Architecture con soporte opcional para resiliencia.
 * Elimina la duplicación entre Use Cases y proporciona una solución elegante
 * y escalable.
 * 
 * @author GitStellarPrism Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryCommitsUseCase {
    
    private final CommitRepository commitRepository;
    private final CacheService cacheService;
    private final RateLimitService rateLimitService;
    private final CircuitBreakerService circuitBreakerService;
    private final CommitAnalysisService commitAnalysisService;
    private final EventPublisher eventPublisher;
    
    @Value("${app.resilience.enabled:true}")
    private boolean resilienceEnabled;
    
    /**
     * Ejecuta el caso de uso para obtener commits con Event-Driven Architecture.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Flux de commits del repositorio
     */
    public Flux<Commit> execute(String owner, String repo, Principal principal) {
        String requestId = java.util.UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        // Publicar evento de solicitud
        CommitRetrievalRequestedEvent requestEvent = new CommitRetrievalRequestedEvent(
            owner, repo, principal, resilienceEnabled);
        
        return eventPublisher.publish(requestEvent)
            .then(Mono.fromCallable(() -> {
                String endpoint = buildEndpoint(owner, repo);
                String cacheKey = buildCacheKey(owner, repo, principal);
                
                log.info("Obteniendo commits para {}/{} (resilience: {})", 
                    owner, repo, resilienceEnabled);
                
                return rateLimitService.canMakeRequest(endpoint, principal)
                    .flatMapMany(canMake -> {
                        if (!canMake) {
                            return handleRateLimitExceeded(requestId, owner, repo, principal, startTime);
                        }
                        
                        return executeCommitRetrieval(requestId, owner, repo, principal, cacheKey, startTime);
                    });
            }))
            .flatMapMany(flux -> flux)
            .doOnError(error -> handleError(requestId, owner, repo, principal, error, startTime));
    }
    
    /**
     * Ejecuta la obtención de commits con la estrategia apropiada.
     */
    private Flux<Commit> executeCommitRetrieval(String requestId, String owner, String repo, 
                                              Principal principal, String cacheKey, Instant startTime) {
        if (resilienceEnabled) {
            return executeWithResilience(requestId, owner, repo, principal, cacheKey, startTime);
        } else {
            return executeWithoutResilience(requestId, owner, repo, principal, cacheKey, startTime);
        }
    }
    
    /**
     * Ejecuta con resiliencia completa (Circuit Breaker + Cache).
     */
    private Flux<Commit> executeWithResilience(String requestId, String owner, String repo, 
                                             Principal principal, String cacheKey, Instant startTime) {
        String circuitBreakerName = String.format("github-api-%s-%s", owner, repo);
        
        return cacheService.getOrFetch(cacheKey, () -> 
            circuitBreakerService.executeWithCircuitBreaker(
                circuitBreakerName,
                commitRepository.findByRepository(owner, repo, principal).collectList(),
                getFallbackCommits(owner, repo, principal).collectList()
            )
        )
        .flatMapMany(commits -> {
            publishCompletionEvent(requestId, owner, repo, principal, commits.size(), startTime, false, true, "api");
            return Flux.fromIterable(commits);
        });
    }
    
    /**
     * Ejecuta sin resiliencia (solo Cache + Rate Limiting).
     */
    private Flux<Commit> executeWithoutResilience(String requestId, String owner, String repo, 
                                                Principal principal, String cacheKey, Instant startTime) {
        return cacheService.getOrFetch(cacheKey, 
            () -> commitRepository.findByRepository(owner, repo, principal).collectList())
        .flatMapMany(commits -> {
            publishCompletionEvent(requestId, owner, repo, principal, commits.size(), startTime, false, false, "api");
            return Flux.fromIterable(commits);
        });
    }
    
    /**
     * Maneja el caso cuando se excede el rate limit.
     */
    private Flux<Commit> handleRateLimitExceeded(String requestId, String owner, String repo, 
                                               Principal principal, Instant startTime) {
        log.warn("Rate limit excedido para {}/{}", owner, repo);
        
        publishFailureEvent(requestId, owner, repo, principal, 
            "Rate limit excedido", "RATE_LIMIT", startTime, "rate_limit");
        
        return Flux.error(new RateLimitService.RateLimitExceededException(
            "Rate limit excedido para commits", 60));
    }
    
    /**
     * Maneja errores generales.
     */
    private void handleError(String requestId, String owner, String repo, 
                           Principal principal, Throwable error, Instant startTime) {
        log.error("Error obteniendo commits para {}/{}: {}", owner, repo, error.getMessage());
        
        publishFailureEvent(requestId, owner, repo, principal, 
            error.getMessage(), error.getClass().getSimpleName(), startTime, "api_error");
    }
    
    /**
     * Obtiene commits de fallback cuando el Circuit Breaker está abierto.
     */
    private Flux<Commit> getFallbackCommits(String owner, String repo, Principal principal) {
        log.info("Usando fallback para {}/{} - Circuit Breaker abierto", owner, repo);
        
        Commit fallbackCommit = Commit.builder()
            .hash("fallback-commit")
            .message("⚠️ Servicio temporalmente no disponible - Circuit Breaker activo")
            .author("GitStellarPrism System")
            .authorEmail("system@ghvis.local")
            .timestamp(Instant.now())
            .branch("main")
            .build();
        
        return Flux.just(fallbackCommit);
    }
    
    /**
     * Publica evento de completación.
     */
    private void publishCompletionEvent(String requestId, String owner, String repo, 
                                      Principal principal, int commitCount, Instant startTime,
                                      boolean fromCache, boolean resilienceUsed, String source) {
        long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
        
        CommitRetrievalCompletedEvent event = new CommitRetrievalCompletedEvent(
            requestId, owner, repo, principal.getName(), commitCount, durationMs, 
            fromCache, resilienceUsed, source);
        
        eventPublisher.publish(event).subscribe();
    }
    
    /**
     * Publica evento de fallo.
     */
    private void publishFailureEvent(String requestId, String owner, String repo, 
                                   Principal principal, String errorMessage, String errorType,
                                   Instant startTime, String failureReason) {
        long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
        
        CommitRetrievalFailedEvent event = new CommitRetrievalFailedEvent(
            requestId, owner, repo, principal.getName(), errorMessage, errorType,
            durationMs, resilienceEnabled, failureReason);
        
        eventPublisher.publish(event).subscribe();
    }
    
    /**
     * Construye el endpoint para rate limiting.
     */
    private String buildEndpoint(String owner, String repo) {
        return String.format("/repos/%s/%s/commits", owner, repo);
    }
    
    /**
     * Construye la clave del cache.
     */
    private String buildCacheKey(String owner, String repo, Principal principal) {
        return String.format("commits:%s:%s:%s", principal.getName(), owner, repo);
    }
    
    /**
     * Analiza los commits obtenidos usando el Domain Service.
     */
    public java.util.Map<String, Object> analyzeCommits(List<Commit> commits) {
        return commitAnalysisService.analyzeCommitFrequency(commits);
    }
    
    /**
     * Obtiene estadísticas del Circuit Breaker (solo si resiliencia está habilitada).
     */
    public reactor.core.publisher.Mono<CircuitBreakerService.CircuitBreakerStats> getCircuitBreakerStats(String owner, String repo) {
        if (!resilienceEnabled) {
            return reactor.core.publisher.Mono.empty();
        }
        
        String circuitBreakerName = String.format("github-api-%s-%s", owner, repo);
        return circuitBreakerService.getStats(circuitBreakerName);
    }
    
    /**
     * Resetea el Circuit Breaker (solo si resiliencia está habilitada).
     */
    public reactor.core.publisher.Mono<Void> resetCircuitBreaker(String owner, String repo) {
        if (!resilienceEnabled) {
            return reactor.core.publisher.Mono.empty();
        }
        
        String circuitBreakerName = String.format("github-api-%s-%s", owner, repo);
        return circuitBreakerService.reset(circuitBreakerName);
    }
} 