package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.usecase.GetRepositoryCommitsUseCase;
import com.drhdn.ghvis.domain.port.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

/**
 * Controller para funcionalidades de resiliencia y Circuit Breaker.
 * 
 * Expone endpoints para monitorear y gestionar la resiliencia del sistema,
 * incluyendo estadísticas de Circuit Breaker y operaciones de reset.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/resilience")
@RequiredArgsConstructor
@Slf4j
public class ResilienceController {

    private final GetRepositoryCommitsUseCase getRepositoryCommitsUseCase;
    // private final CircuitBreakerService circuitBreakerService; // Reserved for circuit breaker monitoring

    /**
     * Obtiene commits de un repositorio con resiliencia completa.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Lista de commits con información de resiliencia
     */
    @GetMapping("/commits/{owner}/{repo}")
    public Mono<ResponseEntity<Map<String, Object>>> getCommitsWithResilience(
            @PathVariable String owner,
            @PathVariable String repo,
            Principal principal) {
        
        log.info("Solicitud de commits con resiliencia para {}/{}", owner, repo);
        
        return getRepositoryCommitsUseCase.execute(owner, repo, principal)
            .collectList()
            .flatMap(commits -> {
                Map<String, Object> response = Map.of(
                    "repository", owner + "/" + repo,
                    "commits", commits,
                    "total_commits", commits.size(),
                    "resilience_enabled", true
                );
                
                return Mono.just(ResponseEntity.ok(response));
            })
            .onErrorResume(throwable -> {
                log.error("Error obteniendo commits con resiliencia para {}/{}: {}", 
                    owner, repo, throwable.getMessage());
                
                Map<String, Object> errorResponse = Map.of(
                    "error", "Error obteniendo commits",
                    "message", throwable.getMessage(),
                    "repository", owner + "/" + repo,
                    "resilience_enabled", true
                );
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Obtiene estadísticas del Circuit Breaker para un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Estadísticas del Circuit Breaker
     */
    @GetMapping("/circuit-breaker/{owner}/{repo}/stats")
    public Mono<ResponseEntity<CircuitBreakerService.CircuitBreakerStats>> getCircuitBreakerStats(
            @PathVariable String owner,
            @PathVariable String repo) {
        
        log.info("Obteniendo estadísticas de Circuit Breaker para {}/{}", owner, repo);
        
        return getRepositoryCommitsUseCase.getCircuitBreakerStats(owner, repo)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .onErrorResume(throwable -> {
                log.error("Error obteniendo estadísticas de Circuit Breaker para {}/{}: {}", 
                    owner, repo, throwable.getMessage());
                
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }

    /**
     * Resetea el Circuit Breaker para un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Respuesta de confirmación
     */
    @PostMapping("/circuit-breaker/{owner}/{repo}/reset")
    public Mono<ResponseEntity<Map<String, Object>>> resetCircuitBreaker(
            @PathVariable String owner,
            @PathVariable String repo) {
        
        log.info("Reseteando Circuit Breaker para {}/{}", owner, repo);
        
        return getRepositoryCommitsUseCase.resetCircuitBreaker(owner, repo)
            .then(Mono.fromCallable(() -> {
                Map<String, Object> response = Map.of(
                    "message", "Circuit Breaker reseteado exitosamente",
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString()
                );
                return ResponseEntity.ok(response);
            }))
            .onErrorResume(throwable -> {
                log.error("Error reseteando Circuit Breaker para {}/{}: {}", 
                    owner, repo, throwable.getMessage());
                
                Map<String, Object> errorResponse = Map.of(
                    "error", "Error reseteando Circuit Breaker",
                    "message", throwable.getMessage(),
                    "repository", owner + "/" + repo
                );
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Obtiene estadísticas generales de resiliencia del sistema.
     * 
     * @return Estadísticas generales
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getResilienceStats() {
        log.info("Obteniendo estadísticas generales de resiliencia");
        
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = Map.of(
                "circuit_breaker_enabled", true,
                "rate_limiting_enabled", true,
                "caching_enabled", true,
                "timestamp", java.time.Instant.now().toString(),
                "version", "1.0.0"
            );
            return ResponseEntity.ok(stats);
        });
    }

    /**
     * Endpoint de health check para resiliencia.
     * 
     * @return Estado de salud del sistema
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "resilience", "ENABLED",
            "timestamp", java.time.Instant.now().toString()
        );
        
        return Mono.just(ResponseEntity.ok(health));
    }
} 