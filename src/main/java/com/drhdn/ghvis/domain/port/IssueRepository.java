package com.drhdn.ghvis.domain.port;

import com.drhdn.ghvis.domain.entity.Issue;
import reactor.core.publisher.Flux;

import java.security.Principal;

/**
 * Puerto de salida para operaciones con Issues.
 * 
 * Define el contrato para obtener issues de repositorios GitHub,
 * incluyendo filtros por estado, ordenamiento y paginación.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface IssueRepository {
    
    /**
     * Obtiene issues de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Flux de issues
     */
    Flux<Issue> findByRepository(String owner, String repo, Principal principal);
    
    /**
     * Obtiene issues de un repositorio con filtros.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param state Estado de los issues ("open", "closed", "all")
     * @param sort Campo de ordenamiento
     * @param direction Dirección del ordenamiento
     * @param perPage Elementos por página
     * @param page Número de página
     * @param principal Usuario autenticado
     * @return Flux de issues
     */
    Flux<Issue> findByRepositoryWithFilters(String owner, String repo, String state, 
                                          String sort, String direction, int perPage, 
                                          int page, Principal principal);
} 