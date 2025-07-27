package com.drhdn.ghvis.domain.valueobject;

import lombok.Value;

/**
 * Value Object que representa el nombre de un repositorio.
 * 
 * Un nombre de repositorio sigue el formato "owner/repo" y es inmutable.
 */
@Value
public class RepositoryName {
    String owner;
    String repo;
    
    public RepositoryName(String owner, String repo) {
        if (owner == null || owner.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner no puede ser nulo o vacío");
        }
        if (repo == null || repo.trim().isEmpty()) {
            throw new IllegalArgumentException("Repo no puede ser nulo o vacío");
        }
        if (!owner.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Owner debe contener solo letras, números, guiones y guiones bajos");
        }
        if (!repo.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Repo debe contener solo letras, números, guiones y guiones bajos");
        }
        
        this.owner = owner.toLowerCase();
        this.repo = repo.toLowerCase();
    }
    
    /**
     * Crea un RepositoryName desde un string en formato "owner/repo".
     */
    public static RepositoryName from(String fullName) {
        if (fullName == null || !fullName.contains("/")) {
            throw new IllegalArgumentException("Nombre debe estar en formato 'owner/repo'");
        }
        
        String[] parts = fullName.split("/", 2);
        return new RepositoryName(parts[0], parts[1]);
    }
    
    /**
     * Obtiene el nombre completo en formato "owner/repo".
     */
    public String getFullName() {
        return owner + "/" + repo;
    }
    
    /**
     * Obtiene el owner del repositorio.
     */
    public String getOwner() {
        return owner;
    }
    
    /**
     * Obtiene el nombre del repositorio.
     */
    public String getRepo() {
        return repo;
    }
    
    @Override
    public String toString() {
        return getFullName();
    }
} 