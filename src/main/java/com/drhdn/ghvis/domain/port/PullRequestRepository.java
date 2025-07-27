package com.drhdn.ghvis.domain.port;

import com.drhdn.ghvis.domain.entity.PullRequest;
import reactor.core.publisher.Flux;

import java.security.Principal;

/**
 * Puerto de salida para operaciones con Pull Requests.
 * 
 * Define el contrato para obtener pull requests de repositorios GitHub,
 * incluyendo filtros por estado, ordenamiento y paginación.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface PullRequestRepository {
    
    /**
     * Obtiene pull requests de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Flux de pull requests
     */
    Flux<PullRequest> findByRepository(String owner, String repo, Principal principal);
    
    /**
     * Obtiene pull requests de un repositorio con filtros.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param state Estado de los PRs ("open", "closed", "all")
     * @param sort Campo de ordenamiento
     * @param direction Dirección del ordenamiento
     * @param perPage Elementos por página
     * @param page Número de página
     * @param principal Usuario autenticado
     * @return Flux de pull requests
     */
    Flux<PullRequest> findByRepositoryWithFilters(String owner, String repo, String state, 
                                                String sort, String direction, int perPage, 
                                                int page, Principal principal);
} 