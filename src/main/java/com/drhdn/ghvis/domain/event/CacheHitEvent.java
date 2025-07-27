package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * Evento de dominio que se dispara cuando hay un hit en el cache.
 * 
 * Este evento permite que otros componentes del sistema reaccionen
 * cuando se obtiene un valor del cache en lugar de hacer una llamada
 * a la API externa.
 */
@Getter
@RequiredArgsConstructor
public class CacheHitEvent {
    private final String cacheKey;
    private final String repositoryName;
    private final String username;
    private final Instant timestamp;
    
    public CacheHitEvent(String cacheKey, String repositoryName, String username) {
        this.cacheKey = cacheKey;
        this.repositoryName = repositoryName;
        this.username = username;
        this.timestamp = Instant.now();
    }
    
    /**
     * Obtiene el tipo de evento para identificación.
     */
    public String getEventType() {
        return "CACHE_HIT";
    }
} 