package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.usecase.GetRepositoryCommitsUseCase;
import com.drhdn.ghvis.application.usecase.GetCommitDetailUseCase;
import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import com.drhdn.ghvis.infrastructure.adapter.inbound.security.OAuth2UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para datos de repositorio (commits, detalles, etc.).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/repository")
public class RepositoryController {

    private final GithubApiAdapter githubApiAdapter;
    private final GetRepositoryCommitsUseCase getRepositoryCommitsUseCase;
    private final GetCommitDetailUseCase getCommitDetailUseCase;
    private final OAuth2UserService oAuth2UserService;

    /**
     * Devuelve la lista de commits de un repositorio.
     * Ejemplo: /api/repository/commits?repo=owner/repo
     */
    @GetMapping(value = "/commits", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Commit>> getCommits(@RequestParam("repo") String repoParam, Principal principal) {
        log.info("🔄 Solicitando commits para: {}", repoParam);
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido. Debe ser 'owner/repo'."));
        }
        String owner = parts[0];
        String repo = parts[1];
        
        log.info("📊 Obteniendo commits para: {}/{}", owner, repo);
        
        // Usar use case que internamente maneja cache, rate limiting y OAuth2
        return getRepositoryCommitsUseCase.execute(owner, repo, principal)
            .collectList()
            .doOnNext(commits -> log.info("✅ Commits obtenidos: {} commits para {}/{}", 
                commits.size(), owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo commits para {}/{}: {}", 
                owner, repo, error.getMessage()));
    }

    /**
     * Devuelve los detalles de un commit.
     */
    @GetMapping(value = "/details/commit/{sha}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Commit> getCommitDetails(@PathVariable("sha") String sha, @RequestParam("repo") String repoParam, Principal principal) {
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido."));
        }
        return getCommitDetailUseCase.execute(parts[0], parts[1], sha, principal);
    }

    /**
     * Devuelve los detalles de un Pull Request.
     */
    @GetMapping(value = "/details/pr/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<PullRequest> getPrDetails(@PathVariable("number") int number,
                                          @RequestParam("repo") String repoParam, Principal principal) {
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido."));
        }
        return githubApiAdapter.getPullRequestDetail(parts[0], parts[1], number, principal);
    }

    /**
     * Devuelve los detalles de un Issue.
     */
    @GetMapping(value = "/details/issue/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Issue> getIssueDetails(@PathVariable("number") int number,
                                       @RequestParam("repo") String repoParam, Principal principal) {
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido."));
        }
        return githubApiAdapter.getIssueDetail(parts[0], parts[1], number, principal);
    }

    // PR y Issue: métodos stub por ahora
} 