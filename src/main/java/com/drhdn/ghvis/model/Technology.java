package com.drhdn.ghvis.model;

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