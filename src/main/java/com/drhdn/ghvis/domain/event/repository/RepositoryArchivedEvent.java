package com.drhdn.ghvis.domain.event.repository;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.event.AbstractDomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Evento de dominio para el archivado de un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryArchivedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.archived";
    
    /**
     * Constructor a partir de un repositorio.
     * 
     * @param repository Repositorio archivado
     */
    public RepositoryArchivedEvent(Repository repository) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir datos básicos al evento
        addData("repositoryId", repository.getId());
        addData("repositoryName", repository.getName());
        addData("repositoryOwner", repository.getOwner());
        addData("archivedAt", Instant.now().toString());
    }
} 