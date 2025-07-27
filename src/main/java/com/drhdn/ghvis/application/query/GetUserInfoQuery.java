package com.drhdn.ghvis.application.query;

import lombok.Builder;
import lombok.Value;

import java.security.Principal;
import java.util.UUID;

/**
 * Query para obtener información de un usuario.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Value
@Builder
public class GetUserInfoQuery {
    
    /**
     * Nombre de usuario del cual obtener la información
     */
    String username;
    
    /**
     * Autenticación del usuario que realiza la consulta
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
     * Crea una query para obtener información de un usuario
     */
    public static GetUserInfoQuery create(String username, Principal principal) {
        return GetUserInfoQuery.builder()
            .username(username)
            .principal(principal)
            .queryId(UUID.randomUUID().toString())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Obtiene el nombre de usuario
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Obtiene la clave de cache para esta query
     */
    public String getCacheKey() {
        return String.format("user:%s:info", username);
    }
} 