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
 * Evento de dominio para la actualización de lenguajes de un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryLanguagesUpdatedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.languages.updated";
    
    /**
     * Constructor a partir de un repositorio.
     * 
     * @param repository Repositorio con lenguajes actualizados
     */
    public RepositoryLanguagesUpdatedEvent(Repository repository) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir distribución de lenguajes al evento
        Map<String, Object> languageData = new HashMap<>();
        if (repository.getLanguageDistribution() != null) {
            languageData.putAll(repository.getLanguageDistribution().entrySet().stream()
                .collect(HashMap::new, 
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()), 
                        HashMap::putAll));
        }
        
        // Añadir todos los datos al evento
        addData("languages", languageData);
        addData("updatedAt", Instant.now().toString());
        
        // Calcular lenguaje principal
        if (repository.getLanguageDistribution() != null && !repository.getLanguageDistribution().isEmpty()) {
            String primaryLanguage = repository.getLanguageDistribution().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (primaryLanguage != null) {
                addData("primaryLanguage", primaryLanguage);
            }
        }
    }
} 