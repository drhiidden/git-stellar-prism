package com.drhdn.ghvis.domain.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Puerto para almacenamiento de eventos en Event Sourcing.
 * Define el contrato para persistir y recuperar eventos de dominio.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface EventStore {
    
    /**
     * Almacena un evento de dominio.
     * 
     * @param event Evento a almacenar
     * @return Mono completado cuando el evento es almacenado
     */
    Mono<Void> store(DomainEvent event);
    
    /**
     * Almacena múltiples eventos de dominio.
     * 
     * @param events Eventos a almacenar
     * @return Mono completado cuando todos los eventos son almacenados
     */
    Mono<Void> storeAll(Flux<DomainEvent> events);
    
    /**
     * Obtiene eventos por tipo.
     * 
     * @param eventType Tipo de evento
     * @return Flux de eventos del tipo especificado
     */
    Flux<DomainEvent> getByType(String eventType);
    
    /**
     * Obtiene eventos por agregado.
     * 
     * @param aggregateId ID del agregado
     * @return Flux de eventos del agregado
     */
    Flux<DomainEvent> getByAggregateId(String aggregateId);
    
    /**
     * Obtiene eventos por tipo y agregado.
     * 
     * @param eventType Tipo de evento
     * @param aggregateId ID del agregado
     * @return Flux de eventos del tipo y agregado especificados
     */
    Flux<DomainEvent> getByTypeAndAggregateId(String eventType, String aggregateId);
    
    /**
     * Obtiene eventos en un rango de tiempo.
     * 
     * @param start Inicio del rango
     * @param end Fin del rango
     * @return Flux de eventos en el rango especificado
     */
    Flux<DomainEvent> getByTimeRange(Instant start, Instant end);
    
    /**
     * Obtiene estadísticas del almacén de eventos.
     * 
     * @return Mono con estadísticas
     */
    Mono<Map<String, Object>> getStats();
} 