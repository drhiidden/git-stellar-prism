package com.drhdn.ghvis.domain.event.repository;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.entity.TechnicalSummary;
import com.drhdn.ghvis.domain.event.AbstractDomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Evento de dominio para la actualización del resumen técnico de un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryTechnicalSummaryUpdatedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.technical_summary.updated";
    
    /**
     * Constructor a partir de un repositorio.
     * 
     * @param repository Repositorio con resumen técnico actualizado
     */
    public RepositoryTechnicalSummaryUpdatedEvent(Repository repository) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir resumen técnico al evento
        Map<String, Object> summaryData = new HashMap<>();
        
        TechnicalSummary summary = repository.getTechnicalSummary();
        if (summary != null) {
            summaryData.put("languages", summary.getLanguages());
            summaryData.put("technologies", summary.getTechnologies());
            summaryData.put("totalFiles", summary.getTotalFiles());
            summaryData.put("totalSize", summary.getTotalSize());
            summaryData.put("primaryLanguage", summary.getPrimaryLanguage());
            summaryData.put("complexityScore", summary.getComplexityScore());
        }
        
        // Añadir todos los datos al evento
        addData("technicalSummary", summaryData);
        addData("updatedAt", Instant.now().toString());
    }
} 