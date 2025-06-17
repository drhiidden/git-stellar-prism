package com.drhdn.ghvis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Representa un Pull Request en un repositorio de GitHub.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequest {
    
    /**
     * ID del Pull Request
     */
    private Long id;
    
    /**
     * Número del Pull Request en el repositorio
     */
    private int number;
    
    /**
     * Título del Pull Request
     */
    private String title;
    
    /**
     * Descripción del Pull Request
     */
    private String description;
    
    /**
     * Estado actual del Pull Request (open, closed, merged)
     */
    private String state;
    
    /**
     * Autor del Pull Request (nombre)
     */
    private String author;
    
    /**
     * URL del avatar del autor
     */
    private String authorAvatar;
    
    /**
     * Fecha y hora de creación
     */
    private Instant timestamp;
    
    /**
     * Fecha y hora de última actualización
     */
    private Instant updatedAt;
    
    /**
     * Fecha y hora de cierre (si está cerrado)
     */
    private Instant closedAt;
    
    /**
     * Fecha y hora de merge (si fue mergeado)
     */
    private Instant mergedAt;
    
    /**
     * Rama base (destino)
     */
    private String baseBranch;
    
    /**
     * Rama head (origen)
     */
    private String headBranch;
    
    /**
     * Lista de commits incluidos en el PR
     */
    private List<String> commitHashes;
    
    /**
     * Estadísticas del PR (archivos modificados, líneas añadidas/eliminadas)
     */
    private PrStats stats;
    
    /**
     * Referencias a Issues relacionados
     */
    private List<Commit.Reference> references;
    
    /**
     * Tipo de entidad (siempre "pr" para esta clase)
     */
    private final String type = "pr";
    
    /**
     * Estadísticas de un Pull Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrStats {
        /**
         * Número de archivos modificados
         */
        private int filesChanged;
        
        /**
         * Número de líneas añadidas
         */
        private int additions;
        
        /**
         * Número de líneas eliminadas
         */
        private int deletions;
        
        /**
         * Número de commits incluidos
         */
        private int commitCount;
        
        /**
         * Número de comentarios
         */
        private int commentCount;
        
        /**
         * Número de revisiones
         */
        private int reviewCount;
    }
} 