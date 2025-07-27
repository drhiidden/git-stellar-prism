package com.drhdn.ghvis.domain.port;

import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Puerto de salida para servicios de cache.
 * 
 * Define el contrato para operaciones de cache,
 * siguiendo los principios de arquitectura hexagonal.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface CacheService {
    
    /**
     * Obtiene un valor del cache o lo obtiene de la fuente si no existe.
     * 
     * @param key Clave del cache
     * @param fetchFunction Función para obtener el valor si no está en cache
     * @param <T> Tipo del valor
     * @return Mono con el valor del cache o de la fuente
     */
    <T> Mono<T> getOrFetch(String key, Supplier<Mono<T>> fetchFunction);
    
    /**
     * Obtiene un valor del cache o lo obtiene de la fuente si no existe.
     * 
     * @param key Clave del cache
     * @param fetchFunction Función para obtener el valor si no está en cache
     * @param ttlSeconds TTL en segundos
     * @param <T> Tipo del valor
     * @return Mono con el valor del cache o de la fuente
     */
    <T> Mono<T> getOrFetch(String key, Supplier<Mono<T>> fetchFunction, long ttlSeconds);
    
    /**
     * Limpia entradas del cache que coincidan con el patrón.
     * 
     * @param pattern Patrón para filtrar entradas a limpiar
     * @return Mono completado cuando se limpia el cache
     */
    Mono<Void> clear(String pattern);
    
    /**
     * Limpia todo el cache.
     * 
     * @return Mono completado cuando se limpia todo el cache
     */
    Mono<Void> clearAll();
    
    /**
     * Obtiene estadísticas del cache.
     * 
     * @return Mono con las estadísticas del cache
     */
    Mono<CacheStats> getStats();
    
    /**
     * Calienta el cache con datos específicos.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono completado cuando se calienta el cache
     */
    Mono<Void> warmUp(String owner, String repo, java.security.Principal principal);
    
    /**
     * Estadísticas del cache.
     */
    interface CacheStats {
        long getTotalEntries();
        long getHitCount();
        long getMissCount();
        double getHitRate();
        long getEvictionCount();
        long getSize();
        long getMaxSize();
    }
} 