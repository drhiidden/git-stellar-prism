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
 * Evento de dominio para la actualización de estadísticas de un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryStatsUpdatedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.stats.updated";
    
    /**
     * Constructor a partir de un repositorio.
     * 
     * @param repository Repositorio con estadísticas actualizadas
     */
    public RepositoryStatsUpdatedEvent(Repository repository) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir estadísticas actualizadas al evento
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("stargazersCount", repository.getStargazersCount());
        statsData.put("forksCount", repository.getForksCount());
        statsData.put("watchersCount", repository.getWatchersCount());
        statsData.put("openIssuesCount", repository.getOpenIssuesCount());
        statsData.put("size", repository.getSize());
        statsData.put("updatedAt", repository.getUpdatedAt() != null ? 
                                 repository.getUpdatedAt().toString() : null);
        
        // Añadir todos los datos al evento
        addData("stats", statsData);
        addData("updatedAt", Instant.now().toString());
    }
} 