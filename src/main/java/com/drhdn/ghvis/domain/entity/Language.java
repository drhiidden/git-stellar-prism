package com.drhdn.ghvis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entidad que representa un lenguaje de programación en un repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Language {
    
    /**
     * ID único del lenguaje
     */
    private Long id;
    
    /**
     * Nombre del lenguaje de programación
     */
    private String name;
    
    /**
     * Nombre del repositorio donde se usa
     */
    private String repositoryName;
    
    /**
     * Owner del repositorio
     */
    private String repositoryOwner;
    
    /**
     * Número de bytes de código en este lenguaje
     */
    private Long bytes;
    
    /**
     * Porcentaje del repositorio que representa este lenguaje
     */
    private Double percentage;
    
    /**
     * Número de líneas de código estimadas
     */
    private Long estimatedLines;
    
    /**
     * Color del lenguaje (hexadecimal)
     */
    private String color;
    
    /**
     * Tipo de lenguaje (programming, markup, data, etc.)
     */
    private String type;
    
    /**
     * Si es un lenguaje de programación
     */
    private Boolean isProgrammingLanguage;
    
    /**
     * Fecha de análisis
     */
    private Instant analyzedAt;
    
    /**
     * Fecha de creación del registro
     */
    private Instant createdAt;
    
    /**
     * Fecha de última actualización
     */
    private Instant updatedAt;
    
    /**
     * Obtiene el nombre completo del repositorio
     */
    public String getRepositoryFullName() {
        return repositoryOwner + "/" + repositoryName;
    }
    
    /**
     * Verifica si es un lenguaje de programación principal
     */
    public boolean isMainLanguage() {
        return percentage != null && percentage > 50.0;
    }
    
    /**
     * Verifica si es un lenguaje de programación
     */
    public boolean isProgramming() {
        return isProgrammingLanguage != null && isProgrammingLanguage;
    }
    
    /**
     * Obtiene el tamaño en formato legible
     */
    public String getFormattedSize() {
        if (bytes == null) return "0 B";
        
        long bytes = this.bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        
        while (bytes >= 1024 && unitIndex < units.length - 1) {
            bytes /= 1024;
            unitIndex++;
        }
        
        return bytes + " " + units[unitIndex];
    }
    
    /**
     * Obtiene el porcentaje formateado
     */
    public String getFormattedPercentage() {
        if (percentage == null) return "0.0%";
        return String.format("%.1f%%", percentage);
    }
} 