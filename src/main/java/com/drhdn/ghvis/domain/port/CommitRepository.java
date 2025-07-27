package com.drhdn.ghvis.domain.port;

import com.drhdn.ghvis.domain.entity.Commit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Puerto de salida para acceso a datos de commits.
 * 
 * Define el contrato para obtener commits de repositorios,
 * siguiendo los principios de arquitectura hexagonal.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface CommitRepository {
    
    /**
     * Obtiene todos los commits de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Flux de commits del repositorio
     */
    Flux<Commit> findByRepository(String owner, String repo, Principal principal);
    
    /**
     * Obtiene un commit específico por su SHA.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param sha SHA del commit
     * @param principal Usuario autenticado
     * @return Mono con el commit detallado
     */
    Mono<Commit> findBySha(String owner, String repo, String sha, Principal principal);
    
    /**
     * Verifica si el usuario tiene acceso al repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con true si tiene acceso
     */
    Mono<Boolean> hasAccess(String owner, String repo, Principal principal);
} 