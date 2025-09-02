package com.drhdn.ghvis.infrastructure.adapter.outbound.ratelimit;

import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.infrastructure.adapter.outbound.error.ReactiveErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter de infraestructura para RateLimitService con implementación reactiva completa.
 * 
 * Implementa el puerto RateLimitService con rate limiting basado en headers de GitHub API,
 * backoff exponencial para reintentos y métricas de rate limiting.
 * 
 * @author GitStellarPrism Team
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitServiceAdapter implements RateLimitService {
    
    private final ReactiveErrorHandler errorHandler;
    
    // Cache de límites por endpoint
    private final Map<String, RateLimitInfo> rateLimits = new ConcurrentHashMap<>();
    
    // Configuración desde properties
    @Value("${app.rate-limit.requests-per-hour:5000}")
    private int defaultRequestsPerHour;
    
    @Value("${app.rate-limit.burst-size:100}")
    private int defaultBurstSize;
    
    @Value("${app.rate-limit.enable-backoff:true}")
    private boolean enableBackoff;
    
    @Value("${app.rate-limit.max-retries:3}")
    private int maxRetries;

    @Override
    public Mono<Boolean> canMakeRequest(String endpoint, Principal principal) {
        String key = buildRateLimitKey(endpoint, principal);
        RateLimitInfo info = rateLimits.get(key);
        
        if (info == null) {
            // Primera petición, permitir
            return Mono.just(true);
        }
        
        Instant now = Instant.now();
        if (info.isExpired(now)) {
            // Reset del rate limit
            info.reset(now);
            return Mono.just(true);
        }
        
        if (info.hasRemainingRequests()) {
            info.decrementRequests();
            return Mono.just(true);
        }
        
        // Rate limit excedido
        log.warn("Rate limit excedido para endpoint: {} (usuario: {})", endpoint, principal.getName());
        return Mono.just(false);
    }

    @Override
    public Mono<Void> updateLimits(String endpoint, Principal principal, HttpHeaders headers) {
        return Mono.fromRunnable(() -> {
            String key = buildRateLimitKey(endpoint, principal);
            
            try {
                int limit = Integer.parseInt(headers.getFirst("X-RateLimit-Limit"));
                int remaining = Integer.parseInt(headers.getFirst("X-RateLimit-Remaining"));
                long resetTime = Long.parseLong(headers.getFirst("X-RateLimit-Reset"));
                
                RateLimitInfo info = new RateLimitInfo(limit, remaining, resetTime);
                rateLimits.put(key, info);
                
                log.debug("Rate limits actualizados para {}: {}/{} (reset: {})", 
                         endpoint, remaining, limit, Instant.ofEpochSecond(resetTime));
                
            } catch (NumberFormatException e) {
                log.warn("Error parseando headers de rate limit para endpoint: {}", endpoint);
                // Usar errorHandler para manejo consistente de errores
                errorHandler.handleGithubError().apply(e)
                    .doOnError(err -> log.error("Error procesando rate limit headers: {}", err.getMessage()))
                    .subscribe();
            }
        });
    }

    @Override
    public Retry createRetryPolicy(String endpoint, Principal principal) {
        return Retry.backoff(maxRetries, Duration.ofSeconds(1))
            .filter(throwable -> {
                if (throwable instanceof WebClientResponseException) {
                    WebClientResponseException ex = (WebClientResponseException) throwable;
                    return ex.getStatusCode().value() == 429;
                }
                return false;
            })
            .doBeforeRetry(retrySignal -> {
                log.info("Reintentando petición a {} después de rate limit (intento {}/{})", 
                        endpoint, retrySignal.totalRetries() + 1, maxRetries);
            })
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                log.error("Rate limit persistente para endpoint: {} después de {} intentos", 
                         endpoint, maxRetries);
                return new RateLimitExceededException(
                    "Rate limit persistente después de múltiples intentos", 0);
            });
    }

    @Override
    public Mono<RateLimitStats> getStats() {
        return Mono.fromCallable(() -> {
            long totalEndpoints = rateLimits.size();
            long activeEndpoints = rateLimits.values().stream()
                .filter(info -> !info.isExpired(Instant.now()))
                .count();
            
            long totalRemaining = rateLimits.values().stream()
                .mapToLong(RateLimitInfo::getRemaining)
                .sum();
            
            long totalLimit = rateLimits.values().stream()
                .mapToLong(RateLimitInfo::getLimit)
                .sum();
            
            return new RateLimitStats() {
                @Override
                public long getTotalEndpoints() { return totalEndpoints; }
                
                @Override
                public long getActiveEndpoints() { return activeEndpoints; }
                
                @Override
                public long getTotalRemaining() { return totalRemaining; }
                
                @Override
                public long getTotalLimit() { return totalLimit; }
                
                @Override
                public double getUtilizationRate() { 
                    return totalLimit > 0 ? (double) (totalLimit - totalRemaining) / totalLimit : 0.0; 
                }
            };
        });
    }

    @Override
    public Mono<Void> cleanupExpiredLimits() {
        return Mono.fromRunnable(() -> {
            Instant now = Instant.now();
            rateLimits.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
            log.debug("Limpieza de rate limits expirados completada");
        });
    }
    
    /**
     * Obtiene información específica de rate limit para un endpoint.
     * 
     * @param endpoint Endpoint de la API
     * @param principal Usuario autenticado
     * @return Información de rate limit o null si no existe
     */
    public RateLimitInfo getRateLimitInfo(String endpoint, Principal principal) {
        String key = buildRateLimitKey(endpoint, principal);
        return rateLimits.get(key);
    }

    /**
     * Construye una clave única para el rate limiting.
     */
    private String buildRateLimitKey(String endpoint, Principal principal) {
        return String.format("%s:%s", principal.getName(), endpoint);
    }

    /**
     * Información de rate limiting para un endpoint.
     */
    public static class RateLimitInfo {
        private final int limit;
        private int remaining;
        private final long resetTime;
        private Instant lastUpdated; // Reserved for future rate limiting metrics

        public RateLimitInfo(int limit, int remaining, long resetTime) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
            this.lastUpdated = Instant.now();
        }

        public boolean hasRemainingRequests() {
            return remaining > 0;
        }

        public void decrementRequests() {
            if (remaining > 0) {
                remaining--;
            }
        }

        public boolean isExpired(Instant now) {
            return now.isAfter(Instant.ofEpochSecond(resetTime));
        }

        public void reset(Instant now) {
            this.remaining = this.limit;
            this.lastUpdated = now;
        }

        public int getRemaining() { return remaining; }
        public int getLimit() { return limit; }
        public long getResetTime() { return resetTime; }
    }

    /**
     * Excepción específica para rate limiting.
     */
    public static class RateLimitExceededException extends RuntimeException {
        private final long waitSeconds;

        public RateLimitExceededException(String message, long waitSeconds) {
            super(message);
            this.waitSeconds = waitSeconds;
        }

        public long getWaitSeconds() {
            return waitSeconds;
        }
    }
} 