package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryAnalysisQuery;
import com.drhdn.ghvis.application.usecase.GetRepositoryAnalysisUseCase;
import com.drhdn.ghvis.domain.port.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Query Handler para GetRepositoryAnalysisQuery.
 * 
 * Maneja las consultas de análisis de repositorios,
 * optimizando con cache y separando responsabilidades de lectura.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryAnalysisQueryHandler {
    
    private final GetRepositoryAnalysisUseCase getRepositoryAnalysisUseCase;
    private final CacheService cacheService;
    
    /**
     * Maneja la query de obtención de análisis de repositorio.
     * 
     * @param query Query a ejecutar
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handle(GetRepositoryAnalysisQuery query) {
        log.info("🔍 Ejecutando query de análisis para {}/{} (QueryId: {})", 
            query.getOwner(), query.getRepo(), query.getQueryId());
        
        String cacheKey = buildCacheKey(query);
        
        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando datos frescos para {}/{}", query.getOwner(), query.getRepo());
            
            return getRepositoryAnalysisUseCase.execute(
                query.getOwner(), query.getRepo(), query.getPrincipal(),
                query.isIncludeCommits(), query.isIncludeIssues(),
                query.isIncludePullRequests(), query.isIncludeTechnicalSummary()
            );
        })
            .doOnSuccess(result -> {
                log.info("✅ Query completada exitosamente para {}/{} (QueryId: {})", 
                    query.getOwner(), query.getRepo(), query.getQueryId());
            })
            .doOnError(error -> {
                log.error("❌ Error en query para {}/{} (QueryId: {}): {}", 
                    query.getOwner(), query.getRepo(), query.getQueryId(), error.getMessage());
            });
    }
    
    /**
     * Maneja la query de obtención de análisis completo.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handleFullQuery(String owner, String repo, java.security.Principal principal) {
        GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.create(owner, repo, principal);
        return handle(query);
    }
    
    /**
     * Maneja la query de obtención de análisis personalizado.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @param includeCommits Incluir commits
     * @param includeIssues Incluir issues
     * @param includePullRequests Incluir pull requests
     * @param includeTechnicalSummary Incluir resumen técnico
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handleCustomQuery(String owner, String repo, java.security.Principal principal,
                                                     boolean includeCommits, boolean includeIssues,
                                                     boolean includePullRequests, boolean includeTechnicalSummary) {
        GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createCustom(
            owner, repo, principal, includeCommits, includeIssues, includePullRequests, includeTechnicalSummary);
        return handle(query);
    }
    
    /**
     * Construye la clave de cache para la query.
     * 
     * @param query Query para la cual construir la clave
     * @return Clave de cache
     */
    private String buildCacheKey(GetRepositoryAnalysisQuery query) {
        String username = query.getPrincipal() != null ? query.getPrincipal().getName() : "anonymous";
        return String.format("analysis:%s:%s:%s:%s:%s:%s:%s", 
            username, query.getOwner(), query.getRepo(),
            query.isIncludeCommits(), query.isIncludeIssues(),
            query.isIncludePullRequests(), query.isIncludeTechnicalSummary());
    }
} 