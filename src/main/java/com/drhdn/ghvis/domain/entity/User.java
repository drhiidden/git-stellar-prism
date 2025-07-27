package com.drhdn.ghvis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entidad que representa un usuario de GitHub.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * ID único del usuario en GitHub
     */
    private Long id;
    
    /**
     * Nombre de usuario (login)
     */
    private String login;
    
    /**
     * Nombre completo del usuario
     */
    private String name;
    
    /**
     * Email del usuario
     */
    private String email;
    
    /**
     * URL del avatar del usuario
     */
    private String avatarUrl;
    
    /**
     * Biografía del usuario
     */
    private String bio;
    
    /**
     * Ubicación del usuario
     */
    private String location;
    
    /**
     * Número de repositorios públicos
     */
    private Integer publicRepos;
    
    /**
     * Número de repositorios privados
     */
    private Integer totalPrivateRepos;
    
    /**
     * Número de seguidores
     */
    private Integer followers;
    
    /**
     * Número de usuarios que sigue
     */
    private Integer following;
    
    /**
     * Fecha de creación de la cuenta
     */
    private Instant createdAt;
    
    /**
     * Fecha de última actualización
     */
    private Instant updatedAt;
    
    /**
     * URL del perfil de GitHub
     */
    private String htmlUrl;
    
    /**
     * Tipo de usuario (User, Organization)
     */
    private String type;
    
    /**
     * Si el usuario es verificado
     */
    private Boolean verified;
    
    /**
     * Si el usuario es un bot
     */
    private Boolean bot;
    
    /**
     * Si el usuario es un empleado de GitHub
     */
    private Boolean siteAdmin;
    
    /**
     * Obtiene el nombre completo del usuario o el login si no hay nombre
     */
    public String getDisplayName() {
        return name != null && !name.trim().isEmpty() ? name : login;
    }
    
    /**
     * Verifica si el usuario tiene información completa
     */
    public boolean hasCompleteProfile() {
        return name != null && email != null && bio != null && location != null;
    }
    
    /**
     * Obtiene el total de repositorios (públicos + privados)
     */
    public Integer getTotalRepos() {
        int publicCount = publicRepos != null ? publicRepos : 0;
        int privateCount = totalPrivateRepos != null ? totalPrivateRepos : 0;
        return publicCount + privateCount;
    }
} 