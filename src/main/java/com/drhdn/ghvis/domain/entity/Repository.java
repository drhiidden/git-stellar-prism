package com.drhdn.ghvis.domain.entity;

import com.drhdn.ghvis.domain.event.AbstractEventSourcedAggregate;
import com.drhdn.ghvis.domain.event.DomainEvent;
import com.drhdn.ghvis.domain.event.EventSourcedAggregate;
import com.drhdn.ghvis.domain.event.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Entidad que representa un repositorio de GitHub.
 * Implementa EventSourcedAggregate para soportar Event Sourcing.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Repository extends AbstractEventSourcedAggregate {
    
    private Long id;
    private String name;
    private String owner;
    private String description;
    private String url;
    private String defaultBranch;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant pushedAt;
    private Integer stargazersCount;
    private Integer forksCount;
    private Integer watchersCount;
    private Integer openIssuesCount;
    private Integer size;
    private boolean fork;
    private boolean isPrivate;
    private boolean archived;
    private Map<String, Long> languageDistribution;
    private List<String> topics;
    private List<Commit> recentCommits;
    private List<Issue> recentIssues;
    private List<PullRequest> recentPullRequests;
    private TechnicalSummary technicalSummary;
    
    // Eventos pendientes de persistir
    private transient List<DomainEvent> pendingEvents = new ArrayList<>();
    
    @Builder
    public Repository(Long id, String name, String owner, String description, String url,
                    String defaultBranch, Instant createdAt, Instant updatedAt, Instant pushedAt,
                    Integer stargazersCount, Integer forksCount, Integer watchersCount,
                    Integer openIssuesCount, Integer size, boolean fork, boolean isPrivate,
                    boolean archived, Map<String, Long> languageDistribution, List<String> topics) {
        super(owner + "/" + name, "repository");
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.description = description;
        this.url = url;
        this.defaultBranch = defaultBranch;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.pushedAt = pushedAt;
        this.stargazersCount = stargazersCount;
        this.forksCount = forksCount;
        this.watchersCount = watchersCount;
        this.openIssuesCount = openIssuesCount;
        this.size = size;
        this.fork = fork;
        this.isPrivate = isPrivate;
        this.archived = archived;
        this.languageDistribution = languageDistribution;
        this.topics = topics;
        this.recentCommits = new ArrayList<>();
        this.recentIssues = new ArrayList<>();
        this.recentPullRequests = new ArrayList<>();
        
        // Registrar evento de creación
        registerEvent(new RepositoryCreatedEvent(this));
    }
    
    /**
     * Actualiza la información básica del repositorio.
     * 
     * @param description Nueva descripción
     * @param defaultBranch Nuevo branch por defecto
     * @param topics Nuevos topics
     * @return El repositorio actualizado
     */
    public Repository updateInfo(String description, String defaultBranch, List<String> topics) {
        this.description = description;
        this.defaultBranch = defaultBranch;
        this.topics = topics;
        this.updatedAt = Instant.now();
        
        // Registrar evento de actualización
        registerEvent(new RepositoryUpdatedEvent(this));
        
        return this;
    }
    
    /**
     * Actualiza las estadísticas del repositorio.
     * 
     * @param stargazersCount Nuevo conteo de estrellas
     * @param forksCount Nuevo conteo de forks
     * @param watchersCount Nuevo conteo de watchers
     * @param openIssuesCount Nuevo conteo de issues abiertos
     * @param size Nuevo tamaño
     * @return El repositorio actualizado
     */
    public Repository updateStats(Integer stargazersCount, Integer forksCount, 
                               Integer watchersCount, Integer openIssuesCount, Integer size) {
        this.stargazersCount = stargazersCount;
        this.forksCount = forksCount;
        this.watchersCount = watchersCount;
        this.openIssuesCount = openIssuesCount;
        this.size = size;
        this.updatedAt = Instant.now();
        
        // Registrar evento de actualización de estadísticas
        registerEvent(new RepositoryStatsUpdatedEvent(this));
        
        return this;
    }
    
    /**
     * Actualiza la distribución de lenguajes del repositorio.
     * 
     * @param languageDistribution Nueva distribución de lenguajes
     * @return El repositorio actualizado
     */
    public Repository updateLanguages(Map<String, Long> languageDistribution) {
        this.languageDistribution = languageDistribution;
        this.updatedAt = Instant.now();
        
        // Registrar evento de actualización de lenguajes
        registerEvent(new RepositoryLanguagesUpdatedEvent(this));
        
        return this;
    }
    
    /**
     * Añade commits recientes al repositorio.
     * 
     * @param commits Commits a añadir
     * @return El repositorio actualizado
     */
    public Repository addCommits(List<Commit> commits) {
        if (this.recentCommits == null) {
            this.recentCommits = new ArrayList<>();
        }
        this.recentCommits.addAll(commits);
        this.updatedAt = Instant.now();
        
        // Registrar evento de adición de commits
        registerEvent(new RepositoryCommitsAddedEvent(this, commits));
        
        return this;
    }
    
    /**
     * Añade issues recientes al repositorio.
     * 
     * @param issues Issues a añadir
     * @return El repositorio actualizado
     */
    public Repository addIssues(List<Issue> issues) {
        if (this.recentIssues == null) {
            this.recentIssues = new ArrayList<>();
        }
        this.recentIssues.addAll(issues);
        this.updatedAt = Instant.now();
        
        // Registrar evento de adición de issues
        registerEvent(new RepositoryIssuesAddedEvent(this, issues));
        
        return this;
    }
    
    /**
     * Añade pull requests recientes al repositorio.
     * 
     * @param pullRequests Pull requests a añadir
     * @return El repositorio actualizado
     */
    public Repository addPullRequests(List<PullRequest> pullRequests) {
        if (this.recentPullRequests == null) {
            this.recentPullRequests = new ArrayList<>();
        }
        this.recentPullRequests.addAll(pullRequests);
        this.updatedAt = Instant.now();
        
        // Registrar evento de adición de pull requests
        registerEvent(new RepositoryPullRequestsAddedEvent(this, pullRequests));
        
        return this;
    }
    
    /**
     * Establece el resumen técnico del repositorio.
     * 
     * @param technicalSummary Resumen técnico
     * @return El repositorio actualizado
     */
    public Repository setTechnicalSummary(TechnicalSummary technicalSummary) {
        this.technicalSummary = technicalSummary;
        this.updatedAt = Instant.now();
        
        // Registrar evento de actualización de resumen técnico
        registerEvent(new RepositoryTechnicalSummaryUpdatedEvent(this));
        
        return this;
    }
    
    /**
     * Archiva o desarchiva el repositorio.
     * 
     * @param archived Si el repositorio debe estar archivado
     * @return El repositorio actualizado
     */
    public Repository setArchived(boolean archived) {
        this.archived = archived;
        this.updatedAt = Instant.now();
        
        // Registrar evento de archivado/desarchivado
        if (archived) {
            registerEvent(new RepositoryArchivedEvent(this));
        } else {
            registerEvent(new RepositoryUnarchivedEvent(this));
        }
        
        return this;
    }
    
    /**
     * Obtiene los eventos pendientes de persistir y limpia la lista.
     * 
     * @return Lista de eventos pendientes
     */
    public List<DomainEvent> getPendingEvents() {
        List<DomainEvent> events = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return events;
    }
    
    /**
     * Registra un evento para ser persistido posteriormente.
     * 
     * @param event Evento a registrar
     */
    private void registerEvent(DomainEvent event) {
        if (pendingEvents == null) {
            pendingEvents = new ArrayList<>();
        }
        pendingEvents.add(event);
        incrementVersion();
    }
    
    @Override
    @SuppressWarnings("unchecked") // Event sourcing requires dynamic casting from generic event data
    public EventSourcedAggregate apply(DomainEvent event) {
        if (!belongsToAggregate(event)) {
            return this;
        }
        
        // Implementar la aplicación de eventos específicos
        if (event instanceof RepositoryCreatedEvent) {
            Map<String, Object> repositoryData = (Map<String, Object>) event.getData().get("repository");
            if (repositoryData != null) {
                this.id = getLongValue(repositoryData, "id");
                this.name = (String) repositoryData.get("name");
                this.owner = (String) repositoryData.get("owner");
                this.description = (String) repositoryData.get("description");
                this.url = (String) repositoryData.get("url");
                this.defaultBranch = (String) repositoryData.get("defaultBranch");
                this.isPrivate = getBooleanValue(repositoryData, "isPrivate");
                this.fork = getBooleanValue(repositoryData, "fork");
                
                String createdAtStr = (String) repositoryData.get("createdAt");
                if (createdAtStr != null) {
                    this.createdAt = Instant.parse(createdAtStr);
                }
            }
            
            Map<String, Object> statsData = (Map<String, Object>) event.getData().get("stats");
            if (statsData != null) {
                this.stargazersCount = getIntegerValue(statsData, "stargazersCount");
                this.forksCount = getIntegerValue(statsData, "forksCount");
                this.watchersCount = getIntegerValue(statsData, "watchersCount");
                this.openIssuesCount = getIntegerValue(statsData, "openIssuesCount");
                this.size = getIntegerValue(statsData, "size");
            }
        } else if (event instanceof RepositoryUpdatedEvent) {
            Map<String, Object> repositoryData = (Map<String, Object>) event.getData().get("repository");
            if (repositoryData != null) {
                this.description = (String) repositoryData.get("description");
                this.defaultBranch = (String) repositoryData.get("defaultBranch");
                
                if (repositoryData.get("topics") instanceof List) {
                    this.topics = (List<String>) repositoryData.get("topics");
                }
                
                String updatedAtStr = (String) repositoryData.get("updatedAt");
                if (updatedAtStr != null) {
                    this.updatedAt = Instant.parse(updatedAtStr);
                }
            }
        } else if (event instanceof RepositoryStatsUpdatedEvent) {
            Map<String, Object> statsData = (Map<String, Object>) event.getData().get("stats");
            if (statsData != null) {
                this.stargazersCount = getIntegerValue(statsData, "stargazersCount");
                this.forksCount = getIntegerValue(statsData, "forksCount");
                this.watchersCount = getIntegerValue(statsData, "watchersCount");
                this.openIssuesCount = getIntegerValue(statsData, "openIssuesCount");
                this.size = getIntegerValue(statsData, "size");
                
                String updatedAtStr = (String) statsData.get("updatedAt");
                if (updatedAtStr != null) {
                    this.updatedAt = Instant.parse(updatedAtStr);
                }
            }
        } else if (event instanceof RepositoryLanguagesUpdatedEvent) {
            Map<String, Object> languageData = (Map<String, Object>) event.getData().get("languages");
            if (languageData != null) {
                this.languageDistribution = new HashMap<>();
                for (Map.Entry<String, Object> entry : languageData.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        this.languageDistribution.put(entry.getKey(), ((Number) entry.getValue()).longValue());
                    }
                }
            }
        } else if (event instanceof RepositoryCommitsAddedEvent) {
            List<Map<String, Object>> commitsData = (List<Map<String, Object>>) event.getData().get("commits");
            if (commitsData != null) {
                if (this.recentCommits == null) {
                    this.recentCommits = new ArrayList<>();
                }
                
                for (Map<String, Object> commitData : commitsData) {
                    Commit commit = Commit.builder()
                        .hash((String) commitData.get("hash"))
                        .message((String) commitData.get("message"))
                        .author((String) commitData.get("author"))
                        .build();
                    
                    String timestampStr = (String) commitData.get("timestamp");
                    if (timestampStr != null) {
                        commit.setTimestamp(Instant.parse(timestampStr));
                    }
                    
                    this.recentCommits.add(commit);
                }
            }
        } else if (event instanceof RepositoryIssuesAddedEvent) {
            List<Map<String, Object>> issuesData = (List<Map<String, Object>>) event.getData().get("issues");
            if (issuesData != null) {
                if (this.recentIssues == null) {
                    this.recentIssues = new ArrayList<>();
                }
                
                for (Map<String, Object> issueData : issuesData) {
                    Issue issue = Issue.builder()
                        .id(getLongValue(issueData, "id"))
                        .number(getIntegerValue(issueData, "number"))
                        .title((String) issueData.get("title"))
                        .state((String) issueData.get("state"))
                        .author((String) issueData.get("author"))
                        .build();
                    
                    String timestampStr = (String) issueData.get("timestamp");
                    if (timestampStr != null) {
                        issue.setTimestamp(Instant.parse(timestampStr));
                    }
                    
                    this.recentIssues.add(issue);
                }
            }
        } else if (event instanceof RepositoryPullRequestsAddedEvent) {
            List<Map<String, Object>> prsData = (List<Map<String, Object>>) event.getData().get("pullRequests");
            if (prsData != null) {
                if (this.recentPullRequests == null) {
                    this.recentPullRequests = new ArrayList<>();
                }
                
                for (Map<String, Object> prData : prsData) {
                    PullRequest pr = PullRequest.builder()
                        .id(getLongValue(prData, "id"))
                        .number(getIntegerValue(prData, "number"))
                        .title((String) prData.get("title"))
                        .state((String) prData.get("state"))
                        .author((String) prData.get("author"))
                        .headBranch((String) prData.get("headBranch"))
                        .baseBranch((String) prData.get("baseBranch"))
                        .build();
                    
                    String timestampStr = (String) prData.get("timestamp");
                    if (timestampStr != null) {
                        pr.setTimestamp(Instant.parse(timestampStr));
                    }
                    
                    this.recentPullRequests.add(pr);
                }
            }
        } else if (event instanceof RepositoryTechnicalSummaryUpdatedEvent) {
            Map<String, Object> summaryData = (Map<String, Object>) event.getData().get("technicalSummary");
            if (summaryData != null) {
                if (this.technicalSummary == null) {
                    this.technicalSummary = new TechnicalSummary();
                }
                
                if (summaryData.get("languages") instanceof List) {
                    this.technicalSummary.setLanguages((List<String>) summaryData.get("languages"));
                }
                
                if (summaryData.get("technologies") instanceof List) {
                    this.technicalSummary.setTechnologies((List<String>) summaryData.get("technologies"));
                }
                
                this.technicalSummary.setTotalFiles(getIntegerValue(summaryData, "totalFiles"));
                this.technicalSummary.setTotalSize(getLongValue(summaryData, "totalSize"));
                this.technicalSummary.setPrimaryLanguage((String) summaryData.get("primaryLanguage"));
                this.technicalSummary.setComplexityScore(getDoubleValue(summaryData, "complexityScore"));
            }
        } else if (event instanceof RepositoryArchivedEvent) {
            this.archived = true;
        } else if (event instanceof RepositoryUnarchivedEvent) {
            this.archived = false;
        }
        
        // Actualizar la versión del agregado
        this.version = event.getVersion();
        
        return this;
    }
    
    // Métodos auxiliares para obtener valores tipados de mapas
    
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
    
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
} 