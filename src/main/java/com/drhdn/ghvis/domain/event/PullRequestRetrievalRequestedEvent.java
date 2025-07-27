package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.security.Principal;

/**
 * Evento de dominio que representa una solicitud de pull requests.
 * 
 * Este evento se dispara cuando se solicita la obtención de pull requests
 * de un repositorio, permitiendo análisis de colaboración y métricas.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class PullRequestRetrievalRequestedEvent {
    
    private final String owner;
    private final String repo;
    private final Principal principal;
    private final String requestId;
    private final Instant timestamp;
    private final String state; // "open", "closed", "all"
    private final String sort; // "created", "updated", "popularity", "long-running"
    private final String direction; // "asc", "desc"
    private final int perPage;
    private final int page;
    private final boolean includeReviews;
    private final boolean includeComments;
    
    public PullRequestRetrievalRequestedEvent(String owner, String repo, Principal principal, 
                                            String state, String sort, String direction, 
                                            int perPage, int page, boolean includeReviews, 
                                            boolean includeComments) {
        this.owner = owner;
        this.repo = repo;
        this.principal = principal;
        this.requestId = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.state = state != null ? state : "all";
        this.sort = sort != null ? sort : "created";
        this.direction = direction != null ? direction : "desc";
        this.perPage = perPage > 0 ? perPage : 30;
        this.page = page > 0 ? page : 1;
        this.includeReviews = includeReviews;
        this.includeComments = includeComments;
    }
    
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    public String getEventType() {
        return "PULL_REQUEST_RETRIEVAL_REQUESTED";
    }
    
    public String getUsername() {
        return principal != null ? principal.getName() : "anonymous";
    }
    
    public boolean isOpenPRsOnly() {
        return "open".equals(state);
    }
    
    public boolean isClosedPRsOnly() {
        return "closed".equals(state);
    }
    
    public boolean isDetailedAnalysis() {
        return includeReviews || includeComments;
    }
} 