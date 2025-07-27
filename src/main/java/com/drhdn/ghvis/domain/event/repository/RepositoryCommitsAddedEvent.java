package com.drhdn.ghvis.domain.event.repository;

import com.drhdn.ghvis.domain.entity.Commit;
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
 * Evento de dominio para la adición de commits a un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RepositoryCommitsAddedEvent extends AbstractDomainEvent {
    
    private static final String EVENT_TYPE = "repository.commits.added";
    
    /**
     * Constructor a partir de un repositorio y commits.
     * 
     * @param repository Repositorio al que se añaden commits
     * @param commits Commits añadidos
     */
    public RepositoryCommitsAddedEvent(Repository repository, List<Commit> commits) {
        super(EVENT_TYPE, 
              repository.getOwner() + "/" + repository.getName(), 
              "repository", 
              repository.getVersion(), 
              "system");
        
        // Añadir información resumida de los commits al evento
        List<Map<String, Object>> commitsData = new ArrayList<>();
        
        if (commits != null) {
            commitsData = commits.stream()
                .map(commit -> {
                    Map<String, Object> commitData = new HashMap<>();
                    commitData.put("hash", commit.getHash());
                    commitData.put("message", commit.getMessage());
                    commitData.put("author", commit.getAuthor());
                    commitData.put("timestamp", commit.getTimestamp() != null ? 
                                             commit.getTimestamp().toString() : null);
                    return commitData;
                })
                .collect(Collectors.toList());
        }
        
        // Añadir todos los datos al evento
        addData("commits", commitsData);
        addData("commitCount", commits != null ? commits.size() : 0);
        addData("updatedAt", Instant.now().toString());
    }
} 