package com.drhdn.ghvis.application.usecase;

import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.entity.TechnicalSummary;
import com.drhdn.ghvis.domain.event.*;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.CircuitBreakerService;
import com.drhdn.ghvis.domain.port.CommitRepository;
import com.drhdn.ghvis.domain.port.EventPublisher;
import com.drhdn.ghvis.domain.port.IssueRepository;
import com.drhdn.ghvis.domain.port.PullRequestRepository;
import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import com.drhdn.ghvis.domain.port.TechnicalSummaryRepository;
import com.drhdn.ghvis.domain.service.CommitAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Use Case para análisis completo de repositorio con Event-Driven Architecture.
 * 
 * Integra todos los componentes del análisis: commits, issues, pull requests
 * y resumen técnico, publicando eventos para cada operación.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryAnalysisUseCase {

    private final CommitRepository commitRepository;
    private final IssueRepository issueRepository;
    private final PullRequestRepository pullRequestRepository;
    private final RepositoryRepository repositoryRepository;
    private final TechnicalSummaryRepository technicalSummaryRepository;
    private final CacheService cacheService;
    private final RateLimitService rateLimitService;
    private final CircuitBreakerService circuitBreakerService;
    private final CommitAnalysisService commitAnalysisService;
    private final EventPublisher eventPublisher;
    
    @Value("${app.resilience.enabled:true}")
    private boolean resilienceEnabled;

    /**
     * Ejecuta análisis completo de repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @param includeCommits Incluir análisis de commits
     * @param includeIssues Incluir análisis de issues
     * @param includePullRequests Incluir análisis de pull requests
     * @param includeTechnicalSummary Incluir resumen técnico
     * @return Mono con el análisis completo
     */
    public Mono<Map<String, Object>> execute(String owner, String repo, Principal principal,
                                           boolean includeCommits, boolean includeIssues,
                                           boolean includePullRequests, boolean includeTechnicalSummary) {
        String requestId = java.util.UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        // Publicar evento de solicitud
        RepositoryAnalysisRequestedEvent requestEvent = new RepositoryAnalysisRequestedEvent(
            owner, repo, principal, includeCommits, includeIssues, includePullRequests, includeTechnicalSummary);
        
        return eventPublisher.publish(requestEvent)
            .then(Mono.defer(() -> {
                log.info("🔍 Iniciando análisis completo para {}/{}", owner, repo);
                
                return performAnalysis(requestId, owner, repo, principal, startTime,
                    includeCommits, includeIssues, includePullRequests, includeTechnicalSummary);
            }))
            .doOnError(error -> {
                log.error("❌ Error en análisis de {}/{}: {}", owner, repo, error.getMessage());
                publishFailureEvent(requestId, owner, repo, principal, error, startTime);
            });
    }

    /**
     * Realiza el análisis completo del repositorio.
     */
    private Mono<Map<String, Object>> performAnalysis(String requestId, String owner, String repo,
                                                     Principal principal, Instant startTime,
                                                     boolean includeCommits, boolean includeIssues,
                                                     boolean includePullRequests, boolean includeTechnicalSummary) {
        
        return Mono.zip(
            includeCommits ? getCommits(requestId, owner, repo, principal) : Mono.just(List.<Commit>of()),
            includeIssues ? getIssues(requestId, owner, repo, principal) : Mono.just(List.<Issue>of()),
            includePullRequests ? getPullRequests(requestId, owner, repo, principal) : Mono.just(List.<PullRequest>of()),
            includeTechnicalSummary ? getTechnicalSummary(requestId, owner, repo, principal) : Mono.just(null)
        )
        .map(tuple -> {
            List<Commit> commits = tuple.getT1();
            List<Issue> issues = tuple.getT2();
            List<PullRequest> pullRequests = tuple.getT3();
            TechnicalSummary technicalSummary = tuple.getT4();
            
            // Publicar evento de completación
            publishCompletionEvent(requestId, owner, repo, principal, startTime,
                commits.size(), issues.size(), pullRequests.size(), technicalSummary != null);
            
            // Construir respuesta
            return buildAnalysisResponse(owner, repo, commits, issues, pullRequests, technicalSummary);
        });
    }

    /**
     * Obtiene commits del repositorio.
     */
    private Mono<List<Commit>> getCommits(String requestId, String owner, String repo, Principal principal) {
        // Publicar evento de solicitud de commits
        CommitRetrievalRequestedEvent commitEvent = new CommitRetrievalRequestedEvent(owner, repo, principal, resilienceEnabled);
        eventPublisher.publish(commitEvent).subscribe();
        
        return commitRepository.findByRepository(owner, repo, principal)
            .collectList()
            .doOnSuccess(commits -> {
                // Publicar evento de completación de commits
                CommitRetrievalCompletedEvent completedEvent = new CommitRetrievalCompletedEvent(
                    requestId, owner, repo, principal.getName(), commits.size(), 
                    java.time.Duration.between(Instant.now(), Instant.now()).toMillis(), 
                    false, resilienceEnabled, "api");
                eventPublisher.publish(completedEvent).subscribe();
            });
    }

    /**
     * Obtiene issues del repositorio.
     */
    private Mono<List<Issue>> getIssues(String requestId, String owner, String repo, Principal principal) {
        // Publicar evento de solicitud de issues
        IssueRetrievalRequestedEvent issueEvent = new IssueRetrievalRequestedEvent(
            owner, repo, principal, "all", "created", "desc", 30, 1);
        eventPublisher.publish(issueEvent).subscribe();
        
        return issueRepository.findByRepository(owner, repo, principal)
            .collectList();
    }

    /**
     * Obtiene pull requests del repositorio.
     */
    private Mono<List<PullRequest>> getPullRequests(String requestId, String owner, String repo, Principal principal) {
        // Publicar evento de solicitud de PRs
        PullRequestRetrievalRequestedEvent prEvent = new PullRequestRetrievalRequestedEvent(
            owner, repo, principal, "all", "created", "desc", 30, 1, true, true);
        eventPublisher.publish(prEvent).subscribe();
        
        return pullRequestRepository.findByRepository(owner, repo, principal)
            .collectList();
    }

    /**
     * Obtiene resumen técnico del repositorio.
     */
    private Mono<TechnicalSummary> getTechnicalSummary(String requestId, String owner, String repo, Principal principal) {
        return technicalSummaryRepository.generateForRepository(owner, repo, principal)
            .doOnSuccess(summary -> {
                if (summary != null) {
                    // Publicar evento de generación de resumen técnico
                    TechnicalSummaryGeneratedEvent summaryEvent = new TechnicalSummaryGeneratedEvent(
                        requestId, owner, repo, principal.getName(), 
                        java.time.Duration.between(Instant.now(), Instant.now()).toMillis(),
                        summary.getTechnologies(), summary.getLanguages(), 
                        summary.getTotalFiles(), summary.getTotalSize(),
                        summary.getPrimaryLanguage(), summary.getComplexityScore(),
                        false, "comprehensive");
                    eventPublisher.publish(summaryEvent).subscribe();
                }
            });
    }

    /**
     * Construye la respuesta del análisis.
     */
    private Map<String, Object> buildAnalysisResponse(String owner, String repo, 
                                                     List<Commit> commits, List<Issue> issues,
                                                     List<PullRequest> pullRequests, TechnicalSummary technicalSummary) {
        return Map.of(
            "repository", owner + "/" + repo,
            "analysis_timestamp", Instant.now().toString(),
            "commits", Map.of(
                "count", commits.size(),
                "data", commits,
                "analysis", commitAnalysisService.analyzeCommitFrequency(commits)
            ),
            "issues", Map.of(
                "count", issues.size(),
                "data", issues
            ),
            "pull_requests", Map.of(
                "count", pullRequests.size(),
                "data", pullRequests
            ),
            "technical_summary", technicalSummary != null ? technicalSummary : Map.of(),
            "total_items", commits.size() + issues.size() + pullRequests.size()
        );
    }

    /**
     * Publica evento de completación.
     */
    private void publishCompletionEvent(String requestId, String owner, String repo, 
                                      Principal principal, Instant startTime,
                                      int commitCount, int issueCount, int pullRequestCount, 
                                      boolean technicalSummaryGenerated) {
        long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
        
        RepositoryAnalysisCompletedEvent event = new RepositoryAnalysisCompletedEvent(
            requestId, owner, repo, principal.getName(), durationMs, commitCount,
            issueCount, pullRequestCount, technicalSummaryGenerated, "full", false, resilienceEnabled);
        
        eventPublisher.publish(event).subscribe();
    }

    /**
     * Publica evento de fallo.
     */
    private void publishFailureEvent(String requestId, String owner, String repo, 
                                   Principal principal, Throwable error, Instant startTime) {
        long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
        
        CommitRetrievalFailedEvent event = new CommitRetrievalFailedEvent(
            requestId, owner, repo, principal.getName(), error.getMessage(), 
            error.getClass().getSimpleName(), durationMs, resilienceEnabled, "analysis_error");
        
        eventPublisher.publish(event).subscribe();
    }
} 