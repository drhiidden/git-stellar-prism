package com.drhdn.ghvis.application.service;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.entity.TechnicalCV;
import com.drhdn.ghvis.domain.entity.User;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import com.drhdn.ghvis.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para generar CVs técnicos desde GitHub.
 * 
 * Utiliza datos ya cargados de repositorios para generar un CV profesional
 * SIN hacer llamadas adicionales a la API de GitHub.
 * 
 * @author GitStellarPrism Team
 * @version 1.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CVService {
    
    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final RepositoryAnalyzer repositoryAnalyzer;
    
    /**
     * Genera un CV técnico completo para un usuario.
     */
    @Cacheable(value = "technicalCV", key = "#principal.name", unless = "#result == null")
    public Mono<TechnicalCV> generateCV(String username, Principal principal) {
        log.info("📄 Generando CV técnico para usuario: {}", principal.getName());
        
        Instant startTime = Instant.now();
        
        return Mono.zip(
            userRepository.getCurrentUser(principal),
            repositoryRepository.findByUser(principal).collectList()
        )
        .map(tuple -> {
            User user = tuple.getT1();
            List<Repository> repos = tuple.getT2();
            
            log.info("✅ Datos obtenidos: {} repositorios para {}", repos.size(), user.getLogin());
            
            // Generar CV (procesamiento local, sin API calls)
            TechnicalCV cv = buildCV(user, repos);
            
            long duration = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            log.info("🎉 CV generado en {}ms para {}", duration, user.getLogin());
            
            return cv;
        })
        .doOnError(error -> log.error("❌ Error generando CV: {}", error.getMessage()));
    }
    
    /**
     * Construye el CV completo desde los datos del usuario y repositorios.
     */
    private TechnicalCV buildCV(User user, List<Repository> repos) {
        // Analizar metadata técnica
        RepositoryAnalyzer.TechnicalMetadata metadata = repositoryAnalyzer.generateTechnicalMetadata(repos);
        
        TechnicalCV.CVHeader header = buildHeader(user, metadata);
        TechnicalCV.CVSummary summary = buildSummary(user, repos, metadata);
        
        return TechnicalCV.builder()
            .metadata(buildMetadata(repos.size(), user, metadata))
            .header(header)
            .summary(summary)
            .projects(mapProjects(repos))
            .aiPrompt(generateAIPrompt(header, summary, metadata, repos))
            .build();
    }
    
    private List<TechnicalCV.CVProject> mapProjects(List<Repository> repos) {
        return repos.stream()
            .sorted(Comparator.comparing(Repository::getStargazersCount).reversed())
            .map(repo -> TechnicalCV.CVProject.builder()
                .name(repo.getName())
                .description(repo.getDescription())
                .stars(repo.getStargazersCount())
                .language(getPrimaryLanguage(repo))
                .languages(repo.getLanguageDistribution())
                .topics(repo.getTopics())
                .url(repo.getUrl())
                .updatedAt(repo.getUpdatedAt() != null ? repo.getUpdatedAt().toString() : null)
                .createdAt(repo.getCreatedAt() != null ? repo.getCreatedAt().toString() : null)
                .build())
            .collect(Collectors.toList());
    }
    
    private String generateAIPrompt(TechnicalCV.CVHeader header, TechnicalCV.CVSummary summary, RepositoryAnalyzer.TechnicalMetadata metadata, List<Repository> repos) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("[CONTEXTO]\n");
        prompt.append(String.format("Soy %s (%s), un %s con %d años de experiencia activa.\n", 
            header.getName() != null ? header.getName() : header.getUsername(), 
            header.getUsername(), 
            summary.getRole(), 
            summary.getYearsActive()));
        prompt.append("Busco generar un resumen ejecutivo profesional para mi CV y perfil de LinkedIn.\n\n");
        
        prompt.append("[PERFIL TÉCNICO]\n");
        prompt.append("- Rol Inferido: ").append(summary.getRole()).append("\n");
        prompt.append("- Tecnologías Principales: ").append(String.join(", ", summary.getPrimaryTechnologies())).append("\n");
        
        if (!metadata.architectures().isEmpty()) {
            prompt.append("- Arquitecturas y Patrones: ").append(String.join(", ", metadata.architectures().keySet())).append("\n");
        }
        if (!metadata.cicdTools().isEmpty()) {
            prompt.append("- Herramientas CI/CD: ").append(String.join(", ", metadata.cicdTools().keySet())).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("[PROYECTOS DESTACADOS]\n");
        // Ordenar por estrellas y tomar top 5
        repos.stream()
            .sorted(Comparator.comparing(Repository::getStargazersCount).reversed())
            .limit(5)
            .forEach(r -> {
                prompt.append(String.format("- %s (⭐ %d): %s\n", r.getName(), r.getStargazersCount(), r.getDescription() != null ? r.getDescription() : "Sin descripción"));
                
                String lang = getPrimaryLanguage(r);
                if (!"Desconocido".equals(lang)) {
                    prompt.append(String.format("  Lenguaje: %s\n", lang));
                }
                
                if (r.getTopics() != null && !r.getTopics().isEmpty()) prompt.append(String.format("  Topics: %s\n", String.join(", ", r.getTopics())));
            });
        prompt.append("\n");
        
        prompt.append("[TAREA]\n");
        prompt.append("Actúa como un Reclutador Técnico Senior y redactor de CVs experto.\n");
        prompt.append("1. Escribe un 'Resumen Profesional' de 2-3 párrafos que sintetice mi experiencia, destacando mis fortalezas técnicas y arquitectónicas.\n");
        prompt.append("2. Genera una lista de 5 'Puntos Clave' (bullet points) con logros cuantificables o técnicos basados en mis proyectos.\n");
        prompt.append("3. Sugiere un título profesional (Headline) impactante.\n");
        prompt.append("4. Mantén un tono profesional, seguro y orientado a resultados.\n");
        
        return prompt.toString();
    }
    
    private String getPrimaryLanguage(Repository repo) {
        if (repo.getLanguageDistribution() == null || repo.getLanguageDistribution().isEmpty()) {
            return "Desconocido";
        }
        return repo.getLanguageDistribution().entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Desconocido");
    }
    
    private TechnicalCV.CVMetadata buildMetadata(int totalRepos, User user, RepositoryAnalyzer.TechnicalMetadata techMetadata) {
        return TechnicalCV.CVMetadata.builder()
            .generatedAt(Instant.now())
            .version("2.0.0")
            .source("GitStellarPrism")
            .totalRepositories(totalRepos)
            .githubProfile(user.getHtmlUrl())
            .build();
    }
    
    private TechnicalCV.CVHeader buildHeader(User user, RepositoryAnalyzer.TechnicalMetadata metadata) {
        return TechnicalCV.CVHeader.builder()
            .name(user.getName())
            .username(user.getLogin())
            .role(metadata.archetype()) // Inferred Role
            .bio(user.getBio())
            .location(user.getLocation())
            .email(user.getEmail())
            .website(null) 
            .avatarUrl(user.getAvatarUrl())
            .github(user.getHtmlUrl())
            .followers(user.getFollowers())
            .following(user.getFollowing())
            .build();
    }
    
    private TechnicalCV.CVSummary buildSummary(User user, List<Repository> repos, RepositoryAnalyzer.TechnicalMetadata techMetadata) {
        List<String> primaryTechnologies = getTopTechnologies(techMetadata);
        int activeYears = calculateActiveYears(repos);
        String headline = buildHeadline(techMetadata.archetype(), primaryTechnologies, activeYears);
        String description = buildProfessionalBiography(techMetadata, repos.size(), activeYears, primaryTechnologies);

        return TechnicalCV.CVSummary.builder()
            .role(techMetadata.archetype())
            .yearsActive(activeYears)
            .totalProjects(repos.size())
            .publicProjects((int) repos.stream().filter(r -> !r.isPrivate()).count())
            .primaryTechnologies(primaryTechnologies)
            .headline(headline)
            .description(description)
            .build();
    }

    private List<String> getTopTechnologies(RepositoryAnalyzer.TechnicalMetadata techMetadata) {
        return techMetadata.languages().entrySet().stream()
            .sorted(Map.Entry.<String, RepositoryAnalyzer.LanguageStats>comparingByValue(
                Comparator.comparing(RepositoryAnalyzer.LanguageStats::getProjectCount).reversed()
            ))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private int calculateActiveYears(List<Repository> repos) {
        if (repos.isEmpty()) return 0;
        Instant oldest = repos.stream()
            .map(Repository::getCreatedAt)
            .filter(t -> t != null)
            .min(Instant::compareTo)
            .orElse(Instant.now());
        return (int) ChronoUnit.YEARS.between(oldest.atZone(ZoneId.systemDefault()), Instant.now().atZone(ZoneId.systemDefault())) + 1;
    }

    private String buildHeadline(String archetype, List<String> primaryTechnologies, int activeYears) {
        return String.format("%s | %s Specialist | %d+ Years Exp", 
            archetype, 
            primaryTechnologies.isEmpty() ? "Software" : primaryTechnologies.get(0),
            activeYears);
    }

    private String buildProfessionalBiography(RepositoryAnalyzer.TechnicalMetadata techMetadata, int totalRepos, int activeYears, List<String> primaryTechnologies) {
        StringBuilder desc = new StringBuilder();
        desc.append(String.format("Passionate %s with %d+ years of experience across %d projects. ", 
            techMetadata.archetype(), activeYears, totalRepos));
        
        if (!primaryTechnologies.isEmpty()) {
            desc.append("Expertise in ").append(String.join(", ", primaryTechnologies)).append(". ");
        }
        
        if (!techMetadata.architectures().isEmpty()) {
            desc.append("Demonstrated ability in implementing architectural patterns such as ")
                .append(String.join(", ", techMetadata.architectures().keySet()))
                .append(". ");
        }
        
        if (!techMetadata.openSourceProjects().isEmpty()) {
            desc.append(String.format("Active Open Source contributor with %d public projects including %s.",
                techMetadata.openSourceProjects().size(),
                techMetadata.openSourceProjects().get(0).name()));
        }
        return desc.toString();
    }
}
