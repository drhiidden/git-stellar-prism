package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.usecase.GetRepositoryCommitsUseCase;
import com.drhdn.ghvis.application.usecase.GetCommitDetailUseCase;
import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.application.query.GetRepositoryDetailQuery;
import com.drhdn.ghvis.application.handler.GetRepositoryDetailQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para datos de repositorio (commits, detalles, etc.).
 * Implementa CQRS usando queries y handlers.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/repository")
public class RepositoryController {

    private final GetRepositoryCommitsUseCase getRepositoryCommitsUseCase;
    private final GetCommitDetailUseCase getCommitDetailUseCase;
    private final GetRepositoryDetailQueryHandler getRepositoryDetailQueryHandler;

    /**
     * Devuelve la lista de commits de un repositorio.
     * Ejemplo: /api/repository/commits?repo=owner/repo
     */
    @GetMapping(value = "/commits", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<Commit>>> getCommits(@RequestParam("repo") String repoParam, Principal principal) {
        log.info("🔄 Solicitando commits para: {}", repoParam);
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        String owner = parts[0];
        String repo = parts[1];
        
        log.info("📊 Obteniendo commits para: {}/{}", owner, repo);
        
        // Usar use case que internamente maneja cache, rate limiting y OAuth2
        return getRepositoryCommitsUseCase.execute(owner, repo, principal)
            .collectList()
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Commits obtenidos: {} commits para {}/{}", 
                response.getBody().size(), owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo commits para {}/{}: {}", 
                owner, repo, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    /**
     * Devuelve los detalles de un commit.
     */
    @GetMapping(value = "/details/commit/{sha}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Commit>> getCommitDetails(@PathVariable("sha") String sha, 
                                                        @RequestParam("repo") String repoParam, 
                                                        Principal principal) {
        log.info("🔍 Obteniendo detalles del commit: {} para repositorio: {}", sha, repoParam);
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        return getCommitDetailUseCase.execute(parts[0], parts[1], sha, principal)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Detalles del commit obtenidos: {} para {}/{}", sha, parts[0], parts[1]))
            .doOnError(error -> log.error("❌ Error obteniendo detalles del commit {}: {}", sha, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    /**
     * Devuelve los detalles de un Pull Request.
     */
    @GetMapping(value = "/details/pr/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<PullRequest>> getPrDetails(@PathVariable("number") int number,
                                                          @RequestParam("repo") String repoParam, 
                                                          Principal principal) {
        log.info("🔍 Obteniendo detalles del PR: #{} para repositorio: {}", number, repoParam);
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        GetRepositoryDetailQuery query = GetRepositoryDetailQuery.createPullRequestQuery(parts[0], parts[1], number, principal);
        
        return getRepositoryDetailQueryHandler.handlePullRequestQuery(query)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Detalles del PR obtenidos: #{} para {}/{}", number, parts[0], parts[1]))
            .doOnError(error -> log.error("❌ Error obteniendo detalles del PR #{}: {}", number, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().build()));
    }

    /**
     * Devuelve los detalles de un Issue.
     */
    @GetMapping(value = "/details/issue/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Issue>> getIssueDetails(@PathVariable("number") int number,
                                                       @RequestParam("repo") String repoParam, 
                                                       Principal principal) {
        log.info("🔍 Obteniendo detalles del issue: #{} para repositorio: {}", number, repoParam);
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        GetRepositoryDetailQuery query = GetRepositoryDetailQuery.createIssueQuery(parts[0], parts[1], number, principal);
        
        return getRepositoryDetailQueryHandler.handleIssueQuery(query)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Detalles del issue obtenidos: #{} para {}/{}", number, parts[0], parts[1]))
            .doOnError(error -> log.error("❌ Error obteniendo detalles del issue #{}: {}", number, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().build()));
    }
} 