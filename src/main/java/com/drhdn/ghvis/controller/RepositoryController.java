package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.model.Commit;
import com.drhdn.ghvis.model.PullRequest;
import com.drhdn.ghvis.model.Issue;
import com.drhdn.ghvis.service.GithubService;
import com.drhdn.ghvis.service.CommitCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Controlador REST para datos de repositorio (commits, detalles, etc.).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repository")
public class RepositoryController {

    private final GithubService githubService;
    private final CommitCacheService commitCacheService;

    /**
     * Devuelve la lista de commits de un repositorio.
     * Ejemplo: /api/repository/commits?repo=owner/repo
     */
    @GetMapping(value = "/commits", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Commit>> getCommits(@RequestParam("repo") String repoParam) {
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido. Debe ser 'owner/repo'."));
        }
        String owner = parts[0];
        String repo = parts[1];
        return commitCacheService.getCommits(owner, repo).collectList();
    }

    /**
     * Devuelve los detalles de un commit.
     */
    @GetMapping(value = "/details/commit/{sha}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Commit> getCommitDetails(@PathVariable("sha") String sha, @RequestParam("repo") String repoParam) {
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido."));
        }
        return githubService.getCommitDetail(parts[0], parts[1], sha);
    }

    /**
     * Devuelve los detalles de un Pull Request.
     */
    @GetMapping(value = "/details/pr/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<PullRequest> getPrDetails(@PathVariable("number") int number,
                                          @RequestParam("repo") String repoParam) {
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido."));
        }
        return githubService.getPullRequestDetail(parts[0], parts[1], number);
    }

    /**
     * Devuelve los detalles de un Issue.
     */
    @GetMapping(value = "/details/issue/{number}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Issue> getIssueDetails(@PathVariable("number") int number,
                                       @RequestParam("repo") String repoParam) {
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido."));
        }
        return githubService.getIssueDetail(parts[0], parts[1], number);
    }

    // PR y Issue: métodos stub por ahora
} 