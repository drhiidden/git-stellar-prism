package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryAnalysisQuery;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.LanguageRepository;
import com.drhdn.ghvis.domain.port.TechnologyRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import com.drhdn.ghvis.domain.event.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.stream.Collectors;
import com.drhdn.ghvis.domain.entity.TechnicalSummary;

/**
 * Handler para procesar queries de análisis de repositorio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("all") // Suprimir todas las advertencias temporalmente
public class GetRepositoryAnalysisQueryHandler {
    
    private final LanguageRepository languageRepository;
    private final CacheService cacheService;
    private final EventStore eventStore;
    private final TechnologyRepository technologyRepository;
    private final GithubApiAdapter githubApiAdapter;
    
    /**
     * Maneja la query para obtener análisis de repositorio.
     * 
     * @param query La query a procesar
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handle(GetRepositoryAnalysisQuery query) {
        log.info("🔍 Ejecutando query de análisis para repositorio: {} (QueryId: {})",
            query.getRepositoryFullName(), query.getQueryId());
        
        String cacheKey = query.getCacheKey();
        
        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando análisis fresco para: {}/{}", 
                query.getOwner(), query.getRepo());
            
            // Inicializar mapa de resultados
            Map<String, Object> result = new HashMap<>();
            
            // Crear Mono para cada componente solicitado
            Mono<Map<String, Object>> languagesMono = query.isIncludeLanguages() 
                ? getLanguageDistribution(query.getOwner(), query.getRepo(), query.getPrincipal())
                : Mono.just(Collections.emptyMap());
            
            Mono<List<Object>> technologiesMono = query.isIncludeTechnologies()
                ? getTechnologies(query.getOwner(), query.getRepo(), query.getPrincipal())
                : Mono.just(Collections.emptyList());
            
            Mono<Map<String, Object>> projectStructureMono = query.isIncludeProjectStructure()
                ? getProjectStructure(query.getOwner(), query.getRepo(), query.getPrincipal())
                : Mono.just(Collections.emptyMap());
            
            Mono<Map<String, Object>> technicalSummaryMono = query.isIncludeTechnicalSummary()
                ? getTechnicalSummary(query.getOwner(), query.getRepo(), query.getPrincipal())
                : Mono.just(Collections.emptyMap());
            
            // Combinar todos los resultados
            return Mono.zip(
                    languagesMono,
                    technologiesMono,
                    projectStructureMono,
                    technicalSummaryMono
                )
                .map(tuple -> {
                    Map<String, Object> analysisResult = new HashMap<>();
                    analysisResult.put("languageDistribution", tuple.getT1());
                    analysisResult.put("technologies", tuple.getT2());
                    analysisResult.put("projectStructure", tuple.getT3());
                    analysisResult.put("technicalSummary", tuple.getT4());
                    return analysisResult;
                });
        })
        .doOnSuccess(result -> {
            log.info("✅ Query de análisis completada exitosamente para: {}/{} (QueryId: {})",
                query.getOwner(), query.getRepo(), query.getQueryId());
        })
        .doOnError(error -> {
            log.error("❌ Error en query de análisis para: {}/{} (QueryId: {}): {}",
                query.getOwner(), query.getRepo(), query.getQueryId(), error.getMessage());
        });
    }
    
    /**
     * Maneja query para análisis completo de repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handleFullQuery(String owner, String repo, Principal principal) {
        GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createFullAnalysis(owner, repo, principal);
        return handle(query);
    }
    
    /**
     * Maneja query para análisis personalizado de repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @param commits Incluir commits
     * @param issues Incluir issues
     * @param pullRequests Incluir pull requests
     * @param technicalSummary Incluir resumen técnico
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handleCustomQuery(
            String owner, String repo, Principal principal,
            boolean commits, boolean issues, boolean pullRequests, boolean technicalSummary) {
        GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createCustomAnalysis(
            owner, repo, principal, true, true, true, true);
        return handle(query);
    }
    
    /**
     * Obtiene la distribución de lenguajes de un repositorio.
     */
    private Mono<Map<String, Object>> getLanguageDistribution(String owner, String repo, Principal principal) {
        return languageRepository.getLanguagesMap(owner, repo, principal)
            .map(langMap -> (Map<String, Object>) (Map<?, ?>) langMap)
            .defaultIfEmpty(Collections.emptyMap());
    }
    
    /**
     * Obtiene las tecnologías utilizadas en un repositorio.
     */
    private Mono<List<Object>> getTechnologies(String owner, String repo, Principal principal) {
        return technologyRepository.detectTechnologies(owner, repo, principal)
            .collectList()
            .map(list -> (List<Object>) (List<?>) list)
            .defaultIfEmpty(Collections.emptyList());
    }
    
    /**
     * Obtiene la estructura del proyecto.
     */
    private Mono<Map<String, Object>> getProjectStructure(String owner, String repo, Principal principal) {
        return githubApiAdapter.getRepositoryTree(owner, repo, null, principal)
            .map(treeList -> {
                int totalFiles = 0;
                int totalFolders = 0;
                int maxDepth = 0;

                for (Map<String, Object> node : treeList) {
                    String type = (String) node.getOrDefault("type", "");
                    String path = (String) node.getOrDefault("path", "");
                    if (type.equals("blob")) totalFiles++; else if (type.equals("tree")) totalFolders++;
                    int depth = path.split("/").length;
                    if (depth > maxDepth) maxDepth = depth;
                }

                Map<String, Object> structure = new HashMap<>();
                structure.put("totalFiles", totalFiles);
                structure.put("totalFolders", totalFolders);
                structure.put("maxDepth", maxDepth);
                structure.put("generatedAt", java.time.Instant.now().toString());
                return structure;
            })
            .defaultIfEmpty(Collections.emptyMap());
    }
    
    /**
     * Obtiene el resumen técnico del repositorio.
     */
    private Mono<Map<String, Object>> getTechnicalSummary(String owner, String repo, Principal principal) {
        Mono<Map<String, Long>> langMono = languageRepository.getLanguagesMap(owner, repo, principal)
                .defaultIfEmpty(Collections.emptyMap());

        Mono<List<Object>> techMono = technologyRepository.detectTechnologies(owner, repo, principal)
                .map(t -> (Object) t)
                .collectList()
                .defaultIfEmpty(Collections.emptyList());

        // Simular obtención del README para análisis de IA
        Mono<String> readmeMono = githubApiAdapter.getReadme(owner, repo, principal)
            .map(readme -> new String(Base64.getDecoder().decode(readme.getContent())))
            .defaultIfEmpty("No README disponible.")
            .onErrorResume(e -> {
                log.warn("No se pudo obtener README para {}/{}: {}", owner, repo, e.getMessage());
                return Mono.just("No README disponible.");
            });

        return Mono.zip(langMono, techMono, readmeMono)
            .map(tuple -> {
                Map<String, Long> langs = tuple.getT1();
                List<Object> techs = tuple.getT2();
                String readmeContent = tuple.getT3();

                String mainLanguage = langs.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Desconocido");

                // Simulación de análisis de IA para rellenar campos del resumen
                String projectPurpose = extractPurposeFromReadme(readmeContent);
                String rolesAndResponsibilities = ""; // Placeholder, la IA podría extraer esto
                List<String> achievements = Collections.emptyList(); // Placeholder, la IA podría extraer esto
                String documentationQuality = ""; // Placeholder, la IA podría evaluar esto

                TechnicalSummary summary = new TechnicalSummary(
                        null, // repositoryId: no disponible en este contexto
                        repo,
                        owner,
                        projectPurpose,
                        techs.stream().limit(5)
                                .map(obj -> obj instanceof Map ? ((Map<?, ?>) obj).get("name").toString() : obj.toString())
                                .collect(Collectors.toList()),
                        langs.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()),
                        techs.stream()
                                .map(obj -> obj instanceof Map ? ((Map<?, ?>) obj).get("name").toString() : obj.toString())
                                .collect(Collectors.toList()),
                        null, // totalFiles: no disponible en este contexto
                        null, // totalSize: no disponible en este contexto
                        langs.size(), // languageCount
                        mainLanguage,
                        null, // complexityScore: no disponible en este contexto
                        rolesAndResponsibilities,
                        achievements,
                        null, // codeSnippets: no disponible en este contexto
                        documentationQuality,
                        null, // preferredExportFormat: no disponible en este contexto
                        java.time.Instant.now() // generatedAt
                );

                return summary.toMap(); // Convertir TechnicalSummary a Map antes de devolver
            });
    }
    
    /**
     * Limpia el cache de análisis de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     */
    public void clearCache(String owner, String repo) {
        String cachePattern = String.format("repo:%s:%s:analysis:*", owner, repo);
        cacheService.clear(cachePattern).subscribe();
        log.info("🧹 Cache limpiado para análisis de repositorio: {}/{}", owner, repo);
    }

    private String extractPurposeFromReadme(String readmeContent) {
        // Simulación de extracción de propósito del README
        if (readmeContent.toLowerCase().contains("propósito:")) {
            int index = readmeContent.toLowerCase().indexOf("propósito:");
            String purposeSection = readmeContent.substring(index);
            int endIndex = purposeSection.indexOf('\n');
            if (endIndex != -1) {
                return purposeSection.substring(0, endIndex).replace("propósito:", "").trim();
            }
        } else if (readmeContent.toLowerCase().contains("objetivo:")) {
             int index = readmeContent.toLowerCase().indexOf("objetivo:");
            String purposeSection = readmeContent.substring(index);
            int endIndex = purposeSection.indexOf('\n');
            if (endIndex != -1) {
                return purposeSection.substring(0, endIndex).replace("objetivo:", "").trim();
            }
        }
        return "Propósito no disponible";
    }
} 