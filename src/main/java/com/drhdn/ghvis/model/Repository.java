package com.drhdn.ghvis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Representa un repositorio de GitHub.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Repository {
    
    /**
     * ID del repositorio
     */
    private Long id;
    
    /**
     * Nombre del repositorio
     */
    private String name;
    
    /**
     * Propietario del repositorio
     */
    private String owner;
    
    /**
     * Descripción del repositorio
     */
    private String description;
    
    /**
     * URL del repositorio en GitHub
     */
    private String url;
    
    /**
     * Rama por defecto
     */
    private String defaultBranch;
    
    /**
     * Fecha de creación
     */
    private Instant createdAt;
    
    /**
     * Fecha de última actualización
     */
    private Instant updatedAt;
    
    /**
     * Fecha del último push
     */
    private Instant pushedAt;
    
    /**
     * Número de estrellas
     */
    private int stargazersCount;
    
    /**
     * Número de forks
     */
    private int forksCount;
    
    /**
     * Número de watchers
     */
    private int watchersCount;
    
    /**
     * Número de issues abiertos
     */
    private int openIssuesCount;
    
    /**
     * Tamaño del repositorio en KB
     */
    private int size;
    
    /**
     * Indica si el repositorio es un fork
     */
    private boolean fork;
    
    /**
     * Indica si el repositorio es privado
     */
    private boolean isPrivate;
    
    /**
     * Indica si el repositorio está archivado
     */
    private boolean archived;
    
    /**
     * Distribución de lenguajes (nombre -> bytes)
     */
    private Map<String, Long> languageDistribution;
    
    /**
     * Configuración de visualización
     */
    private VisualizationConfig visualizationConfig;
    
    /**
     * Configuración para la visualización del repositorio
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualizationConfig {
        /**
         * Colores personalizados para autores
         */
        private Map<String, String> authorColors;
        
        /**
         * Opacidad de las conexiones (0.0 - 1.0)
         */
        private double connectionOpacity;
        
        /**
         * Tamaño de los nodos
         */
        private int nodeSize;
        
        /**
         * Color de fondo
         */
        private String backgroundColor;
        
        /**
         * Velocidad de órbita
         */
        private double orbitSpeed;
    }
} 