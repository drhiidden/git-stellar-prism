package com.drhdn.ghvis.domain.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interfaz base para agregados que soportan Event Sourcing.
 */
public interface EventSourcedAggregate {
    
    /**
     * Aplica un evento al agregado.
     * 
     * @param event Evento a aplicar
     * @return El agregado con el evento aplicado
     */
    EventSourcedAggregate apply(DomainEvent event);
    
    /**
     * Reconstruye el estado del agregado a partir de una secuencia de eventos.
     * 
     * @param events Flujo de eventos a aplicar
     * @return Mono con el agregado reconstruido
     */
    <T extends EventSourcedAggregate> Mono<T> replay(Flux<DomainEvent> events);
    
    /**
     * Obtiene el ID del agregado.
     */
    String getAggregateId();
    
    /**
     * Obtiene el tipo del agregado.
     */
    String getAggregateType();
    
    /**
     * Obtiene la versión actual del agregado.
     */
    long getVersion();
} 