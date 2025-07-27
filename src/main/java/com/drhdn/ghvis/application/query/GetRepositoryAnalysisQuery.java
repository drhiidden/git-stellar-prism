package com.drhdn.ghvis.application.query;

import lombok.Builder;
import lombok.Value;

import java.security.Principal;

/**
 * Query para obtener análisis de repositorio.
 * 
 * Representa una consulta para obtener análisis de repositorios,
 * incluyendo filtros y opciones de paginación.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Value
@Builder
public class GetRepositoryAnalysisQuery {
    
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
     * ID de la consulta para trazabilidad
     */
    String queryId;
    
    /**
     * Timestamp de la consulta
     */
    long timestamp;
    
    /**
     * Crea una query con valores por defecto.
     */
    public static GetRepositoryAnalysisQuery create(String owner, String repo, Principal principal) {
        return GetRepositoryAnalysisQuery.builder()
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .includeCommits(true)
            .includeIssues(true)
            .includePullRequests(true)
            .includeTechnicalSummary(true)
            .queryId(java.util.UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea una query personalizada.
     */
    public static GetRepositoryAnalysisQuery createCustom(String owner, String repo, Principal principal,
                                                        boolean includeCommits, boolean includeIssues,
                                                        boolean includePullRequests, boolean includeTechnicalSummary) {
        return GetRepositoryAnalysisQuery.builder()
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .includeCommits(includeCommits)
            .includeIssues(includeIssues)
            .includePullRequests(includePullRequests)
            .includeTechnicalSummary(includeTechnicalSummary)
            .queryId(java.util.UUID.randomUUID().toString())
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
     * Verifica si es una consulta completa.
     */
    public boolean isFullQuery() {
        return includeCommits && includeIssues && includePullRequests && includeTechnicalSummary;
    }
} 