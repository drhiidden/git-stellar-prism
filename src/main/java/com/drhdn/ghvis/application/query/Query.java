package com.drhdn.ghvis.application.query;

/**
 * Interfaz marcadora para todas las consultas del sistema.
 * 
 * Las queries representan intenciones de LEER datos del sistema:
 * - Obtener datos sin modificarlos
 * - Filtrar y buscar información
 * - Generar reportes
 * 
 * Características de las Queries:
 * - Son inmutables (records en Java)
 * - NO modifican el estado del sistema
 * - Pueden ser cacheadas agresivamente
 * - Pueden ser optimizadas independientemente
 * - Retornan datos de negocio
 * 
 * Patrón CQRS (Command Query Responsibility Segregation)
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface Query<R> {
    
    /**
     * Obtiene el nombre de la query para logging y métricas.
     * Por defecto usa el nombre de la clase.
     */
    default String getQueryName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Indica si esta query puede ser cacheada.
     * Por defecto, todas las queries son cacheables.
     */
    default boolean isCacheable() {
        return true;
    }
    
    /**
     * Obtiene la clave de caché para esta query.
     * Por defecto usa el nombre de la query + hashCode.
     */
    default String getCacheKey() {
        return getQueryName() + ":" + this.hashCode();
    }
}

