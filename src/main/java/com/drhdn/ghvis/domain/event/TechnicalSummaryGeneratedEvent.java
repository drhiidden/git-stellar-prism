package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Evento de dominio que representa la generación de un resumen técnico.
 * 
 * Este evento se dispara cuando se genera exitosamente un resumen técnico
 * de un repositorio, incluyendo tecnologías detectadas y métricas.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class TechnicalSummaryGeneratedEvent {
    
    private final String requestId;
    private final String owner;
    private final String repo;
    private final String username;
    private final Instant timestamp;
    private final long generationDurationMs;
    private final List<String> technologies;
    private final List<String> languages;
    private final int totalFiles;
    private final long totalSize;
    private final String primaryLanguage;
    private final double complexityScore;
    private final boolean fromCache;
    private final String summaryType; // "basic", "detailed", "comprehensive"
    
    public TechnicalSummaryGeneratedEvent(String requestId, String owner, String repo, 
                                        String username, long generationDurationMs,
                                        List<String> technologies, List<String> languages,
                                        int totalFiles, long totalSize, String primaryLanguage,
                                        double complexityScore, boolean fromCache, String summaryType) {
        this.requestId = requestId;
        this.owner = owner;
        this.repo = repo;
        this.username = username;
        this.timestamp = Instant.now();
        this.generationDurationMs = generationDurationMs;
        this.technologies = technologies != null ? technologies : List.of();
        this.languages = languages != null ? languages : List.of();
        this.totalFiles = totalFiles;
        this.totalSize = totalSize;
        this.primaryLanguage = primaryLanguage;
        this.complexityScore = complexityScore;
        this.fromCache = fromCache;
        this.summaryType = summaryType != null ? summaryType : "basic";
    }
    
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    public String getEventType() {
        return "TECHNICAL_SUMMARY_GENERATED";
    }
    
    public boolean isSuccessful() {
        return totalFiles >= 0 && totalSize >= 0 && complexityScore >= 0;
    }
    
    public int getTechnologyCount() {
        return technologies.size();
    }
    
    public int getLanguageCount() {
        return languages.size();
    }
    
    public boolean isComplexProject() {
        return complexityScore > 7.0;
    }
    
    public boolean isMultiLanguage() {
        return languages.size() > 1;
    }
} 