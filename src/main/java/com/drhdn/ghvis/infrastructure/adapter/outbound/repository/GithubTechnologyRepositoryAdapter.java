package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.entity.Technology;
import com.drhdn.ghvis.domain.port.LanguageRepository;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import com.drhdn.ghvis.domain.port.TechnologyRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter para detección de tecnologías usando GitHub API.
 * Implementa el puerto TechnologyRepository siguiendo arquitectura hexagonal.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubTechnologyRepositoryAdapter implements TechnologyRepository {

    private final GithubApiAdapter githubApiAdapter;
    private final LanguageRepository languageRepository;
    private final RepositoryRepository repositoryRepository;

    // Mapas de tecnologías conocidas por lenguaje y archivos
    private static final Map<String, Set<String>> LANGUAGE_FRAMEWORKS = Map.of(
        "JavaScript", Set.of("React", "Vue", "Angular", "Express", "Node.js"),
        "TypeScript", Set.of("React", "Vue", "Angular", "Nest.js", "Deno"),
        "Java", Set.of("Spring", "Spring Boot", "Hibernate", "Maven", "Gradle"),
        "Python", Set.of("Django", "Flask", "FastAPI", "Pandas", "NumPy"),
        "C#", Set.of(".NET", "ASP.NET", "Entity Framework", "Blazor"),
        "Go", Set.of("Gin", "Echo", "Fiber", "GORM"),
        "Rust", Set.of("Actix", "Rocket", "Tokio", "Serde")
    );

    private static final Map<String, String> BUILD_TOOLS = Map.of(
        "package.json", "npm/yarn",
        "pom.xml", "Maven",
        "build.gradle", "Gradle",
        "Cargo.toml", "Cargo",
        "requirements.txt", "pip",
        "go.mod", "Go Modules",
        "composer.json", "Composer"
    );

    @Override
    public Flux<Technology> detectTechnologies(String owner, String repo, Principal principal) {
        log.debug("🔍 Detectando tecnologías para repositorio: {}/{}", owner, repo);
        
        return Mono.zip(
            detectFromLanguages(owner, repo, principal).collectList(),
            detectFrameworks(owner, repo, principal).collectList(),
            detectBuildTools(owner, repo, principal).collectList()
        )
        .flatMapMany(tuple -> {
            List<Technology> allTechnologies = new ArrayList<>();
            allTechnologies.addAll(tuple.getT1());
            allTechnologies.addAll(tuple.getT2());
            allTechnologies.addAll(tuple.getT3());
            return Flux.fromIterable(allTechnologies);
        })
        .distinct(Technology::getName)
        .doOnComplete(() -> log.debug("✅ Tecnologías detectadas para: {}/{}", owner, repo))
        .doOnError(error -> log.error("❌ Error detectando tecnologías para {}/{}: {}", 
            owner, repo, error.getMessage()));
    }

    @Override
    public Flux<Technology> getTopTechnologies(String owner, String repo, Principal principal, int limit) {
        log.debug("🔍 Obteniendo top {} tecnologías para repositorio: {}/{}", limit, owner, repo);
        
        return detectTechnologies(owner, repo, principal)
            .sort((t1, t2) -> Double.compare(t2.getConfidence(), t1.getConfidence()))
            .take(limit)
            .doOnComplete(() -> log.debug("✅ Top {} tecnologías obtenidas para: {}/{}", limit, owner, repo));
    }

    @Override
    public Flux<Technology> detectFrameworks(String owner, String repo, Principal principal) {
        log.debug("🔍 Detectando frameworks para repositorio: {}/{}", owner, repo);
        
        return languageRepository.getLanguagesMap(owner, repo, principal)
            .flatMapMany(languagesMap -> {
                List<Technology> frameworks = new ArrayList<>();
                
                for (String language : languagesMap.keySet()) {
                    Set<String> langFrameworks = LANGUAGE_FRAMEWORKS.getOrDefault(language, Set.of());
                    for (String framework : langFrameworks) {
                        frameworks.add(Technology.builder()
                            .name(framework)
                            .category("Framework")
                            .language(language)
                            .confidence(calculateFrameworkConfidence(framework, language))
                            .repositoryOwner(owner)
                            .repositoryName(repo)
                            .detectedAt(Instant.now())
                            .build());
                    }
                }
                
                return Flux.fromIterable(frameworks);
            })
            .doOnComplete(() -> log.debug("✅ Frameworks detectados para: {}/{}", owner, repo));
    }

    @Override
    public Flux<Technology> detectBuildTools(String owner, String repo, Principal principal) {
        log.debug("🔍 Detectando herramientas de build para repositorio: {}/{}", owner, repo);
        
        // Simular detección basada en archivos conocidos
        List<Technology> buildTools = BUILD_TOOLS.entrySet().stream()
            .map(entry -> Technology.builder()
                .name(entry.getValue())
                .category("Build Tool")
                .language("Configuration")
                .confidence(0.8) // Confianza fija por simplicidad
                .repositoryOwner(owner)
                .repositoryName(repo)
                .detectedAt(Instant.now())
                .build())
            .collect(Collectors.toList());
        
        return Flux.fromIterable(buildTools)
            .doOnComplete(() -> log.debug("✅ Herramientas de build detectadas para: {}/{}", owner, repo));
    }

    @Override
    public Flux<TechnologyStats> getTechnologyStatsByUser(String username, Principal principal) {
        log.debug("🔍 Obteniendo estadísticas de tecnologías para usuario: {}", username);
        
        // Implementación básica - se podría expandir
        return Flux.<TechnologyRepository.TechnologyStats>just(
                new TechnologyStatsImpl(username, "Unknown", "Unknown"))
            .doOnComplete(() -> log.debug("✅ Estadísticas de tecnologías obtenidas para usuario: {}", username));
    }

    @Override
    public Flux<Technology> getTopTechnologiesByUser(String username, Principal principal, int limit) {
        log.debug("🔍 Obteniendo top {} tecnologías para usuario: {}", limit, username);
        
        return repositoryRepository.findByUser(principal)
            .flatMap(repository -> detectTechnologies(repository.getOwner(), repository.getName(), principal)
                .collectList()
                .onErrorReturn(Collections.emptyList()))
            .flatMapIterable(list -> list)
            .groupBy(Technology::getName)
            .flatMap(group -> group
                .reduce((t1, t2) -> Technology.builder()
                    .name(t1.getName())
                    .category(t1.getCategory())
                    .language(t1.getLanguage())
                    .confidence(Math.max(t1.getConfidence(), t2.getConfidence()))
                    .repositoryOwner(username)
                    .repositoryName("aggregated")
                    .detectedAt(java.time.Instant.now())
                    .build()))
            .sort((t1, t2) -> Double.compare(t2.getConfidence(), t1.getConfidence()))
            .take(limit)
            .doOnComplete(() -> log.debug("✅ Top {} tecnologías obtenidas para usuario: {}", limit, username))
            .doOnError(error -> log.error("❌ Error obteniendo tecnologías para usuario {}: {}", username, error.getMessage()));
    }

    @Override
    public Mono<Boolean> hasTechnologies(String owner, String repo, Principal principal) {
        log.debug("🔍 Verificando si repositorio {}/{} tiene tecnologías detectables", owner, repo);
        
        return detectTechnologies(owner, repo, principal)
            .hasElements()
            .doOnNext(hasTech -> log.debug("✅ Repositorio {}/{} tiene tecnologías: {}", 
                owner, repo, hasTech));
    }

    @Override
    public Mono<TechnologyCompatibility> analyzeTechnologyCompatibility(String owner, String repo, Principal principal) {
        log.debug("🔍 Analizando compatibilidad de tecnologías para repositorio: {}/{}", owner, repo);
        
        return detectTechnologies(owner, repo, principal)
            .collectList()
            .map(technologies -> (TechnologyRepository.TechnologyCompatibility) new TechnologyCompatibilityImpl(technologies))
            .doOnSuccess(compat -> log.debug("✅ Análisis de compatibilidad completado para: {}/{} (Score: {})", 
                owner, repo, compat.getCompatibilityScore()));
    }

    // Métodos auxiliares privados

    /**
     * Detecta tecnologías basándose en los lenguajes del repositorio.
     */
    private Flux<Technology> detectFromLanguages(String owner, String repo, Principal principal) {
        return languageRepository.getLanguagesMap(owner, repo, principal)
            .flatMapMany(languagesMap -> 
                Flux.<Map.Entry<String, Long>>fromIterable(languagesMap.entrySet())
                    .map(entry -> Technology.builder()
                        .name(entry.getKey())
                        .category("Programming Language")
                        .language(entry.getKey())
                        .confidence(0.9) // Alta confianza para lenguajes detectados
                        .repositoryOwner(owner)
                        .repositoryName(repo)
                        .detectedAt(Instant.now())
                        .build())
            );
    }

    /**
     * Calcula la confianza de detección de un framework.
     */
    private double calculateFrameworkConfidence(String framework, String language) {
        // Lógica simple de confianza basada en popularidad
        Map<String, Double> frameworkConfidence = Map.of(
            "React", 0.9,
            "Spring Boot", 0.9,
            "Django", 0.8,
            "Express", 0.8,
            "Angular", 0.7
        );
        
        return frameworkConfidence.getOrDefault(framework, 0.5);
    }

    /**
     * Implementación de TechnologyStats.
     */
    private static class TechnologyStatsImpl implements TechnologyRepository.TechnologyStats {
        private final String repositoryName;
        private final String repositoryOwner;
        private final String primaryTechnology;

        public TechnologyStatsImpl(String repositoryOwner, String repositoryName, String primaryTechnology) {
            this.repositoryOwner = repositoryOwner;
            this.repositoryName = repositoryName;
            this.primaryTechnology = primaryTechnology;
        }

        @Override
        public String getRepositoryName() { return repositoryName; }

        @Override
        public String getRepositoryOwner() { return repositoryOwner; }

        @Override
        public String getPrimaryTechnology() { return primaryTechnology; }

        @Override
        public Double getPrimaryTechnologyPercentage() { return 0.0; }

        @Override
        public Integer getTotalTechnologies() { return 0; }

        @Override
        public Integer getFrameworkCount() { return 0; }

        @Override
        public Integer getBuildToolCount() { return 0; }

        @Override
        public Instant getAnalyzedAt() { return Instant.now(); }

        @Override
        public String getComplexityLevel() { return "Simple"; }
    }

    /**
     * Implementación de TechnologyCompatibility.
     */
    private static class TechnologyCompatibilityImpl implements TechnologyRepository.TechnologyCompatibility {
        private final List<Technology> technologies;

        public TechnologyCompatibilityImpl(List<Technology> technologies) {
            this.technologies = technologies;
        }

        @Override
        public Integer getCompatibilityScore() {
            // Scoring simple basado en número de tecnologías
            if (technologies.size() <= 3) return 95;
            if (technologies.size() <= 6) return 80;
            if (technologies.size() <= 10) return 65;
            return 50;
        }

        @Override
        public Map<String, String> getConflicts() {
            // Análisis básico de conflictos
            return Map.of();
        }

        @Override
        public Map<String, String> getSuggestions() {
            // Sugerencias básicas
            return Map.of("optimization", "Consider reducing technology stack complexity");
        }

        @Override
        public String getMaturityLevel() {
            return technologies.size() > 5 ? "Complex" : "Mature";
        }
    }
} 