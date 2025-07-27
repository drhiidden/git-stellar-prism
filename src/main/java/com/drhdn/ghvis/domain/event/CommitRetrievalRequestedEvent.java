package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.security.Principal;

/**
 * Evento de dominio que representa una solicitud de commits.
 * 
 * Este evento se dispara cuando se solicita la obtención de commits
 * de un repositorio, permitiendo desacoplar la lógica de resiliencia
 * y monitoreo.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class CommitRetrievalRequestedEvent {
    
    private final String owner;
    private final String repo;
    private final Principal principal;
    private final String requestId;
    private final Instant timestamp;
    private final boolean resilienceEnabled;
    
    public CommitRetrievalRequestedEvent(String owner, String repo, Principal principal, boolean resilienceEnabled) {
        this.owner = owner;
        this.repo = repo;
        this.principal = principal;
        this.requestId = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.resilienceEnabled = resilienceEnabled;
    }
    
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    public String getEventType() {
        return "COMMIT_RETRIEVAL_REQUESTED";
    }
    
    public String getUsername() {
        return principal != null ? principal.getName() : "anonymous";
    }
} 