package com.drhdn.ghvis.application.query;

import com.drhdn.ghvis.domain.entity.Repository;

import java.security.Principal;
import java.util.List;
import java.util.Set;

/**
 * Queries relacionadas con consulta de repositorios.
 * 
 * Estas queries NO modifican el estado:
 * - Solo leen datos
 * - Pueden usar caché agresivamente
 * - Optimizadas para performance
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public sealed interface RepositoryQuery<R> extends Query<R>
    permits RepositoryQuery.GetRepositories,
            RepositoryQuery.GetRepositoriesPaged,
            RepositoryQuery.SearchRepositories,
            RepositoryQuery.GetRepositoryById,
            RepositoryQuery.GetRepositoryStatistics {
    
    /**
     * Query para obtener todos los repositorios de un usuario.
     * 
     * Retorna: List<Repository>
     * 
     * @param username Nombre de usuario
     * @param principal Principal de autenticación (puede ser null para datos públicos)
     */
    record GetRepositories(
        String username,
        Principal principal
    ) implements RepositoryQuery<List<Repository>> {
        
        @Override
        public String getCacheKey() {
            return "repositories:" + username;
        }
    }
    
    /**
     * Query paginada para obtener repositorios de un usuario.
     * 
     * Usa cursor-based pagination para mejor performance.
     * 
     * Retorna: PagedResponse<Repository>
     * 
     * @param username Nombre de usuario
     * @param cursor Cursor de paginación (null para primera página)
     * @param limit Número de items por página
     * @param principal Principal de autenticación
     */
    record GetRepositoriesPaged(
        String username,
        String cursor,
        int limit,
        Principal principal
    ) implements RepositoryQuery<PagedResponse<Repository>> {
        
        public GetRepositoriesPaged(String username, int limit, Principal principal) {
            this(username, null, limit, principal);
        }
        
        @Override
        public String getCacheKey() {
            return "repositories:paged:" + username + ":" + cursor + ":" + limit;
        }
    }
    
    /**
     * Query para buscar repositorios con filtros.
     * 
     * Retorna: List<Repository>
     * 
     * @param username Nombre de usuario
     * @param searchTerm Término de búsqueda (busca en nombre, descripción)
     * @param technologies Filtro de tecnologías (AND logic)
     * @param isPrivate Filtro de visibilidad (null = todos)
     * @param sortBy Campo de ordenamiento (updated, stars, name)
     * @param principal Principal de autenticación
     */
    record SearchRepositories(
        String username,
        String searchTerm,
        Set<String> technologies,
        Boolean isPrivate,
        String sortBy,
        Principal principal
    ) implements RepositoryQuery<List<Repository>> {
        
        public SearchRepositories(String username, String searchTerm, Principal principal) {
            this(username, searchTerm, Set.of(), null, "updated", principal);
        }
        
        @Override
        public String getCacheKey() {
            return "repositories:search:" + username + ":" + 
                   (searchTerm != null ? searchTerm : "all") + ":" +
                   String.join(",", technologies) + ":" +
                   sortBy;
        }
        
        @Override
        public boolean isCacheable() {
            // Búsquedas con filtros dinámicos no se cachean tanto tiempo
            return false;
        }
    }
    
    /**
     * Query para obtener un repositorio específico por ID.
     * 
     * Retorna: Repository (opcional)
     * 
     * @param repositoryId ID del repositorio
     * @param principal Principal de autenticación
     */
    record GetRepositoryById(
        Long repositoryId,
        Principal principal
    ) implements RepositoryQuery<Repository> {
        
        @Override
        public String getCacheKey() {
            return "repository:id:" + repositoryId;
        }
    }
    
    /**
     * Query para obtener estadísticas de repositorios de un usuario.
     * 
     * Retorna: RepositoryStatistics
     * 
     * @param username Nombre de usuario
     * @param principal Principal de autenticación
     */
    record GetRepositoryStatistics(
        String username,
        Principal principal
    ) implements RepositoryQuery<RepositoryStatistics> {
        
        @Override
        public String getCacheKey() {
            return "repositories:stats:" + username;
        }
    }
    
    // ========== DTOs de Respuesta ==========
    
    /**
     * Respuesta paginada genérica
     */
    record PagedResponse<T>(
        List<T> items,
        String nextCursor,
        String previousCursor,
        boolean hasMore,
        int total
    ) {}
    
    /**
     * Estadísticas de repositorios
     */
    record RepositoryStatistics(
        int totalRepositories,
        int publicRepositories,
        int privateRepositories,
        int totalStars,
        int totalForks,
        long totalSize,
        java.util.Map<String, Integer> languageCounts
    ) {}
}

