package com.drhdn.ghvis.domain.event.repository;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.event.AbstractDomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Evento de dominio para el desarchivado de un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryUnarchivedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.unarchived";
    
    /**
     * Constructor a partir de un repositorio.
     * 
     * @param repository Repositorio desarchivado
     */
    public RepositoryUnarchivedEvent(Repository repository) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir datos básicos al evento
        addData("repositoryId", repository.getId());
        addData("repositoryName", repository.getName());
        addData("repositoryOwner", repository.getOwner());
        addData("unarchivedAt", Instant.now().toString());
    }
} 