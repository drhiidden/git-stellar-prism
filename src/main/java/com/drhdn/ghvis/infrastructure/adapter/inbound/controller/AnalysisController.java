package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.handler.GetRepositoryAnalysisQueryHandler;
import com.drhdn.ghvis.application.query.GetRepositoryAnalysisQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

/**
 * Controlador REST para análisis de repositorios y generación de resúmenes.
 * Implementa CQRS usando queries y handlers de forma consistente.
 * 
 * Endpoints disponibles:
 * - GET /api/analysis/analyze?repo=owner/repo - Análisis básico (lenguajes)
 * - GET /api/analysis/analyze/custom?repo=owner/repo&options - Análisis personalizado
 * - GET /api/analysis/analyze/full?repo=owner/repo - Análisis completo
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
@Slf4j
public class AnalysisController {

    private final GetRepositoryAnalysisQueryHandler repositoryAnalysisQueryHandler;

    /**
     * Analiza un repositorio para obtener distribución de lenguajes.
     * Endpoint básico consumido por summaryEditor.js
     * 
     * @param repoParam Repositorio en formato "owner/repo"
     * @param principal Usuario autenticado
     * @return Análisis básico del repositorio (solo lenguajes)
     */
    @GetMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> analyzeRepository(
            @RequestParam("repo") String repoParam, 
            Principal principal) {
        
        log.info("🔍 Solicitando análisis básico para repositorio: {}", repoParam);
        
        return validateAndParseRepository(repoParam)
            .flatMap(parts -> {
                String owner = parts[0];
                String repo = parts[1];
                
                log.info("📊 Ejecutando análisis básico para: {}/{}", owner, repo);
                
                // Crear query básica usando CQRS
                GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createBasicAnalysis(
                    owner, repo, principal);
                
                return repositoryAnalysisQueryHandler.handle(query)
                    .map(ResponseEntity::ok)
                    .doOnSuccess(response -> log.info("✅ Análisis básico completado para: {}/{} (QueryId: {})", 
                        owner, repo, query.getQueryId()))
                    .doOnError(error -> log.error("❌ Error en análisis básico para {}/{} (QueryId: {}): {}", 
                        owner, repo, query.getQueryId(), error.getMessage()));
            })
            .onErrorResume(this::handleAnalysisError);
    }
    
    /**
     * Analiza un repositorio con opciones personalizadas.
     * 
     * @param repoParam Repositorio en formato "owner/repo"
     * @param includeLanguages Si incluir distribución de lenguajes
     * @param includeTechnologies Si incluir detección de tecnologías
     * @param includeStructure Si incluir estructura del proyecto
     * @param includeSummary Si incluir resumen técnico
     * @param principal Usuario autenticado
     * @return Análisis personalizado del repositorio
     */
    @GetMapping(value = "/analyze/custom", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> analyzeRepositoryCustom(
            @RequestParam("repo") String repoParam,
            @RequestParam(value = "languages", defaultValue = "true") boolean includeLanguages,
            @RequestParam(value = "technologies", defaultValue = "false") boolean includeTechnologies,
            @RequestParam(value = "structure", defaultValue = "false") boolean includeStructure,
            @RequestParam(value = "summary", defaultValue = "false") boolean includeSummary,
            Principal principal) {
        
        log.info("🔍 Solicitando análisis personalizado para repositorio: {}", repoParam);
        
        return validateAndParseRepository(repoParam)
            .flatMap(parts -> {
                String owner = parts[0];
                String repo = parts[1];
                
                log.info("📊 Ejecutando análisis personalizado para: {}/{} (L:{}, T:{}, S:{}, Sum:{})", 
                    owner, repo, includeLanguages, includeTechnologies, includeStructure, includeSummary);
                
                // Crear query personalizada usando CQRS
                GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createCustomAnalysis(
                    owner, repo, principal, 
                    includeLanguages, includeTechnologies, 
                    includeStructure, includeSummary);
                
                return repositoryAnalysisQueryHandler.handle(query)
                    .map(ResponseEntity::ok)
                    .doOnSuccess(response -> log.info("✅ Análisis personalizado completado para: {}/{} (QueryId: {})", 
                        owner, repo, query.getQueryId()))
                    .doOnError(error -> log.error("❌ Error en análisis personalizado para {}/{} (QueryId: {}): {}", 
                        owner, repo, query.getQueryId(), error.getMessage()));
            })
            .onErrorResume(this::handleAnalysisError);
    }
    
    /**
     * Analiza un repositorio de forma completa (todos los componentes).
     * 
     * @param repoParam Repositorio en formato "owner/repo"
     * @param principal Usuario autenticado
     * @return Análisis completo del repositorio
     */
    @GetMapping(value = "/analyze/full", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> analyzeRepositoryFull(
            @RequestParam("repo") String repoParam,
            Principal principal) {
        
        log.info("🔍 Solicitando análisis completo para repositorio: {}", repoParam);
        
        return validateAndParseRepository(repoParam)
            .flatMap(parts -> {
                String owner = parts[0];
                String repo = parts[1];
                
                log.info("📊 Ejecutando análisis completo para: {}/{}", owner, repo);
                
                // Crear query completa usando CQRS
                GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createFullAnalysis(
                    owner, repo, principal);
                
                return repositoryAnalysisQueryHandler.handle(query)
                    .map(ResponseEntity::ok)
                    .doOnSuccess(response -> log.info("✅ Análisis completo finalizado para: {}/{} (QueryId: {})", 
                        owner, repo, query.getQueryId()))
                    .doOnError(error -> log.error("❌ Error en análisis completo para {}/{} (QueryId: {}): {}", 
                        owner, repo, query.getQueryId(), error.getMessage()));
            })
            .onErrorResume(this::handleAnalysisError);
    }
    
    /**
     * Obtiene estadísticas de análisis de repositorio (cache, performance, etc.).
     * Endpoint para monitoreo y debug.
     * 
     * @param repoParam Repositorio en formato "owner/repo"
     * @param principal Usuario autenticado
     * @return Estadísticas del análisis
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getAnalysisStats(
            @RequestParam("repo") String repoParam,
            Principal principal) {
        
        log.info("📊 Solicitando estadísticas de análisis para repositorio: {}", repoParam);
        
        return validateAndParseRepository(repoParam)
            .flatMap(parts -> {
                String owner = parts[0];
                String repo = parts[1];
                
                // Crear estadísticas básicas
                Map<String, Object> stats = Map.of(
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString(),
                    "user", principal != null ? principal.getName() : "anonymous",
                    "cache_enabled", true,
                    "last_analysis", "N/A"
                );
                
                log.info("✅ Estadísticas obtenidas para: {}/{}", owner, repo);
                return Mono.just(ResponseEntity.ok(stats));
            })
            .onErrorResume(this::handleAnalysisError);
    }
    
    /**
     * Limpia el cache de análisis de un repositorio específico.
     * Endpoint administrativo para gestión de cache.
     * 
     * @param repoParam Repositorio en formato "owner/repo"
     * @param principal Usuario autenticado
     * @return Confirmación de limpieza
     */
    @GetMapping(value = "/cache/clear", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> clearAnalysisCache(
            @RequestParam("repo") String repoParam,
            Principal principal) {
        
        log.info("🧹 Solicitando limpieza de cache para repositorio: {}", repoParam);
        
        return validateAndParseRepository(repoParam)
            .flatMap(parts -> {
                String owner = parts[0];
                String repo = parts[1];
                
                // Limpiar cache del handler
                repositoryAnalysisQueryHandler.clearCache(owner, repo);
                
                Map<String, Object> response = Map.of(
                    "message", "Cache limpiado exitosamente",
                    "repository", owner + "/" + repo,
                    "timestamp", java.time.Instant.now().toString(),
                    "cleared_by", principal != null ? principal.getName() : "anonymous"
                );
                
                log.info("✅ Cache limpiado para: {}/{}", owner, repo);
                return Mono.just(ResponseEntity.ok(response));
            })
            .onErrorResume(this::handleAnalysisError);
    }
    
    // Métodos auxiliares privados
    
    /**
     * Valida y parsea el parámetro de repositorio.
     * 
     * @param repoParam Parámetro del repositorio en formato "owner/repo"
     * @return Mono con array [owner, repo] o error
     */
    private Mono<String[]> validateAndParseRepository(String repoParam) {
        if (repoParam == null || repoParam.trim().isEmpty()) {
            log.error("❌ Parámetro 'repo' es obligatorio");
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' es obligatorio"));
        }
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.error(new IllegalArgumentException(
                "Parámetro 'repo' inválido. Debe ser 'owner/repo'"));
        }
        
        return Mono.just(parts);
    }
    
    /**
     * Maneja errores de análisis de forma consistente.
     * 
     * @param error Error ocurrido durante el análisis
     * @return ResponseEntity con error estructurado
     */
    private Mono<ResponseEntity<Map<String, Object>>> handleAnalysisError(Throwable error) {
        String errorType = error.getClass().getSimpleName();
        String errorMessage = error.getMessage();
        
        log.error("❌ Error en análisis de repositorio [{}]: {}", errorType, errorMessage);
        
        Map<String, Object> errorResponse = Map.of(
            "error", "Error analizando repositorio",
            "message", errorMessage != null ? errorMessage : "Error desconocido",
            "type", errorType,
            "timestamp", java.time.Instant.now().toString()
        );
        
        // Diferentes códigos de error según el tipo
        if (error instanceof IllegalArgumentException) {
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        } else {
            return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
        }
    }
} 