package com.drhdn.ghvis.domain.port;

import com.drhdn.ghvis.domain.entity.Technology;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

/**
 * Puerto para operaciones de detección de tecnologías.
 * Define el contrato para identificar y analizar tecnologías en repositorios.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface TechnologyRepository {
    
    /**
     * Detecta tecnologías utilizadas en un repositorio.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Flux con las tecnologías detectadas
     */
    Flux<Technology> detectTechnologies(String owner, String repo, Principal principal);
    
    /**
     * Obtiene las tecnologías principales de un repositorio.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @param limit El número máximo de tecnologías a retornar
     * @return Flux con las tecnologías principales
     */
    Flux<Technology> getTopTechnologies(String owner, String repo, Principal principal, int limit);
    
    /**
     * Detecta frameworks específicos en un repositorio.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Flux con los frameworks detectados
     */
    Flux<Technology> detectFrameworks(String owner, String repo, Principal principal);
    
    /**
     * Detecta herramientas de build y desarrollo.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Flux con las herramientas detectadas
     */
    Flux<Technology> detectBuildTools(String owner, String repo, Principal principal);
    
    /**
     * Obtiene estadísticas de tecnologías para un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @return Flux con estadísticas de tecnologías por repositorio
     */
    Flux<TechnologyStats> getTechnologyStatsByUser(String username, Principal principal);
    
    /**
     * Obtiene las tecnologías más usadas por un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @param limit El número máximo de tecnologías a retornar
     * @return Flux con las tecnologías más usadas
     */
    Flux<Technology> getTopTechnologiesByUser(String username, Principal principal, int limit);
    
    /**
     * Verifica si un repositorio tiene tecnologías detectables.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Mono con true si hay tecnologías detectables
     */
    Mono<Boolean> hasTechnologies(String owner, String repo, Principal principal);
    
    /**
     * Analiza la compatibilidad entre tecnologías detectadas.
     * 
     * @param owner El propietario del repositorio
     * @param repo El nombre del repositorio
     * @param principal El principal del usuario autenticado
     * @return Mono con análisis de compatibilidad
     */
    Mono<TechnologyCompatibility> analyzeTechnologyCompatibility(String owner, String repo, Principal principal);
    
    /**
     * Estadísticas de tecnologías para un repositorio.
     */
    interface TechnologyStats {
        /**
         * Nombre del repositorio
         */
        String getRepositoryName();
        
        /**
         * Propietario del repositorio
         */
        String getRepositoryOwner();
        
        /**
         * Tecnología principal
         */
        String getPrimaryTechnology();
        
        /**
         * Porcentaje de uso de la tecnología principal
         */
        Double getPrimaryTechnologyPercentage();
        
        /**
         * Número total de tecnologías detectadas
         */
        Integer getTotalTechnologies();
        
        /**
         * Número de frameworks detectados
         */
        Integer getFrameworkCount();
        
        /**
         * Número de herramientas de build detectadas
         */
        Integer getBuildToolCount();
        
        /**
         * Fecha de análisis
         */
        java.time.Instant getAnalyzedAt();
        
        /**
         * Nivel de complejidad tecnológica
         */
        String getComplexityLevel();
    }
    
    /**
     * Análisis de compatibilidad de tecnologías.
     */
    interface TechnologyCompatibility {
        /**
         * Score de compatibilidad (0-100)
         */
        Integer getCompatibilityScore();
        
        /**
         * Conflictos detectados
         */
        Map<String, String> getConflicts();
        
        /**
         * Tecnologías complementarias sugeridas
         */
        Map<String, String> getSuggestions();
        
        /**
         * Nivel de madurez del stack tecnológico
         */
        String getMaturityLevel();
    }
} 