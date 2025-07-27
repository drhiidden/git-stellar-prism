package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * Evento de dominio que se dispara cuando se excede el rate limit.
 * 
 * Este evento permite que otros componentes del sistema reaccionen
 * cuando se alcanza el límite de requests a la API externa.
 */
@Getter
@RequiredArgsConstructor
public class RateLimitExceededEvent {
    private final String endpoint;
    private final String username;
    private final long waitSeconds;
    private final Instant timestamp;
    
    public RateLimitExceededEvent(String endpoint, String username, long waitSeconds) {
        this.endpoint = endpoint;
        this.username = username;
        this.waitSeconds = waitSeconds;
        this.timestamp = Instant.now();
    }
    
    /**
     * Obtiene el tipo de evento para identificación.
     */
    public String getEventType() {
        return "RATE_LIMIT_EXCEEDED";
    }
    
    /**
     * Obtiene el tiempo de espera en formato legible.
     */
    public String getWaitTimeFormatted() {
        if (waitSeconds < 60) {
            return waitSeconds + " segundos";
        } else if (waitSeconds < 3600) {
            return (waitSeconds / 60) + " minutos";
        } else {
            return (waitSeconds / 3600) + " horas";
        }
    }
} 