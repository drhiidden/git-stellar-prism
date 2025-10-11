package com.drhdn.ghvis.application.query;

import reactor.core.publisher.Mono;

/**
 * Interfaz genérica para manejadores de consultas.
 * 
 * Un QueryHandler:
 * - Recibe una query
 * - Lee datos (sin modificarlos)
 * - Puede usar caché agresivamente
 * - Optimizado para performance
 * - Retorna datos solicitados
 * 
 * @param <Q> Tipo de la query
 * @param <R> Tipo del resultado
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface QueryHandler<Q extends Query<R>, R> {
    
    /**
     * Maneja la ejecución de la query.
     * 
     * @param query Query a ejecutar
     * @return Resultado de la consulta
     */
    Mono<R> handle(Q query);
}

