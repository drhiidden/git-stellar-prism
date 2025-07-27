package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.domain.port.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controlador para herramientas de desarrollo.
 * Solo disponible en perfil 'dev'.
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("dev")
@Slf4j
public class DevelopmentController {
    
    private final RateLimitService rateLimitService;
    private final CacheService cacheService;
    
    /**
     * Obtiene estadísticas de rate limiting.
     */
    @GetMapping("/rate-limit/stats")
    public Mono<ResponseEntity<RateLimitService.RateLimitStats>> getRateLimitStats() {
        log.info("📊 Solicitando estadísticas de rate limiting");
        return rateLimitService.getStats()
            .map(ResponseEntity::ok)
            .doOnSuccess(stats -> log.info("✅ Estadísticas de rate limiting enviadas"));
    }
    
    /**
     * Obtiene estadísticas del cache.
     */
    @GetMapping("/cache/stats")
    public Mono<ResponseEntity<CacheService.CacheStats>> getCacheStats() {
        log.info("📊 Solicitando estadísticas del cache");
        return cacheService.getStats()
            .map(ResponseEntity::ok)
            .doOnSuccess(stats -> log.info("✅ Estadísticas del cache enviadas"));
    }
    
    /**
     * Limpia el cache.
     */
    @PostMapping("/cache/clear")
    public Mono<ResponseEntity<Map<String, String>>> clearCache() {
        log.info("🧹 Limpiando cache");
        return cacheService.clear("all")
            .then(Mono.just(ResponseEntity.ok(Map.of("message", "Cache limpiado"))))
            .doOnSuccess(result -> log.info("✅ Cache limpiado"));
    }
    
    /**
     * Obtiene información general del estado de desarrollo.
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<Map<String, Object>>> getDevelopmentStatus() {
        log.info("📊 Solicitando estado de desarrollo");
        
        return Mono.zip(
            rateLimitService.getStats(),
            cacheService.getStats()
        ).map(tuple -> {
            Map<String, Object> status = Map.of(
                "rateLimit", tuple.getT1(),
                "cache", tuple.getT2(),
                "timestamp", java.time.Instant.now().toString(),
                "profile", "dev"
            );
            return ResponseEntity.ok(status);
        }).doOnSuccess(status -> log.info("✅ Estado de desarrollo enviado"));
    }
} 