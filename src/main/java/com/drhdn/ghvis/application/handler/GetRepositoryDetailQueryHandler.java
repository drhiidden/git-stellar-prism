package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryDetailQuery;
import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Handler para procesar queries de detalles de repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryDetailQueryHandler {
    
    private final GithubApiAdapter githubApiAdapter;
    private final CacheService cacheService;
    
    /**
     * Maneja la query para obtener detalles de repositorio.
     * 
     * @param query La query a procesar
     * @return Mono con el resultado según el tipo de detalle
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> handle(GetRepositoryDetailQuery query) {
        log.info("🔍 Ejecutando query de detalles para repositorio: {} (QueryId: {})",
            query.getRepositoryFullName(), query.getQueryId());
        
        return switch (query.getDetailType()) {
            case PULL_REQUEST -> (Mono<T>) handlePullRequestQuery(query);
            case ISSUE -> (Mono<T>) handleIssueQuery(query);
            case COMMIT -> (Mono<T>) handleCommitQuery(query);
        };
    }
    
    /**
     * Maneja query para obtener detalles de un Pull Request.
     * 
     * @param query La query a procesar
     * @return Mono con los detalles del Pull Request
     */
    public Mono<PullRequest> handlePullRequestQuery(GetRepositoryDetailQuery query) {
        log.info("🔍 Ejecutando query de detalles de PR para repositorio: {} (QueryId: {})",
            query.getRepositoryFullName(), query.getQueryId());
        
        String cacheKey = query.getCacheKey();
        
        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando detalles de PR frescos para: {}/{}#{}", 
                query.getOwner(), query.getRepo(), query.getNumber());
            return githubApiAdapter.getPullRequestDetail(query.getOwner(), query.getRepo(), query.getNumber(), query.getPrincipal());
        })
        .doOnSuccess(pr -> {
            log.info("✅ Query de detalles de PR completada exitosamente para: {}/{}#{} (QueryId: {})",
                query.getOwner(), query.getRepo(), query.getNumber(), query.getQueryId());
        })
        .doOnError(error -> {
            log.error("❌ Error en query de detalles de PR para: {}/{}#{} (QueryId: {}): {}",
                query.getOwner(), query.getRepo(), query.getNumber(), query.getQueryId(), error.getMessage());
        });
    }
    
    /**
     * Maneja query para obtener detalles de un Issue.
     * 
     * @param query La query a procesar
     * @return Mono con los detalles del Issue
     */
    public Mono<Issue> handleIssueQuery(GetRepositoryDetailQuery query) {
        log.info("🔍 Ejecutando query de detalles de Issue para repositorio: {} (QueryId: {})",
            query.getRepositoryFullName(), query.getQueryId());
        
        String cacheKey = query.getCacheKey();
        
        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando detalles de Issue frescos para: {}/{}#{}", 
                query.getOwner(), query.getRepo(), query.getNumber());
            return githubApiAdapter.getIssueDetail(query.getOwner(), query.getRepo(), query.getNumber(), query.getPrincipal());
        })
        .doOnSuccess(issue -> {
            log.info("✅ Query de detalles de Issue completada exitosamente para: {}/{}#{} (QueryId: {})",
                query.getOwner(), query.getRepo(), query.getNumber(), query.getQueryId());
        })
        .doOnError(error -> {
            log.error("❌ Error en query de detalles de Issue para: {}/{}#{} (QueryId: {}): {}",
                query.getOwner(), query.getRepo(), query.getNumber(), query.getQueryId(), error.getMessage());
        });
    }
    
    /**
     * Maneja query para obtener detalles de un Commit.
     * 
     * @param query La query a procesar
     * @return Mono con los detalles del Commit
     */
    public Mono<Commit> handleCommitQuery(GetRepositoryDetailQuery query) {
        log.info("🔍 Ejecutando query de detalles de Commit para repositorio: {} (QueryId: {})",
            query.getRepositoryFullName(), query.getQueryId());
        
        String cacheKey = query.getCacheKey();
        
        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando detalles de Commit frescos para: {}/{}@{}", 
                query.getOwner(), query.getRepo(), query.getSha());
            return githubApiAdapter.getCommitDetail(query.getOwner(), query.getRepo(), query.getSha(), query.getPrincipal());
        })
        .doOnSuccess(commit -> {
            log.info("✅ Query de detalles de Commit completada exitosamente para: {}/{}@{} (QueryId: {})",
                query.getOwner(), query.getRepo(), query.getSha(), query.getQueryId());
        })
        .doOnError(error -> {
            log.error("❌ Error en query de detalles de Commit para: {}/{}@{} (QueryId: {}): {}",
                query.getOwner(), query.getRepo(), query.getSha(), query.getQueryId(), error.getMessage());
        });
    }
    
    /**
     * Limpia el cache de detalles de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     */
    public void clearCache(String owner, String repo) {
        String[] cacheKeys = {
            String.format("repo:%s:%s:pull_request:*", owner, repo),
            String.format("repo:%s:%s:issue:*", owner, repo),
            String.format("repo:%s:%s:commit:*", owner, repo)
        };
        
        for (String cacheKey : cacheKeys) {
            cacheService.clear(cacheKey);
        }
        
        log.info("🧹 Cache limpiado para repositorio: {}/{}", owner, repo);
    }
} 