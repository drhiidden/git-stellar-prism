package com.drhdn.ghvis.domain.event.repository;

import com.drhdn.ghvis.domain.entity.Issue;
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
 * Evento de dominio para la adición de issues a un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryIssuesAddedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.issues.added";
    
    /**
     * Constructor a partir de un repositorio e issues.
     * 
     * @param repository Repositorio al que se añaden issues
     * @param issues Issues añadidos
     */
    public RepositoryIssuesAddedEvent(Repository repository, List<Issue> issues) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir información resumida de los issues al evento
        List<Map<String, Object>> issuesData = new ArrayList<>();
        
        if (issues != null) {
            issuesData = issues.stream()
                .map(issue -> {
                    Map<String, Object> issueData = new HashMap<>();
                    issueData.put("id", issue.getId());
                    issueData.put("number", issue.getNumber());
                    issueData.put("title", issue.getTitle());
                    issueData.put("state", issue.getState());
                    issueData.put("author", issue.getAuthor());
                    issueData.put("timestamp", issue.getTimestamp() != null ? 
                                            issue.getTimestamp().toString() : null);
                    
                    // Añadir etiquetas si existen
                    if (issue.getLabels() != null) {
                        List<String> labelNames = issue.getLabels().stream()
                            .map(Issue.Label::getName)
                            .collect(Collectors.toList());
                        issueData.put("labels", labelNames);
                    }
                    
                    return issueData;
                })
                .collect(Collectors.toList());
        }
        
        // Añadir todos los datos al evento
        addData("issues", issuesData);
        addData("issueCount", issues != null ? issues.size() : 0);
        addData("updatedAt", Instant.now().toString());
    }
} 