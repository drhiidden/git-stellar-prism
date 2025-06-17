package com.drhdn.ghvis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Representa un Issue en un repositorio de GitHub.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    
    /**
     * ID del Issue
     */
    private Long id;
    
    /**
     * Número del Issue en el repositorio
     */
    private int number;
    
    /**
     * Título del Issue
     */
    private String title;
    
    /**
     * Descripción del Issue
     */
    private String description;
    
    /**
     * Estado actual del Issue (open, closed)
     */
    private String state;
    
    /**
     * Autor del Issue (nombre)
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
     * Etiquetas asociadas al Issue
     */
    private List<Label> labels;
    
    /**
     * Asignados al Issue
     */
    private List<String> assignees;
    
    /**
     * Número de comentarios
     */
    private int commentCount;
    
    /**
     * Referencias a otros Issues o PRs
     */
    private List<Commit.Reference> references;
    
    /**
     * Tipo de entidad (siempre "issue" para esta clase)
     */
    private final String type = "issue";
    
    /**
     * Etiqueta de un Issue
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Label {
        /**
         * Nombre de la etiqueta
         */
        private String name;
        
        /**
         * Color de la etiqueta (en formato hex)
         */
        private String color;
        
        /**
         * Descripción de la etiqueta
         */
        private String description;
    }
} 