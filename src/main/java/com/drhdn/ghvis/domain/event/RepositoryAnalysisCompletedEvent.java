package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * Evento de dominio que representa la finalización del análisis de repositorio.
 * 
 * Este evento se dispara cuando se completa exitosamente el análisis completo
 * de un repositorio, incluyendo métricas y estadísticas.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class RepositoryAnalysisCompletedEvent {
    
    private final String requestId;
    private final String owner;
    private final String repo;
    private final String username;
    private final Instant timestamp;
    private final long durationMs;
    private final int commitCount;
    private final int issueCount;
    private final int pullRequestCount;
    private final boolean technicalSummaryGenerated;
    private final String analysisType; // "full", "partial", "commits_only", etc.
    private final boolean fromCache;
    private final boolean resilienceUsed;
    
    public RepositoryAnalysisCompletedEvent(String requestId, String owner, String repo, 
                                          String username, long durationMs, int commitCount,
                                          int issueCount, int pullRequestCount, 
                                          boolean technicalSummaryGenerated, String analysisType,
                                          boolean fromCache, boolean resilienceUsed) {
        this.requestId = requestId;
        this.owner = owner;
        this.repo = repo;
        this.username = username;
        this.timestamp = Instant.now();
        this.durationMs = durationMs;
        this.commitCount = commitCount;
        this.issueCount = issueCount;
        this.pullRequestCount = pullRequestCount;
        this.technicalSummaryGenerated = technicalSummaryGenerated;
        this.analysisType = analysisType;
        this.fromCache = fromCache;
        this.resilienceUsed = resilienceUsed;
    }
    
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    public String getEventType() {
        return "REPOSITORY_ANALYSIS_COMPLETED";
    }
    
    public boolean isSuccessful() {
        return commitCount >= 0 && issueCount >= 0 && pullRequestCount >= 0;
    }
    
    public int getTotalItems() {
        return commitCount + issueCount + pullRequestCount;
    }
} 