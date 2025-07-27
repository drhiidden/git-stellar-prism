package com.drhdn.ghvis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Gestor de rate limiting reactivo para GitHub API.
 * 
 * Características:
 * - Rate limiting basado en headers de GitHub API
 * - Backoff exponencial para reintentos
 * - Cache de límites por endpoint
 * - Integración con ReactiveErrorHandler
 * - Métricas de rate limiting
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class RateLimitManager {

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

    public RateLimitManager(ReactiveErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Verifica si se puede hacer una petición al endpoint especificado.
     * 
     * @param endpoint Endpoint de la API
     * @param principal Usuario autenticado
     * @return Mono que emite true si se puede hacer la petición
     */
    public Mono<Boolean> canMakeRequest(String endpoint, java.security.Principal principal) {
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

    /**
     * Actualiza los límites de rate limiting basado en headers de respuesta.
     * 
     * @param endpoint Endpoint de la API
     * @param principal Usuario autenticado
     * @param headers Headers de respuesta de GitHub
     * @return Mono completado
     */
    public Mono<Void> updateRateLimits(String endpoint, java.security.Principal principal, HttpHeaders headers) {
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
            }
        });
    }

    /**
     * Maneja errores de rate limiting y aplica backoff exponencial.
     * 
     * @param endpoint Endpoint de la API
     * @param principal Usuario autenticado
     * @param <T> Tipo de retorno
     * @return Function que maneja errores de rate limiting
     */
    public <T> Function<Throwable, Mono<T>> handleRateLimitError(String endpoint, java.security.Principal principal) {
        return throwable -> {
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException ex = (WebClientResponseException) throwable;
                
                if (ex.getStatusCode().value() == 429) {
                    // Rate limit excedido
                    String retryAfter = ex.getHeaders().getFirst("Retry-After");
                    long waitSeconds = retryAfter != null ? Long.parseLong(retryAfter) : 60;
                    
                    log.warn("Rate limit excedido para endpoint: {} (usuario: {}). Reintentar en {} segundos", 
                            endpoint, principal.getName(), waitSeconds);
                    
                    return Mono.error(new RateLimitExceededException(
                        String.format("Rate limit excedido. Reintenta en %d segundos.", waitSeconds), waitSeconds));
                }
            }
            
            return Mono.error(throwable);
        };
    }

    /**
     * Crea un retry policy con backoff exponencial para rate limiting.
     * 
     * @param endpoint Endpoint de la API
     * @param principal Usuario autenticado
     * @return Retry policy configurado
     */
    public Retry createRetryPolicy(String endpoint, java.security.Principal principal) {
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

    /**
     * Obtiene estadísticas de rate limiting.
     * 
     * @return Mono con estadísticas
     */
    public Mono<RateLimitStats> getRateLimitStats() {
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
            
            return RateLimitStats.builder()
                .totalEndpoints(totalEndpoints)
                .activeEndpoints(activeEndpoints)
                .totalRemaining(totalRemaining)
                .totalLimit(totalLimit)
                .utilizationRate(totalLimit > 0 ? (double) (totalLimit - totalRemaining) / totalLimit : 0.0)
                .build();
        });
    }

    /**
     * Limpia rate limits expirados.
     * 
     * @return Mono completado
     */
    public Mono<Void> cleanupExpiredRateLimits() {
        return Mono.fromRunnable(() -> {
            Instant now = Instant.now();
            rateLimits.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
            log.debug("Limpieza de rate limits expirados completada");
        });
    }

    /**
     * Construye una clave única para el rate limiting.
     */
    private String buildRateLimitKey(String endpoint, java.security.Principal principal) {
        return String.format("%s:%s", principal.getName(), endpoint);
    }

    /**
     * Información de rate limiting para un endpoint.
     */
    private static class RateLimitInfo {
        private final int limit;
        private int remaining;
        private final long resetTime;
        private Instant lastUpdated;

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
     * Estadísticas de rate limiting.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RateLimitStats {
        private long totalEndpoints;
        private long activeEndpoints;
        private long totalRemaining;
        private long totalLimit;
        private double utilizationRate;
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