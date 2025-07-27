package com.drhdn.ghvis.application.query;

import lombok.Builder;
import lombok.Value;

import java.security.Principal;
import java.util.UUID;

/**
 * Query para obtener detalles de elementos de repositorio (PR, Issue, etc.).
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Value
@Builder
public class GetRepositoryDetailQuery {
    
    /**
     * Propietario del repositorio
     */
    String owner;
    
    /**
     * Nombre del repositorio
     */
    String repo;
    
    /**
     * Número del elemento (PR, Issue)
     */
    Integer number;
    
    /**
     * SHA del commit (para commits)
     */
    String sha;
    
    /**
     * Tipo de detalle a obtener
     */
    DetailType detailType;
    
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
     * Tipos de detalles disponibles
     */
    public enum DetailType {
        PULL_REQUEST,
        ISSUE,
        COMMIT
    }
    
    /**
     * Crea una query para obtener detalles de un Pull Request
     */
    public static GetRepositoryDetailQuery createPullRequestQuery(String owner, String repo, Integer number, Principal principal) {
        return GetRepositoryDetailQuery.builder()
            .owner(owner)
            .repo(repo)
            .number(number)
            .detailType(DetailType.PULL_REQUEST)
            .principal(principal)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea una query para obtener detalles de un Issue
     */
    public static GetRepositoryDetailQuery createIssueQuery(String owner, String repo, Integer number, Principal principal) {
        return GetRepositoryDetailQuery.builder()
            .owner(owner)
            .repo(repo)
            .number(number)
            .detailType(DetailType.ISSUE)
            .principal(principal)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea una query para obtener detalles de un Commit
     */
    public static GetRepositoryDetailQuery createCommitQuery(String owner, String repo, String sha, Principal principal) {
        return GetRepositoryDetailQuery.builder()
            .owner(owner)
            .repo(repo)
            .sha(sha)
            .detailType(DetailType.COMMIT)
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
        return String.format("repo:%s:%s:%s:%s", owner, repo, detailType.name().toLowerCase(), 
            detailType == DetailType.COMMIT ? sha : number);
    }
} 