package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * Evento de dominio que representa el fallo de una solicitud de commits.
 * 
 * Este evento se dispara cuando falla la obtención de commits,
 * permitiendo logging, alertas y análisis de errores.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class CommitRetrievalFailedEvent {
    
    private final String requestId;
    private final String owner;
    private final String repo;
    private final String username;
    private final String errorMessage;
    private final String errorType;
    private final Instant timestamp;
    private final long durationMs;
    private final boolean resilienceEnabled;
    private final String failureReason; // "rate_limit", "api_error", "network", "timeout"
    
    public CommitRetrievalFailedEvent(String requestId, String owner, String repo, 
                                    String username, String errorMessage, String errorType,
                                    long durationMs, boolean resilienceEnabled, String failureReason) {
        this.requestId = requestId;
        this.owner = owner;
        this.repo = repo;
        this.username = username;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.timestamp = Instant.now();
        this.durationMs = durationMs;
        this.resilienceEnabled = resilienceEnabled;
        this.failureReason = failureReason;
    }
    
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    public String getEventType() {
        return "COMMIT_RETRIEVAL_FAILED";
    }
    
    public boolean isResilienceFailure() {
        return resilienceEnabled && !"rate_limit".equals(failureReason);
    }
} 