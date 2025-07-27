package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.service.ReactiveCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Controlador para gestión del cache reactivo.
 * 
 * Proporciona endpoints para:
 * - Estadísticas del cache
 * - Limpieza de cache
 * - Warm-up del cache
 * - Gestión de TTL
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final ReactiveCacheService reactiveCacheService;

    /**
     * Obtiene estadísticas del cache.
     * 
     * @return Estadísticas detalladas del cache
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<ReactiveCacheService.CacheStats>> getCacheStats() {
        return reactiveCacheService.getCacheStats()
            .map(stats -> {
                log.info("Estadísticas del cache solicitadas - Hit Rate: {:.2f}%, Entradas: {}", 
                        stats.getHitRate() * 100, stats.getTotalEntries());
                return ResponseEntity.ok(stats);
            })
            .onErrorResume(error -> {
                log.error("Error obteniendo estadísticas del cache", error);
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }

    /**
     * Limpia el cache para un repositorio específico.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Respuesta de confirmación
     */
    @DeleteMapping("/repository/{owner}/{repo}")
    public Mono<ResponseEntity<String>> clearRepositoryCache(
            @PathVariable String owner,
            @PathVariable String repo) {
        
        return reactiveCacheService.clearRepositoryCache(owner, repo)
            .then(Mono.just(ResponseEntity.ok("Cache limpiado para " + owner + "/" + repo)))
            .doOnSuccess(response -> log.info("Cache limpiado manualmente para {}/{}", owner, repo))
            .onErrorResume(error -> {
                log.error("Error limpiando cache para {}/{}", owner, repo, error);
                return Mono.just(ResponseEntity.internalServerError()
                    .body("Error limpiando cache: " + error.getMessage()));
            });
    }

    /**
     * Limpia todo el cache.
     * 
     * @return Respuesta de confirmación
     */
    @DeleteMapping("/all")
    public Mono<ResponseEntity<String>> clearAllCache() {
        return reactiveCacheService.clearAllCache()
            .then(Mono.just(ResponseEntity.ok("Todo el cache ha sido limpiado")))
            .doOnSuccess(response -> log.info("Todo el cache limpiado manualmente"))
            .onErrorResume(error -> {
                log.error("Error limpiando todo el cache", error);
                return Mono.just(ResponseEntity.internalServerError()
                    .body("Error limpiando cache: " + error.getMessage()));
            });
    }

    /**
     * Realiza warm-up del cache para el usuario autenticado.
     * 
     * @param principal Usuario autenticado
     * @return Respuesta de confirmación
     */
    @PostMapping("/warmup")
    public Mono<ResponseEntity<String>> warmUpCache(@AuthenticationPrincipal Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401).body("Usuario no autenticado"));
        }

        return reactiveCacheService.warmUpCache(principal)
            .then(Mono.just(ResponseEntity.ok("Cache warm-up completado para " + principal.getName())))
            .doOnSuccess(response -> log.info("Cache warm-up completado para usuario: {}", principal.getName()))
            .onErrorResume(error -> {
                log.error("Error en warm-up del cache para usuario: {}", principal.getName(), error);
                return Mono.just(ResponseEntity.internalServerError()
                    .body("Error en warm-up: " + error.getMessage()));
            });
    }

    /**
     * Obtiene información de salud del cache.
     * 
     * @return Estado de salud del cache
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<CacheHealth>> getCacheHealth() {
        return reactiveCacheService.getCacheStats()
            .map(stats -> {
                boolean healthy = stats.getHitRate() > 0.5 && stats.getValidEntries() > 0;
                String status = healthy ? "HEALTHY" : "DEGRADED";
                
                CacheHealth health = CacheHealth.builder()
                    .status(status)
                    .hitRate(stats.getHitRate())
                    .totalEntries(stats.getTotalEntries())
                    .validEntries(stats.getValidEntries())
                    .message(healthy ? "Cache funcionando correctamente" : "Cache necesita atención")
                    .build();
                
                return ResponseEntity.ok(health);
            })
            .onErrorResume(error -> {
                log.error("Error obteniendo salud del cache", error);
                CacheHealth health = CacheHealth.builder()
                    .status("UNHEALTHY")
                    .hitRate(0.0)
                    .totalEntries(0)
                    .validEntries(0)
                    .message("Error obteniendo estado del cache: " + error.getMessage())
                    .build();
                
                return Mono.just(ResponseEntity.status(503).body(health));
            });
    }

    /**
     * Endpoint para streaming de estadísticas del cache en tiempo real.
     * 
     * @return Stream de estadísticas
     */
    @GetMapping(value = "/stats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<ResponseEntity<String>> streamCacheStats() {
        return reactiveCacheService.getCacheStats()
            .map(stats -> {
                String event = String.format("data: {\"hitRate\":%.2f,\"totalEntries\":%d,\"validEntries\":%d}\n\n",
                    stats.getHitRate(), stats.getTotalEntries(), stats.getValidEntries());
                return ResponseEntity.ok(event);
            })
            .onErrorResume(error -> {
                log.error("Error streaming estadísticas del cache", error);
                return Mono.just(ResponseEntity.internalServerError().body(""));
            });
    }

    /**
     * Clase para representar el estado de salud del cache.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CacheHealth {
        private String status;
        private double hitRate;
        private long totalEntries;
        private long validEntries;
        private String message;
    }
} 