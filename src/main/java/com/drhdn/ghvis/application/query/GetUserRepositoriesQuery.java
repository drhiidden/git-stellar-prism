package com.drhdn.ghvis.application.query;

import lombok.Builder;
import lombok.Value;

import java.security.Principal;
import java.util.UUID;

/**
 * Query para obtener repositorios de un usuario.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Value
@Builder
public class GetUserRepositoriesQuery {
    
    /**
     * Nombre de usuario del cual obtener los repositorios
     */
    String username;
    
    /**
     * Usuario autenticado que realiza la consulta
     */
    Principal principal;
    
    /**
     * Tipo de repositorios a obtener (all, public, private, detailed)
     */
    String repositoryType;
    
    /**
     * Si incluir detalles completos
     */
    boolean includeDetails;
    
    /**
     * ID único de la query
     */
    String queryId;
    
    /**
     * Timestamp de la query
     */
    long timestamp;
    
    /**
     * Crea una query para obtener todos los repositorios de un usuario
     */
    public static GetUserRepositoriesQuery createAll(String username, Principal principal) {
        return GetUserRepositoriesQuery.builder()
            .username(username)
            .principal(principal)
            .repositoryType("all")
            .includeDetails(false)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea una query para obtener repositorios públicos de un usuario
     */
    public static GetUserRepositoriesQuery createPublic(String username, Principal principal) {
        return GetUserRepositoriesQuery.builder()
            .username(username)
            .principal(principal)
            .repositoryType("public")
            .includeDetails(false)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea una query para obtener repositorios con detalles de un usuario
     */
    public static GetUserRepositoriesQuery createDetailed(String username, Principal principal) {
        return GetUserRepositoriesQuery.builder()
            .username(username)
            .principal(principal)
            .repositoryType("detailed")
            .includeDetails(true)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Crea una query personalizada
     */
    public static GetUserRepositoriesQuery createCustom(String username, Principal principal, 
                                                       String repositoryType, boolean includeDetails) {
        return GetUserRepositoriesQuery.builder()
            .username(username)
            .principal(principal)
            .repositoryType(repositoryType)
            .includeDetails(includeDetails)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Obtiene el nombre completo del usuario
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Verifica si es una query para todos los repositorios
     */
    public boolean isAllRepositories() {
        return "all".equals(repositoryType);
    }
    
    /**
     * Verifica si es una query para repositorios públicos
     */
    public boolean isPublicRepositories() {
        return "public".equals(repositoryType);
    }
    
    /**
     * Verifica si es una query para repositorios con detalles
     */
    public boolean isDetailedRepositories() {
        return "detailed".equals(repositoryType) || includeDetails;
    }
    
    /**
     * Obtiene la clave de cache para esta query
     */
    public String getCacheKey() {
        return String.format("user:%s:repositories:%s", username, repositoryType);
    }
} 