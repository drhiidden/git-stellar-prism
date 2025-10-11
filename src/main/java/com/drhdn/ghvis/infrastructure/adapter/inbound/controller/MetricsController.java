package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GitHubRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controlador para exponer métricas del sistema.
 * 
 * Proporciona visibilidad sobre:
 * - Rate limit de GitHub API
 * - Uso de caché
 * - Performance
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsController {
    
    private final GitHubRateLimitInterceptor rateLimitInterceptor;
    
    /**
     * Obtiene el estado actual del rate limit de GitHub API.
     * 
     * Útil para:
     * - Dashboard de administración
     * - Alertas automáticas
     * - Debugging
     * 
     * @return Estado completo del rate limit
     */
    @GetMapping("/rate-limit")
    public Mono<ResponseEntity<GitHubRateLimitInterceptor.RateLimitStatus>> getRateLimit() {
        var status = rateLimitInterceptor.getStatus();
        
        log.debug("📊 Rate limit status requested: {} remaining ({} {}%)", 
            status.remaining(), 
            status.getStatusLevel(),
            String.format("%.2f", status.usagePercentage())
        );
        
        return Mono.just(ResponseEntity.ok(status));
    }
    
    /**
     * Health check simplificado basado en rate limit.
     * 
     * Retorna:
     * - 200 OK si hay suficientes requests
     * - 429 Too Many Requests si estamos en zona crítica
     * 
     * @return Health status
     */
    @GetMapping("/health/rate-limit")
    public Mono<ResponseEntity<RateLimitHealth>> getRateLimitHealth() {
        var status = rateLimitInterceptor.getStatus();
        
        RateLimitHealth health = new RateLimitHealth(
            status.isSafe(),
            status.getStatusLevel(),
            status.remaining(),
            status.usagePercentage()
        );
        
        if (status.isCritical()) {
            return Mono.just(ResponseEntity.status(429).body(health));
        } else if (status.isDanger()) {
            return Mono.just(ResponseEntity.status(503).body(health));
        } else {
            return Mono.just(ResponseEntity.ok(health));
        }
    }
    
    /**
     * Record para health check
     */
    public record RateLimitHealth(
        boolean healthy,
        String status,
        int remaining,
        double usagePercentage
    ) {}
}

