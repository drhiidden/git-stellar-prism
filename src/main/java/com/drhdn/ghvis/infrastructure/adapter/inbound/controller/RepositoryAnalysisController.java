package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;


import com.drhdn.ghvis.application.handler.GetRepositoryAnalysisQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

/**
 * Controller para análisis completo de repositorios con Event-Driven Architecture.
 * 
 * Expone endpoints para realizar análisis completos de repositorios,
 * incluyendo commits, issues, pull requests y resúmenes técnicos.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class RepositoryAnalysisController {

    // private final CreateRepositoryAnalysisCommandHandler commandHandler; // Reserved for future CQRS commands
    private final GetRepositoryAnalysisQueryHandler queryHandler;

    /**
     * Obtiene análisis completo de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Análisis completo del repositorio
     */
    @GetMapping("/{owner}/{repo}")
    public Mono<ResponseEntity<Map<String, Object>>> getFullAnalysis(
            @PathVariable String owner,
            @PathVariable String repo,
            Principal principal) {
        
        log.info("🔍 Solicitud de análisis completo para {}/{}", owner, repo);
        
        return queryHandler.handleFullQuery(owner, repo, principal)
            .map(ResponseEntity::ok)
            .onErrorResume(throwable -> {
                log.error("❌ Error en análisis completo de {}/{}: {}", owner, repo, throwable.getMessage());
                
                Map<String, Object> errorResponse = Map.of(
                    "error", "Error en análisis completo",
                    "message", throwable.getMessage(),
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString()
                );
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Obtiene análisis personalizado de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param commits Incluir commits
     * @param issues Incluir issues
     * @param pullRequests Incluir pull requests
     * @param technicalSummary Incluir resumen técnico
     * @param principal Usuario autenticado
     * @return Análisis personalizado del repositorio
     */
    @GetMapping("/{owner}/{repo}/custom")
    public Mono<ResponseEntity<Map<String, Object>>> getCustomAnalysis(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "true") boolean commits,
            @RequestParam(defaultValue = "true") boolean issues,
            @RequestParam(defaultValue = "true") boolean pullRequests,
            @RequestParam(defaultValue = "true") boolean technicalSummary,
            Principal principal) {
        
        log.info("🔍 Solicitud de análisis personalizado para {}/{} (Commits: {}, Issues: {}, PRs: {}, Summary: {})", 
            owner, repo, commits, issues, pullRequests, technicalSummary);
        
        return queryHandler.handleCustomQuery(owner, repo, principal, commits, issues, pullRequests, technicalSummary)
            .map(ResponseEntity::ok)
            .onErrorResume(throwable -> {
                log.error("❌ Error en análisis personalizado de {}/{}: {}", owner, repo, throwable.getMessage());
                
                Map<String, Object> errorResponse = Map.of(
                    "error", "Error en análisis personalizado",
                    "message", throwable.getMessage(),
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString()
                );
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Obtiene solo commits de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Análisis de commits
     */
    @GetMapping("/{owner}/{repo}/commits")
    public Mono<ResponseEntity<Map<String, Object>>> getCommitsAnalysis(
            @PathVariable String owner,
            @PathVariable String repo,
            Principal principal) {
        
        log.info("📝 Solicitud de análisis de commits para {}/{}", owner, repo);
        
        return queryHandler.handleCustomQuery(owner, repo, principal, true, false, false, false)
            .map(ResponseEntity::ok)
            .onErrorResume(throwable -> {
                log.error("❌ Error en análisis de commits de {}/{}: {}", owner, repo, throwable.getMessage());
                
                Map<String, Object> errorResponse = Map.of(
                    "error", "Error en análisis de commits",
                    "message", throwable.getMessage(),
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString()
                );
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Obtiene solo issues de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Análisis de issues
     */
    @GetMapping("/{owner}/{repo}/issues")
    public Mono<ResponseEntity<Map<String, Object>>> getIssuesAnalysis(
            @PathVariable String owner,
            @PathVariable String repo,
            Principal principal) {
        
        log.info("🐛 Solicitud de análisis de issues para {}/{}", owner, repo);
        
        return queryHandler.handleCustomQuery(owner, repo, principal, false, true, false, false)
            .map(ResponseEntity::ok)
            .onErrorResume(throwable -> {
                log.error("❌ Error en análisis de issues de {}/{}: {}", owner, repo, throwable.getMessage());
                
                Map<String, Object> errorResponse = Map.of(
                    "error", "Error en análisis de issues",
                    "message", throwable.getMessage(),
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString()
                );
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Obtiene solo pull requests de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Análisis de pull requests
     */
    @GetMapping("/{owner}/{repo}/pull-requests")
    public Mono<ResponseEntity<Map<String, Object>>> getPullRequestsAnalysis(
            @PathVariable String owner,
            @PathVariable String repo,
            Principal principal) {
        
        log.info("🔀 Solicitud de análisis de pull requests para {}/{}", owner, repo);
        
        return queryHandler.handleCustomQuery(owner, repo, principal, false, false, true, false)
            .map(ResponseEntity::ok)
            .onErrorResume(throwable -> {
                log.error("❌ Error en análisis de pull requests de {}/{}: {}", owner, repo, throwable.getMessage());
                
                Map<String, Object> errorResponse = Map.of(
                    "error", "Error en análisis de pull requests",
                    "message", throwable.getMessage(),
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString()
                );
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Obtiene solo resumen técnico de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Resumen técnico
     */
    @GetMapping("/{owner}/{repo}/technical-summary")
    public Mono<ResponseEntity<Map<String, Object>>> getTechnicalSummary(
            @PathVariable String owner,
            @PathVariable String repo,
            Principal principal) {
        
        log.info("🔧 Solicitud de resumen técnico para {}/{}", owner, repo);
        
        return queryHandler.handleCustomQuery(owner, repo, principal, false, false, false, true)
            .map(ResponseEntity::ok)
            .onErrorResume(throwable -> {
                log.error("❌ Error en resumen técnico de {}/{}: {}", owner, repo, throwable.getMessage());
                
                Map<String, Object> errorResponse = Map.of(
                    "error", "Error en resumen técnico",
                    "message", throwable.getMessage(),
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString()
                );
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }
} 