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
        
        return TechnicalCV.builder()
            .metadata(buildMetadata(repos.size(), user, metadata))
            .header(buildHeader(user, metadata))
            .summary(buildSummary(user, repos, metadata))
            .build();
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
