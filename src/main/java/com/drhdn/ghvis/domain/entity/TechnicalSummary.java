package com.drhdn.ghvis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representa un resumen técnico de un repositorio para portfolios o CVs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalSummary {
    
    /**
     * ID del repositorio asociado
     */
    private Long repositoryId;
    
    /**
     * Nombre del repositorio
     */
    private String repositoryName;
    
    /**
     * Propietario del repositorio
     */
    private String repositoryOwner;
    
    /**
     * Propósito del proyecto
     */
    private String projectPurpose;
    
    /**
     * Tecnologías principales utilizadas
     */
    private List<String> mainTechnologies;
    
    /**
     * Lenguajes de programación detectados
     */
    private List<String> languages;
    
    /**
     * Tecnologías detectadas en el proyecto
     */
    private List<String> technologies;
    
    /**
     * Número total de archivos
     */
    private Integer totalFiles;
    
    /**
     * Tamaño total del repositorio en bytes
     */
    private Long totalSize;
    
    /**
     * Lenguaje principal del proyecto
     */
    private String primaryLanguage;
    
    /**
     * Puntuación de complejidad del proyecto (0-10)
     */
    private Double complexityScore;
    
    /**
     * Roles y responsabilidades del desarrollador
     */
    private String rolesAndResponsibilities;
    
    /**
     * Logros destacables en el proyecto
     */
    private List<String> achievements;
    
    /**
     * Fragmentos de código relevantes
     */
    private List<CodeSnippet> codeSnippets;
    
    /**
     * Evaluación de la calidad de la documentación
     */
    private String documentationQuality;
    
    /**
     * Formato de exportación preferido
     */
    private ExportFormat preferredExportFormat;
    
    /**
     * Representa un fragmento de código relevante
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeSnippet {
        /**
         * Título o descripción del fragmento
         */
        private String title;
        
        /**
         * Lenguaje del código
         */
        private String language;
        
        /**
         * Código fuente
         */
        private String code;
        
        /**
         * Ruta al archivo (opcional)
         */
        private String filePath;
    }
    
    /**
     * Formatos de exportación disponibles
     */
    public enum ExportFormat {
        HTML,
        MARKDOWN,
        PDF
    }
} 