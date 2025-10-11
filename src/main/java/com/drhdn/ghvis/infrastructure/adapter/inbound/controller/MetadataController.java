package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.query.MetadataQuery;
import com.drhdn.ghvis.application.query.MetadataQueryHandler;
import com.drhdn.ghvis.application.service.RepositoryAnalyzer;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador para exponer metadata técnica procesada por el backend.
 * 
 * Proporciona estadísticas precisas de tecnologías detectadas en los repositorios
 * del usuario, garantizando consistencia entre el dashboard y el CV generado.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
@Slf4j
public class MetadataController {
    
    private final MetadataQueryHandler metadataQueryHandler;
    
    /**
     * Obtiene metadata técnica consolidada de los repositorios del usuario.
     * 
     * Incluye:
     * - Lenguajes de programación (filtrados y validados)
     * - Frameworks y librerías
     * - Herramientas CI/CD
     * 
     * IMPORTANTE: Esta metadata es la MISMA que se usa para generar el CV,
     * garantizando consistencia entre dashboard y CV.
     * 
     * @param principal Usuario autenticado
     * @return Metadata técnica procesada
     */
    @GetMapping("/technologies")
    public Mono<ResponseEntity<TechnologyMetadataResponse>> getTechnologies(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401).build());
        }
        
        String username = principal.getName();
        log.info("📊 [CQRS Query] GetTechnicalMetadata para: {}", username);
        
        // ✅ CQRS: Usar QueryHandler en lugar de acceso directo
        var query = new MetadataQuery.GetTechnicalMetadata(username, principal);
        
        return metadataQueryHandler.handle(query)
            .map(metadata -> {
                // Convertir a formato consumible por frontend
                var response = TechnologyMetadataResponse.builder()
                    .languages(convertLanguages(metadata.languages()))
                    .frameworks(convertFrameworks(metadata.frameworks()))
                    .cicdTools(metadata.cicdTools())
                    .openSourceProjects(metadata.openSourceProjects().stream()
                        .limit(10)
                        .map(p -> OpenSourceProjectDTO.builder()
                            .name(p.name())
                            .fullName(p.fullName())
                            .stars(p.stars())
                            .forks(p.forks())
                            .url(p.url())
                            .build())
                        .collect(Collectors.toList()))
                    .build();
                
                log.info("✅ Metadata generada: {} lenguajes, {} frameworks, {} CI/CD tools",
                    response.languages.size(), 
                    response.frameworks.size(), 
                    response.cicdTools.size());
                
                return ResponseEntity.ok(response);
            })
            .doOnError(error -> log.error("❌ Error generando metadata: {}", error.getMessage()));
    }
    
    /**
     * Convierte estadísticas de lenguajes a DTOs
     */
    private List<TechnologyStat> convertLanguages(java.util.Map<String, RepositoryAnalyzer.LanguageStats> languages) {
        return languages.entrySet().stream()
            .map(entry -> TechnologyStat.builder()
                .name(entry.getKey())
                .count(entry.getValue().getProjectCount())
                .category("Programming Languages")
                .repositories(entry.getValue().getRepositories())
                .build())
            .sorted(Comparator.comparing(TechnologyStat::getCount).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Convierte estadísticas de frameworks a DTOs
     */
    private List<TechnologyStat> convertFrameworks(java.util.Map<String, RepositoryAnalyzer.FrameworkStats> frameworks) {
        return frameworks.entrySet().stream()
            .map(entry -> TechnologyStat.builder()
                .name(entry.getKey())
                .count(entry.getValue().getProjectCount())
                .category("Frameworks & Libraries")
                .repositories(entry.getValue().getRepositories())
                .build())
            .sorted(Comparator.comparing(TechnologyStat::getCount).reversed())
            .collect(Collectors.toList());
    }
    
    // ========== DTOs ==========
    
    @Data
    @Builder
    public static class TechnologyMetadataResponse {
        private List<TechnologyStat> languages;
        private List<TechnologyStat> frameworks;
        private Set<String> cicdTools;
        private List<OpenSourceProjectDTO> openSourceProjects;
    }
    
    @Data
    @Builder
    public static class TechnologyStat {
        private String name;
        private int count;
        private String category;
        private Set<String> repositories;
    }
    
    @Data
    @Builder
    public static class OpenSourceProjectDTO {
        private String name;
        private String fullName;
        private Integer stars;
        private Integer forks;
        private String url;
    }
}

