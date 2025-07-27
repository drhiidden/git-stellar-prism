package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.handler.GetRepositoryAnalysisQueryHandler;
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
 * Implementa CQRS usando queries y handlers.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
@Slf4j
public class AnalysisController {

    private final GetRepositoryAnalysisQueryHandler getRepositoryAnalysisQueryHandler;

    /**
     * Analiza un repositorio para obtener distribución de lenguajes, tecnologías y resumen técnico.
     * Endpoint consumido por summaryEditor.js
     */
    @GetMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> analyzeRepository(@RequestParam("repo") String repoParam, Principal principal) {
        log.info("🔍 Solicitando análisis para repositorio: {}", repoParam);
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "Parámetro 'repo' inválido. Debe ser 'owner/repo'."
            )));
        }
        
        String owner = parts[0];
        String repo = parts[1];
        
        log.info("📊 Obteniendo análisis para: {}/{}", owner, repo);
        
        return getRepositoryAnalysisQueryHandler.handleBasicAnalysis(owner, repo, principal)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Análisis completado para: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo análisis para {}/{}: {}", 
                owner, repo, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().body(Map.of(
                "error", "Error analizando repositorio",
                "message", error.getMessage()
            ))));
    }
    
    /**
     * Analiza un repositorio con opciones personalizadas.
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
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "Parámetro 'repo' inválido. Debe ser 'owner/repo'."
            )));
        }
        
        String owner = parts[0];
        String repo = parts[1];
        
        log.info("📊 Obteniendo análisis personalizado para: {}/{} (L:{}, T:{}, S:{}, Sum:{})", 
            owner, repo, includeLanguages, includeTechnologies, includeStructure, includeSummary);
        
        return getRepositoryAnalysisQueryHandler.handleCustomAnalysis(
                owner, repo, principal, 
                includeLanguages, includeTechnologies, 
                includeStructure, includeSummary)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Análisis personalizado completado para: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo análisis personalizado para {}/{}: {}", 
                owner, repo, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().body(Map.of(
                "error", "Error analizando repositorio",
                "message", error.getMessage()
            ))));
    }
    
    /**
     * Analiza un repositorio de forma completa.
     */
    @GetMapping(value = "/analyze/full", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> analyzeRepositoryFull(
            @RequestParam("repo") String repoParam,
            Principal principal) {
        
        log.info("🔍 Solicitando análisis completo para repositorio: {}", repoParam);
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "error", "Parámetro 'repo' inválido. Debe ser 'owner/repo'."
            )));
        }
        
        String owner = parts[0];
        String repo = parts[1];
        
        log.info("📊 Obteniendo análisis completo para: {}/{}", owner, repo);
        
        return getRepositoryAnalysisQueryHandler.handleFullAnalysis(owner, repo, principal)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Análisis completo finalizado para: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo análisis completo para {}/{}: {}", 
                owner, repo, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().body(Map.of(
                "error", "Error analizando repositorio",
                "message", error.getMessage()
            ))));
    }
} 