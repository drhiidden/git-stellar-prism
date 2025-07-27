package com.drhdn.ghvis.domain.port;

import com.drhdn.ghvis.domain.entity.User;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Puerto para operaciones de usuario.
 * Define el contrato para obtener información de usuarios de GitHub.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface UserRepository {
    
    /**
     * Obtiene la información del usuario autenticado.
     * 
     * @param principal El principal del usuario autenticado
     * @return Mono con la información del usuario
     */
    Mono<User> getCurrentUser(Principal principal);
    
    /**
     * Obtiene la información de un usuario específico por su login.
     * 
     * @param login El login del usuario
     * @param principal El principal del usuario autenticado (para autenticación)
     * @return Mono con la información del usuario
     */
    Mono<User> getUserByLogin(String login, Principal principal);
    
    /**
     * Obtiene la información de un usuario por su ID.
     * 
     * @param userId El ID del usuario
     * @param principal El principal del usuario autenticado
     * @return Mono con la información del usuario
     */
    Mono<User> getUserById(Long userId, Principal principal);
    
    /**
     * Verifica si un usuario existe.
     * 
     * @param login El login del usuario
     * @param principal El principal del usuario autenticado
     * @return Mono con true si el usuario existe, false en caso contrario
     */
    Mono<Boolean> userExists(String login, Principal principal);
    
    /**
     * Obtiene estadísticas básicas del usuario.
     * 
     * @param login El login del usuario
     * @param principal El principal del usuario autenticado
     * @return Mono con las estadísticas del usuario
     */
    Mono<UserStats> getUserStats(String login, Principal principal);
    
    /**
     * Estadísticas de un usuario.
     */
    interface UserStats {
        /**
         * Número total de repositorios
         */
        Integer getTotalRepositories();
        
        /**
         * Número de repositorios públicos
         */
        Integer getPublicRepositories();
        
        /**
         * Número de repositorios privados
         */
        Integer getPrivateRepositories();
        
        /**
         * Número de seguidores
         */
        Integer getFollowers();
        
        /**
         * Número de usuarios que sigue
         */
        Integer getFollowing();
        
        /**
         * Número total de stars recibidos
         */
        Integer getTotalStars();
        
        /**
         * Número total de forks recibidos
         */
        Integer getTotalForks();
        
        /**
         * Fecha de creación de la cuenta
         */
        java.time.Instant getAccountCreatedAt();
        
        /**
         * Última actividad del usuario
         */
        java.time.Instant getLastActivityAt();
    }
} 