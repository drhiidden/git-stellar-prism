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
 * Evento de dominio para la actualización de un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryUpdatedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.updated";
    
    /**
     * Constructor a partir de un repositorio.
     * 
     * @param repository Repositorio actualizado
     */
    public RepositoryUpdatedEvent(Repository repository) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir datos actualizados del repositorio al evento
        Map<String, Object> repositoryData = new HashMap<>();
        repositoryData.put("id", repository.getId());
        repositoryData.put("name", repository.getName());
        repositoryData.put("owner", repository.getOwner());
        repositoryData.put("description", repository.getDescription());
        repositoryData.put("url", repository.getUrl());
        repositoryData.put("defaultBranch", repository.getDefaultBranch());
        repositoryData.put("updatedAt", repository.getUpdatedAt() != null ? 
                                      repository.getUpdatedAt().toString() : null);
        repositoryData.put("topics", repository.getTopics());
        
        // Añadir todos los datos al evento
        addData("repository", repositoryData);
        addData("updatedAt", Instant.now().toString());
    }
} 