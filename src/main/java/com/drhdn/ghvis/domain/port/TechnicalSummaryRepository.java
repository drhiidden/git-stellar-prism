package com.drhdn.ghvis.domain.port;

import com.drhdn.ghvis.domain.entity.TechnicalSummary;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Puerto de salida para operaciones con Technical Summary.
 * 
 * Define el contrato para generar resúmenes técnicos de repositorios,
 * incluyendo análisis de tecnologías, lenguajes y complejidad.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface TechnicalSummaryRepository {
    
    /**
     * Genera un resumen técnico para un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con el resumen técnico
     */
    Mono<TechnicalSummary> generateForRepository(String owner, String repo, Principal principal);
    
    /**
     * Genera un resumen técnico con tipo específico.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param summaryType Tipo de resumen ("basic", "detailed", "comprehensive")
     * @param principal Usuario autenticado
     * @return Mono con el resumen técnico
     */
    Mono<TechnicalSummary> generateForRepositoryWithType(String owner, String repo, 
                                                       String summaryType, Principal principal);
} 