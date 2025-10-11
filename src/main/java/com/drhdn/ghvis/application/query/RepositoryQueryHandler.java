package com.drhdn.ghvis.application.query;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador de queries relacionadas con repositorios.
 * 
 * Responsabilidades:
 * - Leer datos de repositorios
 * - Aplicar filtros y búsquedas
 * - Gestionar paginación
 * - Calcular estadísticas
 * 
 * IMPORTANTE: Este handler NUNCA modifica datos, solo los lee.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryQueryHandler {
    
    private final RepositoryRepository repositoryRepository;
    
    /**
     * Maneja GetRepositories query.
     * 
     * Optimizaciones:
     * - Usa caché con TTL de 15 minutos
     * - Carga todos los repos en memoria (ok para ~50-100 repos)
     */
    @Cacheable(value = "repositories", key = "#query.username()")
    public Mono<List<Repository>> handle(RepositoryQuery.GetRepositories query) {
        log.debug("📖 Ejecutando query: GetRepositories para usuario: {}", query.username());
        
        return repositoryRepository.findByUser(query.principal())
            .collectList()
            .doOnSuccess(repos -> {
                log.debug("✅ Query GetRepositories completada: {} repos encontrados", 
                    repos.size());
            })
            .doOnError(error -> {
                log.error("❌ Error ejecutando GetRepositories: {}", error.getMessage());
            });
    }
    
    /**
     * Maneja GetRepositoriesPaged query.
     * 
     * Usa cursor-based pagination:
     * - Cursor = índice codificado en Base64
     * - Más eficiente que offset-based para grandes datasets
     */
    @Cacheable(value = "repositories-paged", key = "#query.getCacheKey()")
    public Mono<RepositoryQuery.PagedResponse<Repository>> handle(
        RepositoryQuery.GetRepositoriesPaged query) {
        
        log.debug("📖 Ejecutando query: GetRepositoriesPaged - cursor: {}, limit: {}", 
            query.cursor(), query.limit());
        
        return repositoryRepository.findByUser(query.principal())
            .collectList()
            .map(repos -> createPagedResponse(repos, query.cursor(), query.limit()))
            .doOnSuccess(page -> {
                log.debug("✅ Query GetRepositoriesPaged completada: {} items en página", 
                    page.items().size());
            });
    }
    
    /**
     * Maneja SearchRepositories query.
     * 
     * Búsqueda en memoria con filtros múltiples:
     * - Texto en nombre/descripción
     * - Tecnologías (AND logic)
     * - Visibilidad
     * - Ordenamiento
     */
    public Mono<List<Repository>> handle(RepositoryQuery.SearchRepositories query) {
        log.debug("📖 Ejecutando query: SearchRepositories - term: '{}', techs: {}", 
            query.searchTerm(), query.technologies());
        
        return repositoryRepository.findByUser(query.principal())
            .collectList()
            .map(repos -> applySearchFilters(repos, query))
            .doOnSuccess(filtered -> {
                log.debug("✅ Query SearchRepositories completada: {} repos encontrados", 
                    filtered.size());
            });
    }
    
    /**
     * Maneja GetRepositoryById query.
     */
    @Cacheable(value = "repository", key = "#query.repositoryId()")
    public Mono<Repository> handle(RepositoryQuery.GetRepositoryById query) {
        log.debug("📖 Ejecutando query: GetRepositoryById - id: {}", query.repositoryId());
        
        // Como no tenemos repositorio por ID directo, buscar en lista
        return repositoryRepository.findByUser(query.principal())
            .filter(repo -> repo.getId().equals(query.repositoryId()))
            .next()
            .doOnSuccess(repo -> {
                if (repo != null) {
                    log.debug("✅ Repositorio encontrado: {}", repo.getName());
                } else {
                    log.debug("⚠️ Repositorio no encontrado con id: {}", query.repositoryId());
                }
            });
    }
    
    /**
     * Maneja GetRepositoryStatistics query.
     * 
     * Calcula estadísticas agregadas:
     * - Total de repos
     * - Públicos vs privados
     * - Total de stars/forks
     * - Distribución de lenguajes
     */
    @Cacheable(value = "repository-stats", key = "#query.username()")
    public Mono<RepositoryQuery.RepositoryStatistics> handle(
        RepositoryQuery.GetRepositoryStatistics query) {
        
        log.debug("📖 Ejecutando query: GetRepositoryStatistics para: {}", query.username());
        
        return repositoryRepository.findByUser(query.principal())
            .collectList()
            .map(this::calculateStatistics)
            .doOnSuccess(stats -> {
                log.debug("✅ Estadísticas calculadas: {} repos, {} stars", 
                    stats.totalRepositories(), stats.totalStars());
            });
    }
    
    // ========== Utilidades ==========
    
    /**
     * Crea una respuesta paginada desde una lista de repos.
     */
    private RepositoryQuery.PagedResponse<Repository> createPagedResponse(
        List<Repository> allRepos, String cursor, int limit) {
        
        // Decodificar cursor (índice del primer item)
        int startIndex = 0;
        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(cursor));
                startIndex = Integer.parseInt(decoded);
            } catch (Exception e) {
                log.warn("⚠️ Cursor inválido, usando índice 0: {}", cursor);
            }
        }
        
        int endIndex = Math.min(startIndex + limit, allRepos.size());
        List<Repository> page = allRepos.subList(startIndex, endIndex);
        
        // Generar cursors para siguiente/anterior
        String nextCursor = null;
        if (endIndex < allRepos.size()) {
            nextCursor = java.util.Base64.getEncoder()
                .encodeToString(String.valueOf(endIndex).getBytes());
        }
        
        String previousCursor = null;
        if (startIndex > 0) {
            int prevIndex = Math.max(0, startIndex - limit);
            previousCursor = java.util.Base64.getEncoder()
                .encodeToString(String.valueOf(prevIndex).getBytes());
        }
        
        return new RepositoryQuery.PagedResponse<>(
            page,
            nextCursor,
            previousCursor,
            endIndex < allRepos.size(),
            allRepos.size()
        );
    }
    
    /**
     * Aplica filtros de búsqueda a una lista de repositorios.
     */
    private List<Repository> applySearchFilters(
        List<Repository> repos, RepositoryQuery.SearchRepositories query) {
        
        var filtered = repos.stream();
        
        // Filtro por texto (nombre o descripción)
        if (query.searchTerm() != null && !query.searchTerm().isBlank()) {
            String term = query.searchTerm().toLowerCase();
            filtered = filtered.filter(repo -> {
                boolean matchName = repo.getName().toLowerCase().contains(term);
                boolean matchDesc = repo.getDescription() != null && 
                    repo.getDescription().toLowerCase().contains(term);
                return matchName || matchDesc;
            });
        }
        
        // Filtro por tecnologías (AND logic)
        if (query.technologies() != null && !query.technologies().isEmpty()) {
            filtered = filtered.filter(repo -> {
                var repoTechs = extractTechnologies(repo);
                return repoTechs.containsAll(query.technologies());
            });
        }
        
        // Filtro por visibilidad
        if (query.isPrivate() != null) {
            filtered = filtered.filter(repo -> 
                repo.isPrivate() == query.isPrivate()
            );
        }
        
        // Ordenamiento
        filtered = switch (query.sortBy() != null ? query.sortBy() : "updated") {
            case "stars" -> filtered.sorted(
                Comparator.comparing(Repository::getStargazersCount).reversed());
            case "name" -> filtered.sorted(
                Comparator.comparing(Repository::getName));
            case "updated" -> filtered.sorted(
                Comparator.comparing(Repository::getUpdatedAt).reversed());
            default -> filtered;
        };
        
        return filtered.collect(Collectors.toList());
    }
    
    /**
     * Extrae tecnologías de un repositorio (lenguajes + topics).
     */
    private java.util.Set<String> extractTechnologies(Repository repo) {
        java.util.Set<String> techs = new java.util.HashSet<>();
        
        // Agregar lenguajes
        if (repo.getLanguageDistribution() != null) {
            techs.addAll(repo.getLanguageDistribution().keySet());
        }
        
        // Agregar topics
        if (repo.getTopics() != null) {
            techs.addAll(repo.getTopics());
        }
        
        return techs;
    }
    
    /**
     * Calcula estadísticas agregadas de una lista de repositorios.
     */
    private RepositoryQuery.RepositoryStatistics calculateStatistics(List<Repository> repos) {
        int totalStars = 0;
        int totalForks = 0;
        long totalSize = 0;
        Map<String, Integer> languageCounts = new java.util.HashMap<>();
        
        for (Repository repo : repos) {
            totalStars += repo.getStargazersCount() != null ? repo.getStargazersCount() : 0;
            totalForks += repo.getForksCount() != null ? repo.getForksCount() : 0;
            totalSize += repo.getSize() != null ? repo.getSize() : 0;
            
            // Contar lenguajes
            if (repo.getLanguageDistribution() != null) {
                repo.getLanguageDistribution().keySet().forEach(lang -> 
                    languageCounts.merge(lang, 1, Integer::sum)
                );
            }
        }
        
        int publicRepos = (int) repos.stream().filter(r -> !r.isPrivate()).count();
        int privateRepos = repos.size() - publicRepos;
        
        return new RepositoryQuery.RepositoryStatistics(
            repos.size(),
            publicRepos,
            privateRepos,
            totalStars,
            totalForks,
            totalSize,
            languageCounts
        );
    }
}

