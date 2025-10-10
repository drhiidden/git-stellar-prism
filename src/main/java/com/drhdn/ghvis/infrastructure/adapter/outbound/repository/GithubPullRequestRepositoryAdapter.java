package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.port.PullRequestRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.security.Principal;

/**
 * Adaptador para operaciones con Pull Requests de GitHub.
 * Implementa el puerto PullRequestRepository usando GitHub API.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubPullRequestRepositoryAdapter implements PullRequestRepository {

    private final GithubApiAdapter githubApiAdapter;

    @Override
    public Flux<PullRequest> findByRepository(String owner, String repo, Principal principal) {
        log.debug("🔍 Obteniendo pull requests de {}/{}", owner, repo);
        
        // Por ahora retornamos vacío - implementación completa pendiente
        // TODO: Implementar getRepositoryPullRequests en GithubApiAdapter
        return Flux.<PullRequest>empty()
            .doOnComplete(() -> log.debug("⚠️  Pull requests no implementado para {}/{}", owner, repo));
    }

    @Override
    public Flux<PullRequest> findByRepositoryWithFilters(
            String owner, 
            String repo, 
            String state, 
            String sort, 
            String direction, 
            int perPage, 
            int page, 
            Principal principal) {
        
        log.debug("🔍 Obteniendo pull requests de {}/{} con filtros (state={}, sort={}, direction={}, perPage={}, page={})", 
            owner, repo, state, sort, direction, perPage, page);
        
        // Por ahora retornamos todos los PRs sin filtros avanzados
        // TODO: Implementar filtros específicos en GithubApiAdapter si es necesario
        return findByRepository(owner, repo, principal);
    }
}

