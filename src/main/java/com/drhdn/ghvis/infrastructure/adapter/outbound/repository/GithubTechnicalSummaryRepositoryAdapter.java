package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.entity.Language;
import com.drhdn.ghvis.domain.entity.TechnicalSummary;
import com.drhdn.ghvis.domain.entity.Technology;
import com.drhdn.ghvis.domain.port.TechnicalSummaryRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adaptador para generación de resúmenes técnicos desde GitHub.
 * Implementa el puerto TechnicalSummaryRepository usando GitHub API.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubTechnicalSummaryRepositoryAdapter implements TechnicalSummaryRepository {

    private final GithubApiAdapter githubApiAdapter;

    @Override
    public Mono<TechnicalSummary> generateForRepository(String owner, String repo, Principal principal) {
        return generateForRepositoryWithType(owner, repo, "basic", principal);
    }

    @Override
    public Mono<TechnicalSummary> generateForRepositoryWithType(
            String owner, 
            String repo, 
            String summaryType, 
            Principal principal) {
        
        log.info("📊 Generando resumen técnico {} para {}/{}", summaryType, owner, repo);

        // Detectar tecnologías desde el árbol de archivos
        return githubApiAdapter.getRepositoryTree(owner, repo, "HEAD", principal)
            .flatMap(tree -> {
                List<Technology> technologies = detectTechnologiesFromTree(tree);
                
                // Convertir tecnologías a lista de strings
                List<String> techNames = technologies.stream()
                    .map(Technology::getName)
                    .collect(Collectors.toList());
                
                // Crear resumen técnico
                TechnicalSummary summary = new TechnicalSummary();
                summary.setRepositoryName(repo);
                summary.setRepositoryOwner(owner);
                summary.setLanguages(List.of()); // TODO: Implementar detección de lenguajes
                summary.setTechnologies(techNames);
                summary.setGeneratedAt(java.time.Instant.now());
                summary.setTotalFiles(tree.size());
                summary.setPrimaryLanguage("Unknown"); // TODO: Detectar lenguaje principal

                log.info("✅ Resumen técnico generado para {}/{}: {} tecnologías", 
                    owner, repo, technologies.size());

                return Mono.just(summary);
            })
            .doOnError(error -> log.error("❌ Error generando resumen técnico para {}/{}: {}", 
                owner, repo, error.getMessage()));
    }

    /**
     * Detecta tecnologías basándose en el árbol de archivos del repositorio.
     */
    private List<Technology> detectTechnologiesFromTree(List<Map<String, Object>> tree) {
        List<Technology> technologies = new ArrayList<>();
        
        for (Map<String, Object> file : tree) {
            String path = (String) file.get("path");
            if (path == null) continue;

            // Detectar Maven
            if (path.equals("pom.xml")) {
                technologies.add(createTechnology("Maven", "Build Tool", "Java"));
            }
            
            // Detectar Gradle
            if (path.equals("build.gradle") || path.equals("build.gradle.kts")) {
                technologies.add(createTechnology("Gradle", "Build Tool", "Java/Kotlin"));
            }
            
            // Detectar npm
            if (path.equals("package.json")) {
                technologies.add(createTechnology("npm", "Package Manager", "JavaScript"));
            }
            
            // Detectar Docker
            if (path.equals("Dockerfile") || path.equals("docker-compose.yml")) {
                technologies.add(createTechnology("Docker", "Containerization", "DevOps"));
            }
            
            // Detectar Python
            if (path.equals("requirements.txt") || path.equals("Pipfile")) {
                technologies.add(createTechnology("pip", "Package Manager", "Python"));
            }
            
            // Detectar Spring Boot
            if (path.contains("application.properties") || path.contains("application.yml")) {
                technologies.add(createTechnology("Spring Boot", "Framework", "Java"));
            }
            
            // Detectar React
            if (path.contains("react") || (path.equals("package.json") && path.contains("react"))) {
                technologies.add(createTechnology("React", "Framework", "JavaScript"));
            }
        }

        return technologies.stream().distinct().collect(Collectors.toList());
    }

    private Technology createTechnology(String name, String category, String ecosystem) {
        return Technology.builder()
            .name(name)
            .category(category)
            .language(ecosystem)
            .confidence(0.8)
            .build();
    }
}

