package com.drhdn.ghvis.application.query;

import com.drhdn.ghvis.application.service.RepositoryAnalyzer;

import java.security.Principal;

/**
 * Queries relacionadas con metadata técnica.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public sealed interface MetadataQuery<R> extends Query<R>
    permits MetadataQuery.GetTechnicalMetadata,
            MetadataQuery.GetLanguageDistribution,
            MetadataQuery.GetFrameworks {
    
    /**
     * Query para obtener metadata técnica completa de un usuario.
     * 
     * Incluye:
     * - Lenguajes de programación
     * - Frameworks detectados
     * - Herramientas CI/CD
     * - Proyectos Open Source
     * 
     * Retorna: RepositoryAnalyzer.TechnicalMetadata
     * 
     * @param username Nombre de usuario
     * @param principal Principal de autenticación
     */
    record GetTechnicalMetadata(
        String username,
        Principal principal
    ) implements MetadataQuery<RepositoryAnalyzer.TechnicalMetadata> {
        
        @Override
        public String getCacheKey() {
            return "metadata:technical:" + username;
        }
    }
    
    /**
     * Query para obtener solo la distribución de lenguajes.
     * 
     * Retorna: Map<String, LanguageStats>
     * 
     * @param username Nombre de usuario
     * @param principal Principal de autenticación
     * @param includePercentages Si true, calcula porcentajes
     */
    record GetLanguageDistribution(
        String username,
        Principal principal,
        boolean includePercentages
    ) implements MetadataQuery<java.util.Map<String, RepositoryAnalyzer.LanguageStats>> {
        
        public GetLanguageDistribution(String username, Principal principal) {
            this(username, principal, false);
        }
        
        @Override
        public String getCacheKey() {
            return "metadata:languages:" + username;
        }
    }
    
    /**
     * Query para obtener solo los frameworks detectados.
     * 
     * Retorna: Map<String, FrameworkStats>
     * 
     * @param username Nombre de usuario
     * @param principal Principal de autenticación
     */
    record GetFrameworks(
        String username,
        Principal principal
    ) implements MetadataQuery<java.util.Map<String, RepositoryAnalyzer.FrameworkStats>> {
        
        @Override
        public String getCacheKey() {
            return "metadata:frameworks:" + username;
        }
    }
}

