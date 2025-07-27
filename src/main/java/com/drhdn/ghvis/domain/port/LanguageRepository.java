package com.drhdn.ghvis.domain.port;

import com.drhdn.ghvis.domain.entity.Language;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

/**
 * Puerto para operaciones de lenguajes de programación.
 * Define el contrato para obtener información de lenguajes en repositorios.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface LanguageRepository {
    
    /**
     * Obtiene la distribución de lenguajes de un repositorio.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Flux con los lenguajes del repositorio
     */
    Flux<Language> getLanguagesByRepository(String owner, String repo, Principal principal);
    
    /**
     * Obtiene la distribución de lenguajes como mapa (nombre -> bytes).
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Mono con el mapa de lenguajes
     */
    Mono<Map<String, Long>> getLanguagesMap(String owner, String repo, Principal principal);
    
    /**
     * Obtiene el lenguaje principal de un repositorio.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Mono con el lenguaje principal
     */
    Mono<Language> getPrimaryLanguage(String owner, String repo, Principal principal);
    
    /**
     * Obtiene estadísticas de lenguajes para un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @return Flux con estadísticas de lenguajes por repositorio
     */
    Flux<LanguageStats> getLanguageStatsByUser(String username, Principal principal);
    
    /**
     * Obtiene los lenguajes más usados en todos los repositorios de un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @param limit El número máximo de lenguajes a retornar
     * @return Flux con los lenguajes más usados
     */
    Flux<Language> getTopLanguagesByUser(String username, Principal principal, int limit);
    
    /**
     * Verifica si un repositorio tiene lenguajes disponibles.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Mono con true si hay lenguajes disponibles
     */
    Mono<Boolean> hasLanguages(String owner, String repo, Principal principal);
    
    /**
     * Estadísticas de lenguajes para un repositorio.
     */
    interface LanguageStats {
        /**
         * Nombre del repositorio
         */
        String getRepositoryName();
        
        /**
         * Propietario del repositorio
         */
        String getRepositoryOwner();
        
        /**
         * Lenguaje principal
         */
        String getPrimaryLanguage();
        
        /**
         * Porcentaje del lenguaje principal
         */
        Double getPrimaryLanguagePercentage();
        
        /**
         * Número total de lenguajes
         */
        Integer getTotalLanguages();
        
        /**
         * Total de bytes de código
         */
        Long getTotalBytes();
        
        /**
         * Fecha de análisis
         */
        java.time.Instant getAnalyzedAt();
    }
} 