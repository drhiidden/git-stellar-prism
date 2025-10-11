package com.drhdn.ghvis.application.service;

import com.drhdn.ghvis.domain.entity.Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para analizar repositorios y extraer metadata técnica.
 * 
 * Detecta:
 * - Lenguajes de programación
 * - Frameworks y librerías
 * - Herramientas CI/CD
 * - Proyectos Open Source
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class RepositoryAnalyzer {
    
    /**
     * Lenguajes que NO son lenguajes de programación reales.
     * Se excluyen formatos de documentación, configuración, etc.
     */
    private static final Set<String> EXCLUDED_LANGUAGES = Set.of(
        // Formatos de documentación
        "Jupyter Notebook",
        "Markdown",
        "reStructuredText",
        "AsciiDoc",
        "POD",
        
        // Formatos de configuración
        "JSON",
        "YAML",
        "TOML",
        "INI",
        "XML",
        
        // Formatos de infraestructura
        "Dockerfile",
        "Makefile",
        
        // Frameworks que GitHub reporta como "lenguajes"
        "Astro",  // Es un framework, no un lenguaje
        "Svelte", // Es un framework, no un lenguaje (aunque tiene su sintaxis)
        
        // Otros
        "Text",
        "HTML",  // Markup, no programming
        "CSS",   // Styling, no programming
        "SCSS",
        "Less"
    );
    
    /**
     * Analiza lenguajes de programación usados
     */
    public Map<String, LanguageStats> analyzeLanguages(List<Repository> repos) {
        Map<String, LanguageStats> languageStats = new HashMap<>();
        
        repos.forEach(repo -> {
            Map<String, Long> distribution = repo.getLanguageDistribution();
            if (distribution != null && !distribution.isEmpty()) {
                // El lenguaje principal es el que tiene más bytes
                String primaryLanguage = distribution.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                
                // Solo agregar si es un lenguaje de programación válido
                if (primaryLanguage != null && !EXCLUDED_LANGUAGES.contains(primaryLanguage)) {
                    languageStats.computeIfAbsent(primaryLanguage, k -> new LanguageStats())
                        .addRepository(repo.getName());
                }
            }
        });
        
        return languageStats;
    }
    
    /**
     * Detecta frameworks desde topics
     */
    public Map<String, FrameworkStats> detectFrameworks(List<Repository> repos) {
        Map<String, FrameworkStats> frameworkStats = new HashMap<>();
        
        // Mapeo de topics conocidos a frameworks
        Map<String, String> frameworkMappings = Map.ofEntries(
            // Frontend
            Map.entry("react", "React"),
            Map.entry("reactjs", "React"),
            Map.entry("vue", "Vue.js"),
            Map.entry("vuejs", "Vue.js"),
            Map.entry("angular", "Angular"),
            Map.entry("svelte", "Svelte"),
            Map.entry("nextjs", "Next.js"),
            
            // Backend
            Map.entry("spring-boot", "Spring Boot"),
            Map.entry("spring", "Spring Framework"),
            Map.entry("django", "Django"),
            Map.entry("flask", "Flask"),
            Map.entry("fastapi", "FastAPI"),
            Map.entry("express", "Express.js"),
            Map.entry("nestjs", "NestJS"),
            
            // Mobile
            Map.entry("react-native", "React Native"),
            Map.entry("flutter", "Flutter"),
            Map.entry("ionic", "Ionic"),
            
            // Databases
            Map.entry("mongodb", "MongoDB"),
            Map.entry("postgresql", "PostgreSQL"),
            Map.entry("mysql", "MySQL"),
            Map.entry("redis", "Redis"),
            
            // DevOps
            Map.entry("docker", "Docker"),
            Map.entry("kubernetes", "Kubernetes"),
            Map.entry("terraform", "Terraform"),
            
            // AI/ML
            Map.entry("tensorflow", "TensorFlow"),
            Map.entry("pytorch", "PyTorch"),
            Map.entry("machine-learning", "Machine Learning"),
            Map.entry("artificial-intelligence", "Artificial Intelligence")
        );
        
        repos.forEach(repo -> {
            List<String> topics = repo.getTopics();
            if (topics != null) {
                topics.forEach(topic -> {
                    String framework = frameworkMappings.get(topic.toLowerCase());
                    if (framework != null) {
                        frameworkStats.computeIfAbsent(framework, k -> new FrameworkStats())
                            .addRepository(repo.getName());
                    }
                });
            }
        });
        
        return frameworkStats;
    }
    
    /**
     * Detecta herramientas CI/CD desde topics y descripción
     */
    public Set<String> detectCICD(List<Repository> repos) {
        Set<String> cicdTools = new HashSet<>();
        
        // Keywords para detectar CI/CD
        Map<String, String> cicdKeywords = Map.ofEntries(
            Map.entry("docker", "Docker"),
            Map.entry("docker-compose", "Docker Compose"),
            Map.entry("dockerfile", "Docker"),
            Map.entry("kubernetes", "Kubernetes"),
            Map.entry("k8s", "Kubernetes"),
            Map.entry("jenkins", "Jenkins"),
            Map.entry("github-actions", "GitHub Actions"),
            Map.entry("gitlab-ci", "GitLab CI"),
            Map.entry("travis", "Travis CI"),
            Map.entry("circleci", "CircleCI"),
            Map.entry("terraform", "Terraform"),
            Map.entry("ansible", "Ansible"),
            Map.entry("ci-cd", "CI/CD"),
            Map.entry("continuous-integration", "CI/CD")
        );
        
        repos.forEach(repo -> {
            // Buscar en topics
            List<String> topics = repo.getTopics();
            if (topics != null) {
                topics.forEach(topic -> {
                    String tool = cicdKeywords.get(topic.toLowerCase());
                    if (tool != null) {
                        cicdTools.add(tool);
                    }
                });
            }
            
            // Buscar en descripción
            String description = repo.getDescription();
            if (description != null) {
                String lowerDesc = description.toLowerCase();
                cicdKeywords.forEach((keyword, tool) -> {
                    if (lowerDesc.contains(keyword)) {
                        cicdTools.add(tool);
                    }
                });
            }
        });
        
        return cicdTools;
    }
    
    /**
     * Identifica proyectos Open Source
     * 
     * Criterios:
     * - Tiene topics como "open-source", "opensource"
     * - Tiene más de X stars (indica proyecto público activo)
     * - No es fork
     */
    public List<OpenSourceProject> identifyOpenSourceProjects(List<Repository> repos) {
        return repos.stream()
            .filter(this::isOpenSourceProject)
            .map(OpenSourceProject::new)
            .sorted(Comparator.comparing(OpenSourceProject::stars).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Verifica si un repositorio es Open Source
     */
    private boolean isOpenSourceProject(Repository repo) {
        // No es fork
        if (repo.isFork()) {
            return false;
        }
        
        // Es público
        if (repo.isPrivate()) {
            return false;
        }
        
        // Tiene stars (indica actividad)
        if (repo.getStargazersCount() > 0) {
            return true;
        }
        
        // O tiene topics de open source
        List<String> topics = repo.getTopics();
        if (topics != null) {
            return topics.stream()
                .anyMatch(t -> t.toLowerCase().contains("open-source") || 
                              t.toLowerCase().contains("opensource"));
        }
        
        return false;
    }
    
    /**
     * Genera resumen técnico consolidado
     */
    public TechnicalMetadata generateTechnicalMetadata(List<Repository> repos) {
        log.info("📊 Analizando {} repositorios para generar metadata técnica", repos.size());
        
        // DIAGNÓSTICO: Ver qué datos tenemos
        if (!repos.isEmpty()) {
            Repository firstRepo = repos.get(0);
            log.info("🔍 DIAGNÓSTICO - Primer repositorio: {}", firstRepo.getName());
            log.info("  - LanguageDistribution: {}", firstRepo.getLanguageDistribution());
            log.info("  - Topics: {}", firstRepo.getTopics());
            log.info("  - Description: {}", firstRepo.getDescription());
            log.info("  - IsPrivate: {}", firstRepo.isPrivate());
            log.info("  - Stars: {}", firstRepo.getStargazersCount());
            log.info("  - IsFork: {}", firstRepo.isFork());
        }
        
        Map<String, LanguageStats> languages = analyzeLanguages(repos);
        Map<String, FrameworkStats> frameworks = detectFrameworks(repos);
        Set<String> cicdTools = detectCICD(repos);
        List<OpenSourceProject> openSourceProjects = identifyOpenSourceProjects(repos);
        
        log.info("✅ Metadata generada: {} lenguajes, {} frameworks, {} herramientas CI/CD, {} proyectos OS",
            languages.size(), frameworks.size(), cicdTools.size(), openSourceProjects.size());
        
        return new TechnicalMetadata(languages, frameworks, cicdTools, openSourceProjects);
    }
    
    // ========== CLASES INTERNAS ==========
    
    public static class LanguageStats {
        private int projectCount = 0;
        private final Set<String> repositories = new HashSet<>();
        
        public void addRepository(String repoName) {
            repositories.add(repoName);
            projectCount++;
        }
        
        public int getProjectCount() { return projectCount; }
        public Set<String> getRepositories() { return repositories; }
    }
    
    public static class FrameworkStats {
        private int projectCount = 0;
        private final Set<String> repositories = new HashSet<>();
        
        public void addRepository(String repoName) {
            repositories.add(repoName);
            projectCount++;
        }
        
        public int getProjectCount() { return projectCount; }
        public Set<String> getRepositories() { return repositories; }
    }
    
    public record OpenSourceProject(
        String name,
        String fullName, // owner/name
        String description,
        Integer stars,
        Integer forks,
        String url,
        List<String> topics
    ) {
        public OpenSourceProject(Repository repo) {
            this(
                repo.getName(),
                repo.getOwner() + "/" + repo.getName(),
                repo.getDescription(),
                repo.getStargazersCount(),
                repo.getForksCount(),
                repo.getUrl(),
                repo.getTopics()
            );
        }
    }
    
    public record TechnicalMetadata(
        Map<String, LanguageStats> languages,
        Map<String, FrameworkStats> frameworks,
        Set<String> cicdTools,
        List<OpenSourceProject> openSourceProjects
    ) {}
}

