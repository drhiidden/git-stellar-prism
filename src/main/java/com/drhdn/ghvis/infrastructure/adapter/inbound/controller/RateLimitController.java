package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.infrastructure.adapter.outbound.ratelimit.RateLimitServiceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Controlador para gestión del rate limiting.
 * 
 * Proporciona endpoints para:
 * - Estadísticas de rate limiting
 * - Estado de límites por endpoint
 * - Limpieza de rate limits expirados
 * - Configuración de rate limiting
 */
@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
@Slf4j
public class RateLimitController {

    private final RateLimitService rateLimitService;
    private final RateLimitServiceAdapter rateLimitServiceAdapter;

    /**
     * Obtiene estadísticas de rate limiting.
     * 
     * @return Estadísticas detalladas del rate limiting
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<RateLimitService.RateLimitStats>> getRateLimitStats() {
        return rateLimitService.getStats()
            .map(stats -> {
                log.info("Estadísticas de rate limiting solicitadas - Endpoints: {}, Utilización: {:.2f}%", 
                        stats.getTotalEndpoints(), stats.getUtilizationRate() * 100);
                return ResponseEntity.ok(stats);
            })
            .onErrorResume(error -> {
                log.error("Error obteniendo estadísticas de rate limiting", error);
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }

    /**
     * Limpia rate limits expirados.
     * 
     * @return Respuesta de confirmación
     */
    @DeleteMapping("/cleanup")
    public Mono<ResponseEntity<String>> cleanupExpiredRateLimits() {
        return rateLimitService.cleanupExpiredLimits()
            .then(Mono.just(ResponseEntity.ok("Rate limits expirados limpiados")))
            .doOnSuccess(response -> log.info("Limpieza manual de rate limits expirados completada"))
            .onErrorResume(error -> {
                log.error("Error limpiando rate limits expirados", error);
                return Mono.just(ResponseEntity.internalServerError()
                    .body("Error limpiando rate limits: " + error.getMessage()));
            });
    }

    /**
     * Obtiene información de salud del rate limiting.
     * 
     * @return Estado de salud del rate limiting
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<RateLimitHealth>> getRateLimitHealth() {
        return rateLimitService.getStats()
            .map(stats -> {
                boolean healthy = stats.getUtilizationRate() < 0.9 && stats.getActiveEndpoints() > 0;
                String status = healthy ? "HEALTHY" : "DEGRADED";
                
                RateLimitHealth health = RateLimitHealth.builder()
                    .status(status)
                    .totalEndpoints(stats.getTotalEndpoints())
                    .activeEndpoints(stats.getActiveEndpoints())
                    .utilizationRate(stats.getUtilizationRate())
                    .message(healthy ? "Rate limiting funcionando correctamente" : "Rate limiting necesita atención")
                    .build();
                
                return ResponseEntity.ok(health);
            })
            .onErrorResume(error -> {
                log.error("Error obteniendo salud del rate limiting", error);
                RateLimitHealth health = RateLimitHealth.builder()
                    .status("UNHEALTHY")
                    .totalEndpoints(0)
                    .activeEndpoints(0)
                    .utilizationRate(0.0)
                    .message("Error obteniendo estado del rate limiting: " + error.getMessage())
                    .build();
                
                return Mono.just(ResponseEntity.status(503).body(health));
            });
    }

    /**
     * Endpoint para streaming de estadísticas de rate limiting en tiempo real.
     * 
     * @return Stream de estadísticas
     */
    @GetMapping(value = "/stats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ResponseEntity<String>> streamRateLimitStats() {
        return rateLimitService.getStats()
            .map(stats -> {
                String event = String.format("data: {\"totalEndpoints\":%d,\"activeEndpoints\":%d,\"utilizationRate\":%.2f}\n\n",
                    stats.getTotalEndpoints(), stats.getActiveEndpoints(), stats.getUtilizationRate());
                return ResponseEntity.ok(event);
            })
            .onErrorResume(error -> {
                log.error("Error streaming estadísticas de rate limiting", error);
                return Mono.just(ResponseEntity.internalServerError().body(""));
            });
    }

    /**
     * Obtiene información detallada de rate limiting para un endpoint específico.
     * 
     * @param endpoint Endpoint de la API
     * @param username Usuario de GitHub
     * @return Información detallada del rate limit
     */
    @GetMapping("/endpoint/{username}/{endpoint}")
    public Mono<ResponseEntity<EndpointRateLimitInfo>> getEndpointRateLimitInfo(
            @PathVariable String username,
            @PathVariable String endpoint,
            Principal principal) {
        
        return Mono.fromCallable(() -> {
            // Obtener información real de rate limit usando getResetTime()
            RateLimitServiceAdapter.RateLimitInfo rateLimitInfo = 
                rateLimitServiceAdapter.getRateLimitInfo(endpoint, principal);
            
            EndpointRateLimitInfo info;
            if (rateLimitInfo != null) {
                info = EndpointRateLimitInfo.builder()
                    .username(username)
                    .endpoint(endpoint)
                    .isActive(!rateLimitInfo.isExpired(java.time.Instant.now()))
                    .lastUpdated(java.time.Instant.now())
                    .resetTime(java.time.Instant.ofEpochSecond(rateLimitInfo.getResetTime())) // ¡USO DE getResetTime()!
                    .remaining(rateLimitInfo.getRemaining())
                    .limit(rateLimitInfo.getLimit())
                    .message(String.format("Rate limit: %d/%d, reset: %s", 
                        rateLimitInfo.getRemaining(), 
                        rateLimitInfo.getLimit(),
                        java.time.Instant.ofEpochSecond(rateLimitInfo.getResetTime())))
                    .build();
            } else {
                info = EndpointRateLimitInfo.builder()
                    .username(username)
                    .endpoint(endpoint)
                    .isActive(false)
                    .lastUpdated(java.time.Instant.now())
                    .message("No hay información de rate limit para este endpoint")
                    .build();
            }
            
            return ResponseEntity.ok(info);
        })
        .onErrorResume(error -> {
            log.error("Error obteniendo información de rate limit para endpoint: {}/{}", username, endpoint, error);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Clase para representar el estado de salud del rate limiting.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RateLimitHealth {
        private String status;
        private long totalEndpoints;
        private long activeEndpoints;
        private double utilizationRate;
        private String message;
    }

    /**
     * Clase para representar información de rate limit de un endpoint específico.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EndpointRateLimitInfo {
        private String username;
        private String endpoint;
        private boolean isActive;
        private java.time.Instant lastUpdated;
        private java.time.Instant resetTime; // Cuándo se resetean los límites
        private int remaining; // Requests restantes
        private int limit; // Límite total
        private String message;
    }
} 