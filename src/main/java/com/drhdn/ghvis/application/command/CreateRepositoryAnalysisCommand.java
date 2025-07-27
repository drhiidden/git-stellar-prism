package com.drhdn.ghvis.application.command;

import lombok.Builder;
import lombok.Value;

import java.security.Principal;

/**
 * Command para crear un análisis de repositorio.
 * 
 * Representa la intención de crear un análisis completo de un repositorio,
 * incluyendo commits, issues, pull requests y resumen técnico.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Value
@Builder
public class CreateRepositoryAnalysisCommand {
    
    /**
     * Propietario del repositorio
     */
    String owner;
    
    /**
     * Nombre del repositorio
     */
    String repo;
    
    /**
     * Usuario autenticado
     */
    Principal principal;
    
    /**
     * Incluir análisis de commits
     */
    boolean includeCommits;
    
    /**
     * Incluir análisis de issues
     */
    boolean includeIssues;
    
    /**
     * Incluir análisis de pull requests
     */
    boolean includePullRequests;
    
    /**
     * Incluir resumen técnico
     */
    boolean includeTechnicalSummary;
    
    /**
     * ID de la solicitud para trazabilidad
     */
    String requestId;
    
    /**
     * Timestamp de creación del command
     */
    long timestamp;
    
    /**
     * Crea un command con valores por defecto.
     */
    public static CreateRepositoryAnalysisCommand create(String owner, String repo, Principal principal) {
        return CreateRepositoryAnalysisCommand.builder()
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .includeCommits(true)
            .includeIssues(true)
            .includePullRequests(true)
            .includeTechnicalSummary(true)
            .requestId(java.util.UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea un command personalizado.
     */
    public static CreateRepositoryAnalysisCommand createCustom(String owner, String repo, Principal principal,
                                                             boolean includeCommits, boolean includeIssues,
                                                             boolean includePullRequests, boolean includeTechnicalSummary) {
        return CreateRepositoryAnalysisCommand.builder()
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .includeCommits(includeCommits)
            .includeIssues(includeIssues)
            .includePullRequests(includePullRequests)
            .includeTechnicalSummary(includeTechnicalSummary)
            .requestId(java.util.UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Obtiene el nombre completo del repositorio.
     */
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    /**
     * Verifica si es un análisis completo.
     */
    public boolean isFullAnalysis() {
        return includeCommits && includeIssues && includePullRequests && includeTechnicalSummary;
    }
} 