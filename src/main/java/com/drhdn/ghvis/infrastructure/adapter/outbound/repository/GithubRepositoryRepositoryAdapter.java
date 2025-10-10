package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

/**
 * Adaptador para operaciones con Repositorios de GitHub.
 * Implementa el puerto RepositoryRepository usando GitHub API.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubRepositoryRepositoryAdapter implements RepositoryRepository {

    private final GithubApiAdapter githubApiAdapter;

    @Override
    public Mono<Repository> findByOwnerAndName(String owner, String repo, Principal principal) {
        log.debug("🔍 Obteniendo repositorio {}/{}", owner, repo);
        
        return githubApiAdapter.getRepository(owner, repo, principal)
            .doOnSuccess(repository -> log.debug("✅ Repositorio obtenido: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo repositorio {}/{}: {}", 
                owner, repo, error.getMessage()));
    }

    @Override
    public Mono<Repository> findPublicByOwnerAndName(String owner, String repo) {
        log.debug("🔍 Obteniendo repositorio público {}/{}", owner, repo);
        
        // Para repos públicos, podemos pasar null como principal
        return githubApiAdapter.getRepository(owner, repo, null)
            .doOnSuccess(repository -> log.debug("✅ Repositorio público obtenido: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo repositorio público {}/{}: {}", 
                owner, repo, error.getMessage()));
    }

    @Override
    public Flux<Repository> findByUser(Principal principal) {
        if (principal == null) {
            log.warn("⚠️  Intentando obtener repositorios sin principal");
            return Flux.empty();
        }
        
        log.debug("🔍 Obteniendo repositorios del usuario autenticado");
        
        return githubApiAdapter.getUserRepositories(principal)
            .doOnComplete(() -> log.debug("✅ Repositorios obtenidos para usuario"))
            .doOnError(error -> log.error("❌ Error obteniendo repositorios del usuario: {}", 
                error.getMessage()));
    }

    @Override
    public Mono<Map<String, Long>> findLanguages(String owner, String repo, Principal principal) {
        log.debug("🔍 Obteniendo lenguajes de {}/{}", owner, repo);
        
        // Por ahora retornamos vacío - implementación pendiente
        // TODO: Implementar getRepositoryLanguages en GithubApiAdapter
        return Mono.<Map<String, Long>>just(Map.of())
            .doOnSuccess(languages -> log.debug("⚠️  Lenguajes no implementado para {}/{}", owner, repo));
    }
}

