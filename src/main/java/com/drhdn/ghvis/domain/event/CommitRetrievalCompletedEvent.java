package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Evento de dominio que representa la finalización de una solicitud de commits.
 * 
 * Este evento se dispara cuando se completa exitosamente la obtención
 * de commits, incluyendo información sobre el resultado y métricas.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class CommitRetrievalCompletedEvent {
    
    private final String requestId;
    private final String owner;
    private final String repo;
    private final String username;
    private final int commitCount;
    private final Instant timestamp;
    private final long durationMs;
    private final boolean fromCache;
    private final boolean resilienceUsed;
    private final String source; // "api", "cache", "fallback"
    
    public CommitRetrievalCompletedEvent(String requestId, String owner, String repo, 
                                       String username, int commitCount, long durationMs, 
                                       boolean fromCache, boolean resilienceUsed, String source) {
        this.requestId = requestId;
        this.owner = owner;
        this.repo = repo;
        this.username = username;
        this.commitCount = commitCount;
        this.timestamp = Instant.now();
        this.durationMs = durationMs;
        this.fromCache = fromCache;
        this.resilienceUsed = resilienceUsed;
        this.source = source;
    }
    
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    public String getEventType() {
        return "COMMIT_RETRIEVAL_COMPLETED";
    }
    
    public boolean isSuccessful() {
        return commitCount >= 0;
    }
} 