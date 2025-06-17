package com.drhdn.ghvis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Representa un commit en un repositorio de GitHub.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Commit {
    
    /**
     * Hash SHA del commit
     */
    private String hash;
    
    /**
     * Mensaje del commit
     */
    private String message;
    
    /**
     * Autor del commit (nombre)
     */
    private String author;
    
    /**
     * Email del autor
     */
    private String authorEmail;
    
    /**
     * URL del avatar del autor
     */
    private String authorAvatar;
    
    /**
     * Fecha y hora del commit
     */
    private Instant timestamp;
    
    /**
     * Rama en la que se realizó el commit
     */
    private String branch;
    
    /**
     * Hashes de los commits padres
     */
    private List<String> parents;
    
    /**
     * Estadísticas del commit (archivos modificados, líneas añadidas/eliminadas)
     */
    private CommitStats stats;
    
    /**
     * Referencias a Pull Requests o Issues relacionados
     */
    private List<Reference> references;
    
    /**
     * Tipo de entidad (siempre "commit" para esta clase)
     */
    private final String type = "commit";
    
    /**
     * Estadísticas de un commit
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitStats {
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
    }
    
    /**
     * Referencia a otra entidad (PR, Issue)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reference {
        /**
         * Tipo de referencia (PR, Issue)
         */
        private String type;
        
        /**
         * ID de la entidad referenciada
         */
        private String id;
        
        /**
         * Título de la entidad referenciada
         */
        private String title;
    }
} 