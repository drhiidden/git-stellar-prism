package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.domain.port.IssueRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.security.Principal;

/**
 * Adaptador para operaciones con Issues de GitHub.
 * Implementa el puerto IssueRepository usando GitHub API.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubIssueRepositoryAdapter implements IssueRepository {

    private final GithubApiAdapter githubApiAdapter;

    @Override
    public Flux<Issue> findByRepository(String owner, String repo, Principal principal) {
        log.debug("🔍 Obteniendo issues de {}/{}", owner, repo);
        
        // Por ahora retornamos vacío - implementación completa pendiente
        // TODO: Implementar getRepositoryIssues en GithubApiAdapter
        return Flux.<Issue>empty()
            .doOnComplete(() -> log.debug("⚠️  Issues no implementado para {}/{}", owner, repo));
    }

    @Override
    public Flux<Issue> findByRepositoryWithFilters(
            String owner, 
            String repo, 
            String state, 
            String sort, 
            String direction, 
            int perPage, 
            int page, 
            Principal principal) {
        
        log.debug("🔍 Obteniendo issues de {}/{} con filtros (state={}, sort={}, direction={}, perPage={}, page={})", 
            owner, repo, state, sort, direction, perPage, page);
        
        // Por ahora retornamos todos los issues sin filtros avanzados
        // TODO: Implementar filtros específicos en GithubApiAdapter si es necesario
        return findByRepository(owner, repo, principal);
    }
}

