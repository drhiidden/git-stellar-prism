package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryAnalysisQuery;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.LanguageRepository;
import com.drhdn.ghvis.domain.event.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler para procesar queries de análisis de repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryAnalysisQueryHandler {
    
    private final LanguageRepository languageRepository;
    private final CacheService cacheService;
    private final EventStore eventStore;
    
    /**
     * Maneja la query para obtener análisis de repositorio.
     * 
     * @param query La query a procesar
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handle(GetRepositoryAnalysisQuery query) {
        log.info("🔍 Ejecutando query de análisis para repositorio: {} (QueryId: {})",
            query.getRepositoryFullName(), query.getQueryId());
        
        String cacheKey = query.getCacheKey();
        
        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando análisis fresco para: {}/{}", 
                query.getOwner(), query.getRepo());
            
            // Inicializar mapa de resultados
            Map<String, Object> result = new HashMap<>();
            
            // Crear Mono para cada componente solicitado
            Mono<Map<String, Object>> languagesMono = query.isIncludeLanguages() 
                ? getLanguageDistribution(query.getOwner(), query.getRepo(), query.getPrincipal())
                : Mono.just(Collections.emptyMap());
            
            Mono<List<Object>> technologiesMono = query.isIncludeTechnologies()
                ? getTechnologies(query.getOwner(), query.getRepo(), query.getPrincipal())
                : Mono.just(Collections.emptyList());
            
            Mono<Map<String, Object>> projectStructureMono = query.isIncludeProjectStructure()
                ? getProjectStructure(query.getOwner(), query.getRepo(), query.getPrincipal())
                : Mono.just(Collections.emptyMap());
            
            Mono<Map<String, Object>> technicalSummaryMono = query.isIncludeTechnicalSummary()
                ? getTechnicalSummary(query.getOwner(), query.getRepo(), query.getPrincipal())
                : Mono.just(Collections.emptyMap());
            
            // Combinar todos los resultados
            return Mono.zip(
                    languagesMono,
                    technologiesMono,
                    projectStructureMono,
                    technicalSummaryMono
                )
                .map(tuple -> {
                    Map<String, Object> analysisResult = new HashMap<>();
                    analysisResult.put("languageDistribution", tuple.getT1());
                    analysisResult.put("technologies", tuple.getT2());
                    analysisResult.put("projectStructure", tuple.getT3());
                    analysisResult.put("technicalSummary", tuple.getT4());
                    return analysisResult;
                });
        })
        .doOnSuccess(result -> {
            log.info("✅ Query de análisis completada exitosamente para: {}/{} (QueryId: {})",
                query.getOwner(), query.getRepo(), query.getQueryId());
        })
        .doOnError(error -> {
            log.error("❌ Error en query de análisis para: {}/{} (QueryId: {}): {}",
                query.getOwner(), query.getRepo(), query.getQueryId(), error.getMessage());
        });
    }
    
    /**
     * Maneja query para análisis completo de repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handleFullQuery(String owner, String repo, Principal principal) {
        GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createFullAnalysis(owner, repo, principal);
        return handle(query);
    }
    
    /**
     * Maneja query para análisis personalizado de repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @param commits Incluir commits
     * @param issues Incluir issues
     * @param pullRequests Incluir pull requests
     * @param technicalSummary Incluir resumen técnico
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handleCustomQuery(
            String owner, String repo, Principal principal,
            boolean commits, boolean issues, boolean pullRequests, boolean technicalSummary) {
        GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createCustomAnalysis(
            owner, repo, principal, true, true, true, true);
        return handle(query);
    }
    
    /**
     * Obtiene la distribución de lenguajes de un repositorio.
     */
    private Mono<Map<String, Object>> getLanguageDistribution(String owner, String repo, Principal principal) {
        return languageRepository.getLanguagesMap(owner, repo, principal)
            .map(langMap -> (Map<String, Object>) (Map<?, ?>) langMap)
            .defaultIfEmpty(Collections.emptyMap());
    }
    
    /**
     * Obtiene las tecnologías utilizadas en un repositorio.
     */
    private Mono<List<Object>> getTechnologies(String owner, String repo, Principal principal) {
        // TODO: Implementar detección de tecnologías
        return Mono.just(Collections.emptyList());
    }
    
    /**
     * Obtiene la estructura del proyecto.
     */
    private Mono<Map<String, Object>> getProjectStructure(String owner, String repo, Principal principal) {
        // TODO: Implementar análisis de estructura del proyecto
        return Mono.just(Collections.emptyMap());
    }
    
    /**
     * Obtiene el resumen técnico del repositorio.
     */
    private Mono<Map<String, Object>> getTechnicalSummary(String owner, String repo, Principal principal) {
        // Crear un resumen técnico básico
        Map<String, Object> summary = Map.of(
            "repositoryName", repo,
            "repositoryOwner", owner,
            "projectPurpose", "Propósito no disponible",
            "mainTechnologies", Collections.emptyList(),
            "rolesAndResponsibilities", "",
            "achievements", Collections.emptyList(),
            "codeSnippets", Collections.emptyList(),
            "documentationQuality", ""
        );
        
        return Mono.just(summary);
    }
    
    /**
     * Limpia el cache de análisis de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     */
    public void clearCache(String owner, String repo) {
        String cachePattern = String.format("repo:%s:%s:analysis:*", owner, repo);
        cacheService.clear(cachePattern).subscribe();
        log.info("🧹 Cache limpiado para análisis de repositorio: {}/{}", owner, repo);
    }
} 