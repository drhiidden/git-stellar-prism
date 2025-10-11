package com.drhdn.ghvis.application.query;

import com.drhdn.ghvis.application.service.RepositoryAnalyzer;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Manejador de queries relacionadas con metadata técnica.
 * 
 * Responsabilidades:
 * - Leer metadata procesada
 * - Aplicar análisis técnico
 * - Cachear resultados agresivamente
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataQueryHandler {
    
    private final RepositoryRepository repositoryRepository;
    private final RepositoryAnalyzer repositoryAnalyzer;
    
    /**
     * Maneja GetTechnicalMetadata query.
     * 
     * Retorna metadata completa:
     * - Lenguajes
     * - Frameworks
     * - CI/CD tools
     * - Proyectos Open Source
     * 
     * Cache TTL: 24 horas (metadata cambia poco)
     */
    @Cacheable(value = "metadata", key = "#query.getCacheKey()")
    public Mono<RepositoryAnalyzer.TechnicalMetadata> handle(
        MetadataQuery.GetTechnicalMetadata query) {
        
        log.debug("📖 Ejecutando query: GetTechnicalMetadata para: {}", query.username());
        
        return repositoryRepository.findByUser(query.principal())
            .collectList()
            .map(repositoryAnalyzer::generateTechnicalMetadata)
            .doOnSuccess(metadata -> {
                log.debug("✅ Metadata generada: {} lenguajes, {} frameworks, {} CI/CD tools",
                    metadata.languages().size(),
                    metadata.frameworks().size(),
                    metadata.cicdTools().size());
            })
            .doOnError(error -> {
                log.error("❌ Error generando metadata: {}", error.getMessage());
            });
    }
    
    /**
     * Maneja GetLanguageDistribution query.
     * 
     * Optimización: Solo extrae lenguajes, más rápido que metadata completa.
     */
    @Cacheable(value = "languages", key = "#query.getCacheKey()")
    public Mono<Map<String, RepositoryAnalyzer.LanguageStats>> handle(
        MetadataQuery.GetLanguageDistribution query) {
        
        log.debug("📖 Ejecutando query: GetLanguageDistribution para: {}", query.username());
        
        return repositoryRepository.findByUser(query.principal())
            .collectList()
            .map(repositoryAnalyzer::analyzeLanguages)
            .doOnSuccess(languages -> {
                log.debug("✅ Distribución de lenguajes calculada: {} lenguajes",
                    languages.size());
            });
    }
    
    /**
     * Maneja GetFrameworks query.
     * 
     * Optimización: Solo extrae frameworks, más rápido que metadata completa.
     */
    @Cacheable(value = "frameworks", key = "#query.getCacheKey()")
    public Mono<Map<String, RepositoryAnalyzer.FrameworkStats>> handle(
        MetadataQuery.GetFrameworks query) {
        
        log.debug("📖 Ejecutando query: GetFrameworks para: {}", query.username());
        
        return repositoryRepository.findByUser(query.principal())
            .collectList()
            .map(repositoryAnalyzer::detectFrameworks)
            .doOnSuccess(frameworks -> {
                log.debug("✅ Frameworks detectados: {} frameworks", frameworks.size());
            });
    }
}

