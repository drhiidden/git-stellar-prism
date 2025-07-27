package com.drhdn.ghvis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representa una tecnología detectada en un repositorio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Technology {
    
    /**
     * Nombre de la tecnología
     */
    private String name;
    
    /**
     * Tipo de tecnología (lenguaje, framework, librería)
     */
    private TechnologyType type;
    
    /**
     * Versión de la tecnología (si está disponible)
     */
    private String version;
    
    /**
     * Archivos asociados a esta tecnología
     */
    private List<String> files;
    
    /**
     * Nivel de uso en el proyecto (0.0 - 1.0)
     */
    private double usageLevel;
    
    /**
     * URL de documentación o sitio web oficial
     */
    private String documentationUrl;
    
    /**
     * Descripción breve de la tecnología
     */
    private String description;
    
    // --------- Nuevos campos para detección avanzada ---------
    /**
     * Categoría de la tecnología (Framework, Build Tool, etc.)
     */
    private String category;

    /**
     * Lenguaje principal asociado a la tecnología
     */
    private String language;

    /**
     * Nivel de confianza de la detección (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * Propietario del repositorio donde se detectó la tecnología
     */
    private String repositoryOwner;

    /**
     * Nombre del repositorio donde se detectó la tecnología
     */
    private String repositoryName;

    /**
     * Fecha de detección
     */
    private java.time.Instant detectedAt;
    
    /**
     * Tipos de tecnología
     */
    public enum TechnologyType {
        LANGUAGE,       // Lenguaje de programación
        FRAMEWORK,      // Framework
        LIBRARY,        // Biblioteca
        DATABASE,       // Base de datos
        BUILD_TOOL,     // Herramienta de construcción
        TESTING,        // Framework/herramienta de pruebas
        DEPLOYMENT,     // Herramienta de despliegue
        OTHER           // Otro tipo
    }
} 