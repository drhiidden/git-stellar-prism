package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Clase base abstracta para todos los eventos de dominio.
 * Implementa la interfaz DomainEvent con funcionalidad común.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractDomainEvent implements DomainEvent {
    
    private String eventId;
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private long version;
    private Instant timestamp;
    private String user;
    private Map<String, Object> data;
    
    /**
     * Constructor con inicialización de campos comunes.
     * 
     * @param eventType Tipo de evento
     * @param aggregateId ID del agregado
     * @param aggregateType Tipo de agregado
     * @param version Versión del agregado
     * @param user Usuario que genera el evento
     */
    protected AbstractDomainEvent(String eventType, String aggregateId, String aggregateType, 
                                long version, String user) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.version = version;
        this.timestamp = Instant.now();
        this.user = user;
        this.data = new HashMap<>();
    }
    
    /**
     * Añade un dato al evento.
     * 
     * @param key Clave del dato
     * @param value Valor del dato
     */
    public void addData(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }
    
    /**
     * Obtiene un dato específico del evento.
     * 
     * @param key Clave del dato
     * @return Valor del dato
     */
    public Object getDataValue(String key) {
        return data != null ? data.get(key) : null;
    }
    
    /**
     * Obtiene un dato específico del evento con tipo.
     * 
     * @param key Clave del dato
     * @param clazz Clase del tipo de retorno
     * @return Valor del dato con el tipo especificado
     */
    public <T> T getDataValue(String key, Class<T> clazz) {
        Object value = getDataValue(key);
        return value != null && clazz.isInstance(value) ? clazz.cast(value) : null;
    }
} 