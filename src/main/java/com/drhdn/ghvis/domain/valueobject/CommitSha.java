package com.drhdn.ghvis.domain.valueobject;

import lombok.Value;

/**
 * Value Object que representa el SHA de un commit.
 * 
 * Un SHA de commit es un identificador único e inmutable que representa
 * un punto específico en el historial de Git.
 */
@Value
public class CommitSha {
    String value;
    
    public CommitSha(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("SHA no puede ser nulo o vacío");
        }
        if (!value.matches("^[a-fA-F0-9]{7,40}$")) {
            throw new IllegalArgumentException("SHA debe tener entre 7 y 40 caracteres hexadecimales");
        }
        this.value = value.toLowerCase();
    }
    
    /**
     * Obtiene el SHA corto (primeros 7 caracteres).
     */
    public String getShortSha() {
        return value.substring(0, Math.min(7, value.length()));
    }
    
    /**
     * Obtiene el SHA completo.
     */
    public String getFullSha() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
} 