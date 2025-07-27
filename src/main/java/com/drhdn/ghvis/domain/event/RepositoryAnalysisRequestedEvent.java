package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.security.Principal;

/**
 * Evento de dominio que representa una solicitud de análisis de repositorio.
 * 
 * Este evento se dispara cuando se solicita el análisis completo de un repositorio,
 * incluyendo commits, issues, pull requests y métricas técnicas.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class RepositoryAnalysisRequestedEvent {
    
    private final String owner;
    private final String repo;
    private final Principal principal;
    private final String requestId;
    private final Instant timestamp;
    private final boolean includeCommits;
    private final boolean includeIssues;
    private final boolean includePullRequests;
    private final boolean includeTechnicalSummary;
    
    public RepositoryAnalysisRequestedEvent(String owner, String repo, Principal principal, 
                                          boolean includeCommits, boolean includeIssues, 
                                          boolean includePullRequests, boolean includeTechnicalSummary) {
        this.owner = owner;
        this.repo = repo;
        this.principal = principal;
        this.requestId = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.includeCommits = includeCommits;
        this.includeIssues = includeIssues;
        this.includePullRequests = includePullRequests;
        this.includeTechnicalSummary = includeTechnicalSummary;
    }
    
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    public String getEventType() {
        return "REPOSITORY_ANALYSIS_REQUESTED";
    }
    
    public String getUsername() {
        return principal != null ? principal.getName() : "anonymous";
    }
    
    public boolean isFullAnalysis() {
        return includeCommits && includeIssues && includePullRequests && includeTechnicalSummary;
    }
} 