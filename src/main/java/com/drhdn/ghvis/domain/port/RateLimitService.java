package com.drhdn.ghvis.domain.port;

import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.security.Principal;

/**
 * Puerto de salida para servicios de rate limiting.
 * 
 * Define el contrato para operaciones de rate limiting,
 * siguiendo los principios de arquitectura hexagonal.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface RateLimitService {
    
    /**
     * Verifica si se puede hacer una petición al endpoint especificado.
     * 
     * @param endpoint Endpoint de la API
     * @param principal Usuario autenticado
     * @return Mono que emite true si se puede hacer la petición
     */
    Mono<Boolean> canMakeRequest(String endpoint, Principal principal);
    
    /**
     * Actualiza los límites de rate limiting basado en headers de respuesta.
     * 
     * @param endpoint Endpoint de la API
     * @param principal Usuario autenticado
     * @param headers Headers de respuesta
     * @return Mono completado
     */
    Mono<Void> updateLimits(String endpoint, Principal principal, HttpHeaders headers);
    
    /**
     * Crea un retry policy con backoff exponencial para rate limiting.
     * 
     * @param endpoint Endpoint de la API
     * @param principal Usuario autenticado
     * @return Retry policy configurado
     */
    Retry createRetryPolicy(String endpoint, Principal principal);
    
    /**
     * Obtiene estadísticas de rate limiting.
     * 
     * @return Mono con estadísticas
     */
    Mono<RateLimitStats> getStats();
    
    /**
     * Limpia rate limits expirados.
     * 
     * @return Mono completado
     */
    Mono<Void> cleanupExpiredLimits();
    
    /**
     * Estadísticas de rate limiting.
     */
    interface RateLimitStats {
        long getTotalEndpoints();
        long getActiveEndpoints();
        long getTotalRemaining();
        long getTotalLimit();
        double getUtilizationRate();
    }
    
    /**
     * Excepción específica para rate limiting.
     */
    class RateLimitExceededException extends RuntimeException {
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