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
 * - Patrones de Arquitectura (NUEVO)
 * - Arquetipos de Desarrollador (NUEVO)
 * 
 * @author GitStellarPrism Team
 * @version 1.1.0
 */
@Service
@Slf4j
public class RepositoryAnalyzer {
    
    /**
     * Lenguajes que NO son lenguajes de programación reales.
     */
    private static final Set<String> EXCLUDED_LANGUAGES = Set.of(
        "Jupyter Notebook", "Markdown", "reStructuredText", "AsciiDoc", "POD",
        "JSON", "YAML", "TOML", "INI", "XML",
        "Dockerfile", "Makefile",
        "Astro", "Svelte",
        "Text", "HTML", "CSS", "SCSS", "Less", "Shell"
    );
    
    /**
     * Mapeo masivo de tecnologías clasificadas
     */
    private static final Map<String, String> FRAMEWORK_MAPPINGS = Map.ofEntries(
        // Frontend Core
        Map.entry("react", "React"), Map.entry("reactjs", "React"),
        Map.entry("vue", "Vue.js"), Map.entry("vuejs", "Vue.js"),
        Map.entry("angular", "Angular"), Map.entry("svelte", "Svelte"),
        Map.entry("nextjs", "Next.js"), Map.entry("nuxtjs", "Nuxt.js"),
        Map.entry("gatsby", "Gatsby"), Map.entry("vite", "Vite"),
        Map.entry("webpack", "Webpack"), Map.entry("tailwind", "Tailwind CSS"),
        
        // Backend Frameworks
        Map.entry("spring-boot", "Spring Boot"), Map.entry("spring", "Spring Framework"),
        Map.entry("django", "Django"), Map.entry("flask", "Flask"), Map.entry("fastapi", "FastAPI"),
        Map.entry("express", "Express.js"), Map.entry("nestjs", "NestJS"),
        Map.entry("laravel", "Laravel"), Map.entry("rails", "Ruby on Rails"),
        Map.entry("aspnetcore", "ASP.NET Core"), Map.entry("dotnet", ".NET"),
        Map.entry("quarkus", "Quarkus"), Map.entry("micronaut", "Micronaut"),
        
        // Mobile
        Map.entry("react-native", "React Native"), Map.entry("flutter", "Flutter"),
        Map.entry("ionic", "Ionic"), Map.entry("swiftui", "SwiftUI"),
        Map.entry("kotlin-multiplatform", "KMP"),
        
        // Databases & Stores
        Map.entry("mongodb", "MongoDB"), Map.entry("postgresql", "PostgreSQL"),
        Map.entry("mysql", "MySQL"), Map.entry("redis", "Redis"),
        Map.entry("elasticsearch", "Elasticsearch"), Map.entry("neo4j", "Neo4j"),
        Map.entry("cassandra", "Cassandra"), Map.entry("dynamodb", "DynamoDB"),
        Map.entry("sqlite", "SQLite"), Map.entry("mariadb", "MariaDB"),
        
        // DevOps & Cloud
        Map.entry("docker", "Docker"), Map.entry("kubernetes", "Kubernetes"),
        Map.entry("terraform", "Terraform"), Map.entry("aws", "AWS"),
        Map.entry("azure", "Azure"), Map.entry("gcp", "Google Cloud"),
        Map.entry("firebase", "Firebase"), Map.entry("vercel", "Vercel"),
        Map.entry("nginx", "Nginx"), Map.entry("apache", "Apache"),
        
        // AI/ML
        Map.entry("tensorflow", "TensorFlow"), Map.entry("pytorch", "PyTorch"),
        Map.entry("keras", "Keras"), Map.entry("scikit-learn", "Scikit-learn"),
        Map.entry("opencv", "OpenCV"), Map.entry("pandas", "Pandas"),
        Map.entry("numpy", "NumPy"), Map.entry("huggingface", "Hugging Face"),
        
        // Testing
        Map.entry("junit", "JUnit"), Map.entry("jest", "Jest"),
        Map.entry("cypress", "Cypress"), Map.entry("selenium", "Selenium"),
        Map.entry("mockito", "Mockito"), Map.entry("pytest", "Pytest")
    );

    /**
     * Patrones de arquitectura detectables
     */
    private static final Map<String, String> ARCHITECTURE_PATTERNS = Map.ofEntries(
        Map.entry("ddd", "Domain-Driven Design"),
        Map.entry("hexagonal", "Hexagonal Architecture"),
        Map.entry("clean-architecture", "Clean Architecture"),
        Map.entry("cqrs", "CQRS"),
        Map.entry("event-sourcing", "Event Sourcing"),
        Map.entry("microservices", "Microservices"),
        Map.entry("monolith", "Monolithic Architecture"),
        Map.entry("serverless", "Serverless"),
        Map.entry("mvc", "MVC Pattern"),
        Map.entry("mvvm", "MVVM Pattern"),
        Map.entry("rest-api", "RESTful API"),
        Map.entry("graphql", "GraphQL"),
        Map.entry("soap", "SOAP"),
        Map.entry("grpc", "gRPC"),
        Map.entry("reactive", "Reactive Programming"),
        Map.entry("solid", "SOLID Principles")
    );

    /**
     * Herramientas CI/CD y DevOps
     */
    private static final Map<String, String> CICD_KEYWORDS = Map.ofEntries(
        Map.entry("jenkins", "Jenkins"), Map.entry("github-actions", "GitHub Actions"),
        Map.entry("gitlab-ci", "GitLab CI"), Map.entry("travis", "Travis CI"),
        Map.entry("circleci", "CircleCI"), Map.entry("azure-pipelines", "Azure Pipelines"),
        Map.entry("docker", "Docker"), Map.entry("kubernetes", "Kubernetes"),
        Map.entry("k8s", "Kubernetes"), Map.entry("docker-compose", "Docker Compose"),
        Map.entry("maven", "Maven"), Map.entry("gradle", "Gradle"),
        Map.entry("ant", "Ant"), Map.entry("make", "Make"),
        Map.entry("vercel", "Vercel"), Map.entry("heroku", "Heroku"),
        Map.entry("netlify", "Netlify"), Map.entry("aws-codepipeline", "AWS CodePipeline"),
        Map.entry("terraform", "Terraform"), Map.entry("ansible", "Ansible")
    );

    /**
     * Categorías tecnológicas para detección de arquetipos
     */
    private static final Set<String> BACKEND_TECH = Set.of("Java", "Python", "Go", "C#", "PHP", "Ruby", "Spring Boot", "Django", "Express.js");
    private static final Set<String> FRONTEND_TECH = Set.of("JavaScript", "TypeScript", "React", "Vue.js", "Angular", "HTML", "CSS");
    private static final Set<String> MOBILE_TECH = Set.of("Swift", "Kotlin", "React Native", "Flutter", "Dart");
    private static final Set<String> DATA_TECH = Set.of("SQL", "Python", "R", "Pandas", "TensorFlow");

    public Map<String, LanguageStats> analyzeLanguages(List<Repository> repos) {
        Map<String, LanguageStats> languageStats = new HashMap<>();
        
        repos.forEach(repo -> {
            Map<String, Long> distribution = repo.getLanguageDistribution();
            if (distribution != null && !distribution.isEmpty()) {
                String primaryLanguage = distribution.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                
                if (primaryLanguage != null && !EXCLUDED_LANGUAGES.contains(primaryLanguage)) {
                    languageStats.computeIfAbsent(primaryLanguage, k -> new LanguageStats())
                        .addRepository(repo.getName());
                }
            }
        });
        
        return languageStats;
    }
    
    public Map<String, FrameworkStats> detectFrameworks(List<Repository> repos) {
        Map<String, FrameworkStats> frameworkStats = new HashMap<>();
        
        repos.forEach(repo -> {
            analyzeTopics(repo, FRAMEWORK_MAPPINGS, frameworkStats);
            analyzeTextContent(repo.getDescription(), FRAMEWORK_MAPPINGS, frameworkStats, repo.getName());
            analyzeTextContent(repo.getName(), FRAMEWORK_MAPPINGS, frameworkStats, repo.getName());
        });
        
        return frameworkStats;
    }

    public Map<String, FrameworkStats> detectArchitectures(List<Repository> repos) {
        Map<String, FrameworkStats> archStats = new HashMap<>();
        
        repos.forEach(repo -> {
            analyzeTopics(repo, ARCHITECTURE_PATTERNS, archStats);
            analyzeTextContent(repo.getDescription(), ARCHITECTURE_PATTERNS, archStats, repo.getName());
            analyzeTextContent(repo.getName(), ARCHITECTURE_PATTERNS, archStats, repo.getName());
        });
        
        return archStats;
    }
    
    public Map<String, FrameworkStats> detectCICD(List<Repository> repos) {
        Map<String, FrameworkStats> cicdStats = new HashMap<>();
        
        repos.forEach(repo -> {
            analyzeTopics(repo, CICD_KEYWORDS, cicdStats);
            analyzeTextContent(repo.getDescription(), CICD_KEYWORDS, cicdStats, repo.getName());
            analyzeTextContent(repo.getName(), CICD_KEYWORDS, cicdStats, repo.getName());
        });
        return cicdStats;
    }
    
    /**
     * Determina el arquetipo de desarrollador basado en las tecnologías detectadas
     */
    public String determineArchetype(Map<String, LanguageStats> languages, Map<String, FrameworkStats> frameworks) {
        int backendScore = calculateScore(languages, frameworks, BACKEND_TECH);
        int frontendScore = calculateScore(languages, frameworks, FRONTEND_TECH);
        int mobileScore = calculateScore(languages, frameworks, MOBILE_TECH);
        int dataScore = calculateScore(languages, frameworks, DATA_TECH);

        // Determinar rol dominante
        if (mobileScore > backendScore && mobileScore > frontendScore) return "Mobile Developer";
        if (dataScore > backendScore && dataScore > frontendScore) return "Data Engineer / Scientist";
        if (Math.abs(backendScore - frontendScore) < 3 && (backendScore > 2 || frontendScore > 2)) return "Full Stack Developer";
        if (backendScore > frontendScore) return "Backend Developer";
        if (frontendScore > backendScore) return "Frontend Developer";
        
        return "Software Developer"; // Default
    }

    private int calculateScore(Map<String, LanguageStats> languages, Map<String, FrameworkStats> frameworks, Set<String> techSet) {
        int score = 0;
        for (var entry : languages.entrySet()) {
            if (techSet.contains(entry.getKey())) score += entry.getValue().getProjectCount();
        }
        for (var entry : frameworks.entrySet()) {
            if (techSet.contains(entry.getKey())) score += entry.getValue().getProjectCount() * 2;
        }
        return score;
    }

    public List<OpenSourceProject> identifyOpenSourceProjects(List<Repository> repos) {
        return repos.stream()
            .filter(this::isOpenSourceProject)
            .map(OpenSourceProject::new)
            .sorted(Comparator.comparing(OpenSourceProject::stars).reversed())
            .collect(Collectors.toList());
    }
    
    private boolean isOpenSourceProject(Repository repo) {
        if (repo.isFork() || repo.isPrivate()) return false;
        if (repo.getStargazersCount() > 0) return true;
        List<String> topics = repo.getTopics();
        return topics != null && topics.stream()
            .anyMatch(t -> t.toLowerCase().contains("open-source") || t.toLowerCase().contains("opensource"));
    }
    
    private void analyzeTopics(Repository repo, Map<String, String> mappings, Map<String, FrameworkStats> stats) {
        if (repo.getTopics() == null) return;
        repo.getTopics().forEach(topic -> {
            String mapped = mappings.get(topic.toLowerCase());
            if (mapped != null) {
                stats.computeIfAbsent(mapped, k -> new FrameworkStats()).addRepository(repo.getName());
            }
        });
    }

    private void analyzeTextContent(String text, Map<String, String> mappings, Map<String, FrameworkStats> stats, String repoName) {
        if (text == null || text.isEmpty()) return;
        
        // Normalizar: convertir a minúsculas y reemplazar separadores por espacios
        String normalized = text.toLowerCase().replace("-", " ").replace("_", " ").replace(".", " ");
        String padded = " " + normalized + " "; // Padding para búsqueda exacta de palabras
        
        mappings.forEach((key, value) -> {
            // Buscar la clave tal cual (ej: "react") o con espacios en lugar de guiones (ej: "clean architecture")
            String searchKey = key.toLowerCase().replace("-", " ");
            
            if (padded.contains(" " + searchKey + " ")) {
                stats.computeIfAbsent(value, k -> new FrameworkStats()).addRepository(repoName);
            }
        });
    }

    public TechnicalMetadata generateTechnicalMetadata(List<Repository> repos) {
        log.info("📊 Analizando {} repositorios para generar metadata técnica extendida", repos.size());
        
        Map<String, LanguageStats> languages = analyzeLanguages(repos);
        Map<String, FrameworkStats> frameworks = detectFrameworks(repos);
        Map<String, FrameworkStats> architectures = detectArchitectures(repos);
        Map<String, FrameworkStats> cicdTools = detectCICD(repos);
        List<OpenSourceProject> openSourceProjects = identifyOpenSourceProjects(repos);
        String archetype = determineArchetype(languages, frameworks);
        
        log.info("✅ Metadata generada: Rol {}, {} lenguajes, {} frameworks, {} arquitecturas",
            archetype, languages.size(), frameworks.size(), architectures.size());
        
        return new TechnicalMetadata(languages, frameworks, architectures, cicdTools, openSourceProjects, archetype);
    }
    
    // ========== CLASES INTERNAS ==========
    
    public static class LanguageStats {
        private int projectCount = 0;
        private final Set<String> repositories = new HashSet<>();
        public void addRepository(String repoName) { repositories.add(repoName); projectCount++; }
        public int getProjectCount() { return projectCount; }
        public Set<String> getRepositories() { return repositories; }
    }
    
    public static class FrameworkStats {
        private int projectCount = 0;
        private final Set<String> repositories = new HashSet<>();
        public void addRepository(String repoName) { repositories.add(repoName); projectCount++; }
        public int getProjectCount() { return projectCount; }
        public Set<String> getRepositories() { return repositories; }
    }
    
    public record OpenSourceProject(
        String name, String fullName, String description, Integer stars, Integer forks, String url, List<String> topics
    ) {
        public OpenSourceProject(Repository repo) {
            this(repo.getName(), repo.getOwner() + "/" + repo.getName(), repo.getDescription(),
                repo.getStargazersCount(), repo.getForksCount(), repo.getUrl(), repo.getTopics());
        }
    }
    
    public record TechnicalMetadata(
        Map<String, LanguageStats> languages,
        Map<String, FrameworkStats> frameworks,
        Map<String, FrameworkStats> architectures,
        Map<String, FrameworkStats> cicdTools,
        List<OpenSourceProject> openSourceProjects,
        String archetype
    ) {}
}
