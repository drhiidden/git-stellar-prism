package com.drhdn.ghvis.domain.event;

import java.time.Instant;
import java.util.Map;

/**
 * Interfaz base para todos los eventos de dominio.
 * Define la estructura común que deben implementar todos los eventos.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface DomainEvent {
    
    /**
     * Obtiene el ID único del evento.
     * 
     * @return ID del evento
     */
    String getEventId();
    
    /**
     * Obtiene el tipo de evento.
     * 
     * @return Tipo de evento
     */
    String getEventType();
    
    /**
     * Obtiene el ID del agregado al que pertenece el evento.
     * 
     * @return ID del agregado
     */
    String getAggregateId();
    
    /**
     * Obtiene el tipo de agregado al que pertenece el evento.
     * 
     * @return Tipo de agregado
     */
    String getAggregateType();
    
    /**
     * Obtiene la versión del agregado después de aplicar el evento.
     * 
     * @return Versión del agregado
     */
    long getVersion();
    
    /**
     * Obtiene el timestamp de ocurrencia del evento.
     * 
     * @return Timestamp del evento
     */
    Instant getTimestamp();
    
    /**
     * Obtiene el usuario que generó el evento.
     * 
     * @return Usuario generador
     */
    String getUser();
    
    /**
     * Obtiene los datos específicos del evento.
     * 
     * @return Datos del evento
     */
    Map<String, Object> getData();
} 