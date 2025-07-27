package com.drhdn.ghvis.application.query;

import lombok.Builder;
import lombok.Value;

import java.security.Principal;
import java.util.UUID;

/**
 * Query para obtener análisis de repositorio.
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
     * Si incluir distribución de lenguajes
     */
    boolean includeLanguages;
    
    /**
     * Si incluir tecnologías
     */
    boolean includeTechnologies;
    
    /**
     * Si incluir estructura del proyecto
     */
    boolean includeProjectStructure;
    
    /**
     * Si incluir resumen técnico
     */
    boolean includeTechnicalSummary;
    
    /**
     * Usuario autenticado
     */
    Principal principal;
    
    /**
     * ID único de la query
     */
    String queryId;
    
    /**
     * Timestamp de la query
     */
    long timestamp;
    
    /**
     * Crea una query para análisis completo de repositorio
     */
    public static GetRepositoryAnalysisQuery createFullAnalysis(String owner, String repo, Principal principal) {
        return GetRepositoryAnalysisQuery.builder()
            .owner(owner)
            .repo(repo)
            .includeLanguages(true)
            .includeTechnologies(true)
            .includeProjectStructure(true)
            .includeTechnicalSummary(true)
            .principal(principal)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea una query para análisis básico de repositorio (solo lenguajes)
     */
    public static GetRepositoryAnalysisQuery createBasicAnalysis(String owner, String repo, Principal principal) {
        return GetRepositoryAnalysisQuery.builder()
            .owner(owner)
            .repo(repo)
            .includeLanguages(true)
            .includeTechnologies(false)
            .includeProjectStructure(false)
            .includeTechnicalSummary(false)
            .principal(principal)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea una query para análisis personalizado de repositorio
     */
    public static GetRepositoryAnalysisQuery createCustomAnalysis(
            String owner, String repo, Principal principal,
            boolean includeLanguages, boolean includeTechnologies,
            boolean includeProjectStructure, boolean includeTechnicalSummary) {
        return GetRepositoryAnalysisQuery.builder()
            .owner(owner)
            .repo(repo)
            .includeLanguages(includeLanguages)
            .includeTechnologies(includeTechnologies)
            .includeProjectStructure(includeProjectStructure)
            .includeTechnicalSummary(includeTechnicalSummary)
            .principal(principal)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Obtiene el nombre completo del repositorio
     */
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }
    
    /**
     * Obtiene la clave de cache para esta query
     */
    public String getCacheKey() {
        return String.format("repo:%s:%s:analysis:%s-%s-%s-%s", 
            owner, repo,
            includeLanguages ? "L" : "",
            includeTechnologies ? "T" : "",
            includeProjectStructure ? "P" : "",
            includeTechnicalSummary ? "S" : "");
    }
} 