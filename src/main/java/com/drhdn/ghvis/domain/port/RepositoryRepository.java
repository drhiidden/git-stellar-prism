package com.drhdn.ghvis.domain.port;

import com.drhdn.ghvis.domain.entity.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

/**
 * Puerto de salida para acceso a datos de repositorios.
 * 
 * Define el contrato para obtener información de repositorios,
 * siguiendo los principios de arquitectura hexagonal.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface RepositoryRepository {
    
    /**
     * Obtiene información de un repositorio específico.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con la información del repositorio
     */
    Mono<Repository> findByOwnerAndName(String owner, String repo, Principal principal);
    
    /**
     * Obtiene información de un repositorio público.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Mono con la información del repositorio público
     */
    Mono<Repository> findPublicByOwnerAndName(String owner, String repo);
    
    /**
     * Obtiene todos los repositorios de un usuario.
     * 
     * @param principal Usuario autenticado
     * @return Flux de repositorios del usuario
     */
    Flux<Repository> findByUser(Principal principal);
    
    /**
     * Obtiene la distribución de lenguajes de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con mapa de lenguajes y bytes
     */
    Mono<Map<String, Long>> findLanguages(String owner, String repo, Principal principal);
} 