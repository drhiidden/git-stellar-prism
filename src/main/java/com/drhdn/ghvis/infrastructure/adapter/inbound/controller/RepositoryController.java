package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.query.GetRepositoryCommitsQuery;
import com.drhdn.ghvis.application.handler.GetRepositoryCommitsQueryHandler;
import com.drhdn.ghvis.application.query.GetRepositoryDetailQuery;
import com.drhdn.ghvis.application.handler.GetRepositoryDetailQueryHandler;
import com.drhdn.ghvis.application.query.GetRepositoryTimelineQuery;
import com.drhdn.ghvis.application.handler.GetRepositoryTimelineQueryHandler;
import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.domain.entity.TimelineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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

    private final GetRepositoryCommitsQueryHandler getRepositoryCommitsQueryHandler;
    private final GetRepositoryDetailQueryHandler getRepositoryDetailQueryHandler;
    private final GetRepositoryTimelineQueryHandler getRepositoryTimelineQueryHandler;

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
        
        log.info("📊 Ejecutando query de commits para: {}/{}", owner, repo);

        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.create(owner, repo, principal);

        return getRepositoryCommitsQueryHandler.handle(query)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> {
                var body = response.getBody();
                if (body != null) {
                    log.info("✅ Commits obtenidos: {} commits para {}/{} (QueryId: {})", 
                        body.size(), owner, repo, query.getQueryId());
                } else {
                    log.warn("⚠️ Respuesta de commits vacía para {}/{} (QueryId: {})", 
                        owner, repo, query.getQueryId());
                }
            })
            .doOnError(error -> log.error("❌ Error obteniendo commits para {}/{} (QueryId: {}): {}", 
                owner, repo, query.getQueryId(), error.getMessage()))
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
        
        GetRepositoryDetailQuery query = GetRepositoryDetailQuery.createCommitQuery(parts[0], parts[1], sha, principal);

        return getRepositoryDetailQueryHandler.handleCommitQuery(query)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Detalles del commit obtenidos: {} para {}/{} (QueryId: {})", sha, parts[0], parts[1], query.getQueryId()))
            .doOnError(error -> log.error("❌ Error obteniendo detalles del commit {} (QueryId: {}): {}", sha, query.getQueryId(), error.getMessage()))
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

    /**
     * Devuelve el timeline de eventos de un repositorio.
     * Ejemplo: /api/repository/timeline?repo=owner/repo&types=commits,prs&limit=50
     */
    @GetMapping(value = "/timeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<TimelineEvent>>> getTimeline(
            @RequestParam("repo") String repoParam,
            @RequestParam(value = "types", defaultValue = "commits,prs,issues") String types,
            @RequestParam(value = "limit", defaultValue = "100") int limit,
            Principal principal) {
        
        log.info("🔄 Solicitando timeline para: {} (tipos: {}, límite: {})", repoParam, types, limit);
        
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            log.error("❌ Formato inválido de repositorio: {}", repoParam);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        String owner = parts[0];
        String repo = parts[1];
        
        // Parsear tipos de eventos solicitados
        Set<GetRepositoryTimelineQuery.EventType> eventTypes = parseEventTypes(types);
        
        log.info("📊 Ejecutando query de timeline para: {}/{}", owner, repo);
        
        GetRepositoryTimelineQuery query = GetRepositoryTimelineQuery.createFiltered(
                owner, repo, eventTypes, null, null, limit, principal);
        
        return getRepositoryTimelineQueryHandler.handle(query)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> {
                var body = response.getBody();
                if (body != null) {
                    log.info("✅ Timeline obtenido: {} eventos para {}/{} (QueryId: {})", 
                        body.size(), owner, repo, query.getQueryId());
                } else {
                    log.warn("⚠️ Respuesta de timeline vacía para {}/{} (QueryId: {})", 
                        owner, repo, query.getQueryId());
                }
            })
            .doOnError(error -> log.error("❌ Error obteniendo timeline para {}/{} (QueryId: {}): {}", 
                owner, repo, query.getQueryId(), error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().build()));
    }
    
    /**
     * Parsea los tipos de eventos desde el parámetro string.
     * Principio SRP: responsabilidad única de parsing.
     */
    private Set<GetRepositoryTimelineQuery.EventType> parseEventTypes(String typesParam) {
        Set<GetRepositoryTimelineQuery.EventType> eventTypes = new HashSet<>();
        
        String[] typeNames = typesParam.toLowerCase().split(",");
        for (String typeName : typeNames) {
            switch (typeName.trim()) {
                case "commits" -> eventTypes.add(GetRepositoryTimelineQuery.EventType.COMMITS);
                case "prs", "pull_requests" -> eventTypes.add(GetRepositoryTimelineQuery.EventType.PULL_REQUESTS);
                case "issues" -> eventTypes.add(GetRepositoryTimelineQuery.EventType.ISSUES);
                default -> log.warn("⚠️ Tipo de evento desconocido: {}", typeName);
            }
        }
        
        // Si no se reconoció ningún tipo, usar todos por defecto
        if (eventTypes.isEmpty()) {
            eventTypes = Set.of(
                GetRepositoryTimelineQuery.EventType.COMMITS,
                GetRepositoryTimelineQuery.EventType.PULL_REQUESTS,
                GetRepositoryTimelineQuery.EventType.ISSUES
            );
        }
        
        return eventTypes;
    }
} 