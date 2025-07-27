package com.drhdn.ghvis.domain.event.repository;

import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.event.AbstractDomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Evento de dominio para la adición de pull requests a un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryPullRequestsAddedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.pull_requests.added";
    
    /**
     * Constructor a partir de un repositorio y pull requests.
     * 
     * @param repository Repositorio al que se añaden pull requests
     * @param pullRequests Pull requests añadidos
     */
    public RepositoryPullRequestsAddedEvent(Repository repository, List<PullRequest> pullRequests) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir información resumida de los pull requests al evento
        List<Map<String, Object>> prsData = new ArrayList<>();
        
        if (pullRequests != null) {
            prsData = pullRequests.stream()
                .map(pr -> {
                    Map<String, Object> prData = new HashMap<>();
                    prData.put("id", pr.getId());
                    prData.put("number", pr.getNumber());
                    prData.put("title", pr.getTitle());
                    prData.put("state", pr.getState());
                    prData.put("author", pr.getAuthor());
                    prData.put("timestamp", pr.getTimestamp() != null ? 
                                         pr.getTimestamp().toString() : null);
                    prData.put("headBranch", pr.getHeadBranch());
                    prData.put("baseBranch", pr.getBaseBranch());
                    prData.put("merged", pr.getMergedAt() != null);
                    return prData;
                })
                .collect(Collectors.toList());
        }
        
        // Añadir todos los datos al evento
        addData("pullRequests", prsData);
        addData("pullRequestCount", pullRequests != null ? pullRequests.size() : 0);
        addData("updatedAt", Instant.now().toString());
    }
} 