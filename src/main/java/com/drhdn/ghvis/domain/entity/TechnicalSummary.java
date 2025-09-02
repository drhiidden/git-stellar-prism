package com.drhdn.ghvis.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashMap;

/**
 * Representa un resumen técnico de un repositorio para portfolios o CVs.
 */
@Data
@NoArgsConstructor
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
     * Número de lenguajes detectados.
     */
    private Integer languageCount;

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
     * Marca de tiempo de la generación del resumen.
     */
    private Instant generatedAt;

    public TechnicalSummary(Long repositoryId, String repositoryName, String repositoryOwner, String projectPurpose,
                            List<String> mainTechnologies, List<String> languages, List<String> technologies,
                            Integer totalFiles, Long totalSize, Integer languageCount, String primaryLanguage,
                            Double complexityScore, String rolesAndResponsibilities, List<String> achievements,
                            List<CodeSnippet> codeSnippets, String documentationQuality, ExportFormat preferredExportFormat,
                            Instant generatedAt) {
        this.repositoryId = repositoryId;
        this.repositoryName = repositoryName;
        this.repositoryOwner = repositoryOwner;
        this.projectPurpose = projectPurpose;
        this.mainTechnologies = mainTechnologies;
        this.languages = languages;
        this.technologies = technologies;
        this.totalFiles = totalFiles;
        this.totalSize = totalSize;
        this.languageCount = languageCount;
        this.primaryLanguage = primaryLanguage;
        this.complexityScore = complexityScore;
        this.rolesAndResponsibilities = rolesAndResponsibilities;
        this.achievements = achievements;
        this.codeSnippets = codeSnippets;
        this.documentationQuality = documentationQuality;
        this.preferredExportFormat = preferredExportFormat;
        this.generatedAt = generatedAt;
    }

    public void setLanguageCount(Integer languageCount) {
        this.languageCount = languageCount;
    }
    
    /**
     * Representa un fragmento de código relevante
     */
    @Data
    @NoArgsConstructor
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

        public CodeSnippet(String title, String language, String code, String filePath) {
            this.title = title;
            this.language = language;
            this.code = code;
            this.filePath = filePath;
        }
    }
    
    /**
     * Formatos de exportación disponibles
     */
    public enum ExportFormat {
        HTML,
        MARKDOWN,
        PDF
    }

    // Método estático para construir un TechnicalSummary desde un mapa
    public static TechnicalSummary fromMap(Map<String, Object> map) {
        return new TechnicalSummary(
                (Long) map.get("repositoryId"),
                (String) map.get("repositoryName"),
                (String) map.get("repositoryOwner"),
                (String) map.getOrDefault("projectPurpose", "Propósito no disponible"),
                map.containsKey("mainTechnologies") ? 
                                    ((List<?>) map.get("mainTechnologies")).stream()
                                    .map(obj -> obj instanceof Map ? ((Map<?, ?>) obj).get("name").toString() : obj.toString())
                                    .collect(Collectors.toList()) : 
                                    Collections.emptyList(),
                map.containsKey("languages") ? 
                            ((List<?>) map.get("languages")).stream()
                            .map(Object::toString)
                            .collect(Collectors.toList()) : 
                            Collections.emptyList(),
                map.containsKey("technologies") ? 
                                ((List<?>) map.get("technologies")).stream()
                                .map(obj -> obj instanceof Map ? ((Map<?, ?>) obj).get("name").toString() : obj.toString())
                                .collect(Collectors.toList()) : 
                                Collections.emptyList(),
                (Integer) map.get("totalFiles"),
                (Long) map.get("totalSize"),
                (Integer) map.get("languageCount"),
                (String) map.get("mainLanguage"), // Mapear mainLanguage a primaryLanguage
                (Double) map.get("complexityScore"),
                (String) map.getOrDefault("rolesAndResponsibilities", ""),
                map.containsKey("achievements") ? 
                                ((List<?>) map.get("achievements")).stream()
                                .map(Object::toString)
                                .collect(Collectors.toList()) : 
                                Collections.emptyList(),
                map.containsKey("codeSnippets") ? 
                                ((List<?>) map.get("codeSnippets")).stream()
                                .map(obj -> {
                                    if (obj instanceof Map) {
                                        Map<?, ?> snippetMap = (Map<?, ?>) obj;
                                        return new CodeSnippet(
                                                (String) snippetMap.get("title"),
                                                (String) snippetMap.get("language"),
                                                (String) snippetMap.get("code"),
                                                (String) snippetMap.get("filePath")
                                        );
                                    }
                                    return null; // O manejar de otra manera
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()) :
                                Collections.emptyList(),
                (String) map.getOrDefault("documentationQuality", ""),
                map.containsKey("preferredExportFormat") ? 
                                       ExportFormat.valueOf((String) map.get("preferredExportFormat")) : 
                                       null,
                (Instant) map.getOrDefault("generatedAt", Instant.now())
        );
    }

    // Método para convertir un TechnicalSummary a un mapa
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("repositoryId", repositoryId);
        map.put("repositoryName", repositoryName);
        map.put("repositoryOwner", repositoryOwner);
        map.put("projectPurpose", projectPurpose);
        map.put("mainTechnologies", mainTechnologies);
        map.put("languages", languages);
        map.put("technologies", technologies);
        map.put("totalFiles", totalFiles);
        map.put("totalSize", totalSize);
        map.put("mainLanguage", primaryLanguage); // Mapear primaryLanguage a mainLanguage
        map.put("languageCount", languageCount);
        map.put("complexityScore", complexityScore);
        map.put("rolesAndResponsibilities", rolesAndResponsibilities);
        map.put("achievements", achievements);
        map.put("codeSnippets", codeSnippets != null ? codeSnippets.stream()
                                    .map(snippet -> {
                                        Map<String, Object> snippetMap = new HashMap<>();
                                        snippetMap.put("title", snippet.getTitle());
                                        snippetMap.put("language", snippet.getLanguage());
                                        snippetMap.put("code", snippet.getCode());
                                        snippetMap.put("filePath", snippet.getFilePath());
                                        return snippetMap;
                                    }).collect(Collectors.toList()) : null);
        map.put("documentationQuality", documentationQuality);
        map.put("preferredExportFormat", preferredExportFormat != null ? preferredExportFormat.name() : null);
        map.put("generatedAt", generatedAt);
        return map;
    }
} 