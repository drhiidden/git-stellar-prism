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
 * @version 1.0.0
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
     * 
     * Este método es EFICIENTE porque:
     * 1. Usa datos ya en caché (no hace requests a GitHub)
     * 2. Todo el procesamiento es local
     * 3. Resultado se cachea por 24 horas
     * 
     * @param username Nombre de usuario de GitHub (no usado, se obtiene del principal)
     * @param principal Usuario autenticado
     * @return Mono con el CV técnico generado
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
     * Este método es PURAMENTE LOCAL, no hace network calls.
     */
    private TechnicalCV buildCV(User user, List<Repository> repos) {
        // Analizar metadata técnica
        RepositoryAnalyzer.TechnicalMetadata metadata = repositoryAnalyzer.generateTechnicalMetadata(repos);
        
        return TechnicalCV.builder()
            .metadata(buildMetadata(repos.size(), user, metadata))
            .header(buildHeader(user))
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
    
    private TechnicalCV.CVHeader buildHeader(User user) {
        return TechnicalCV.CVHeader.builder()
            .name(user.getName())
            .username(user.getLogin())
            .bio(user.getBio())
            .location(user.getLocation())
            .email(user.getEmail())
            .website(null) // TODO: agregar campo website a User entity si GitHub lo provee
            .avatarUrl(user.getAvatarUrl())
            .github(user.getHtmlUrl())
            .followers(user.getFollowers())
            .following(user.getFollowing())
            .build();
    }
    
    private TechnicalCV.CVSummary buildSummary(User user, List<Repository> repos, RepositoryAnalyzer.TechnicalMetadata techMetadata) {
        // Generar resumen con metadata técnica
        List<String> primaryTechnologies = techMetadata.languages().entrySet().stream()
            .sorted(Map.Entry.<String, RepositoryAnalyzer.LanguageStats>comparingByValue(
                Comparator.comparing(RepositoryAnalyzer.LanguageStats::getProjectCount).reversed()
            ))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        return TechnicalCV.CVSummary.builder()
            .totalProjects(repos.size())
            .publicProjects((int) repos.stream().filter(r -> !r.isPrivate()).count())
            .primaryTechnologies(primaryTechnologies)
            .build();
    }
}

