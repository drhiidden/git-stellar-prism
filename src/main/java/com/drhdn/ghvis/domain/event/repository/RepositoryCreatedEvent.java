package com.drhdn.ghvis.domain.event.repository;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.event.AbstractDomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Evento de dominio para la creación de un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryCreatedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.created";
    
    /**
     * Constructor a partir de un repositorio.
     * 
     * @param repository Repositorio creado
     */
    public RepositoryCreatedEvent(Repository repository) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir datos del repositorio al evento
        Map<String, Object> repositoryData = new HashMap<>();
        repositoryData.put("id", repository.getId());
        repositoryData.put("name", repository.getName());
        repositoryData.put("owner", repository.getOwner());
        repositoryData.put("description", repository.getDescription());
        repositoryData.put("url", repository.getUrl());
        repositoryData.put("defaultBranch", repository.getDefaultBranch());
        repositoryData.put("createdAt", repository.getCreatedAt() != null ? 
                                      repository.getCreatedAt().toString() : null);
        repositoryData.put("isPrivate", repository.isPrivate());
        repositoryData.put("fork", repository.isFork());
        
        // Añadir estadísticas básicas
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("stargazersCount", repository.getStargazersCount());
        statsData.put("forksCount", repository.getForksCount());
        statsData.put("watchersCount", repository.getWatchersCount());
        statsData.put("openIssuesCount", repository.getOpenIssuesCount());
        statsData.put("size", repository.getSize());
        
        // Añadir todos los datos al evento
        addData("repository", repositoryData);
        addData("stats", statsData);
        addData("createdAt", Instant.now().toString());
    }
} 