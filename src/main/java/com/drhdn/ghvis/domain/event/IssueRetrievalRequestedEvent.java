package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.security.Principal;

/**
 * Evento de dominio que representa una solicitud de issues.
 * 
 * Este evento se dispara cuando se solicita la obtención de issues
 * de un repositorio, permitiendo análisis de problemas y métricas.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class IssueRetrievalRequestedEvent {
    
    private final String owner;
    private final String repo;
    private final Principal principal;
    private final String requestId;
    private final Instant timestamp;
    private final String state; // "open", "closed", "all"
    private final String sort; // "created", "updated", "comments"
    private final String direction; // "asc", "desc"
    private final int perPage;
    private final int page;
    
    public IssueRetrievalRequestedEvent(String owner, String repo, Principal principal, 
                                      String state, String sort, String direction, 
                                      int perPage, int page) {
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
    }
    
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    public String getEventType() {
        return "ISSUE_RETRIEVAL_REQUESTED";
    }
    
    public String getUsername() {
        return principal != null ? principal.getName() : "anonymous";
    }
    
    public boolean isOpenIssuesOnly() {
        return "open".equals(state);
    }
    
    public boolean isClosedIssuesOnly() {
        return "closed".equals(state);
    }
} 