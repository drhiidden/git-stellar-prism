package com.drhdn.ghvis.domain.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Clase base abstracta para agregados basados en Event Sourcing.
 * Implementa la interfaz EventSourcedAggregate con funcionalidad común.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractEventSourcedAggregate implements EventSourcedAggregate {
    
    protected String aggregateId;
    protected String aggregateType;
    protected long version;
    
    /**
     * Constructor con inicialización de campos comunes.
     * 
     * @param aggregateId ID del agregado
     * @param aggregateType Tipo del agregado
     */
    protected AbstractEventSourcedAggregate(String aggregateId, String aggregateType) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.version = 0;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends EventSourcedAggregate> Mono<T> replay(Flux<DomainEvent> events) {
        return events
            .reduce((EventSourcedAggregate) this, EventSourcedAggregate::apply)
            .map(aggregate -> (T) aggregate)
            .doOnSuccess(aggregate -> {
                if (aggregate instanceof AbstractEventSourcedAggregate) {
                    ((AbstractEventSourcedAggregate) aggregate).onRehydrated();
                }
            });
    }
    
    /**
     * Método llamado después de reconstruir el agregado a partir de eventos.
     * Puede ser sobreescrito por las implementaciones para realizar acciones adicionales.
     */
    protected void onRehydrated() {
        // Por defecto no hace nada
    }
    
    /**
     * Incrementa la versión del agregado.
     */
    protected void incrementVersion() {
        this.version++;
    }
    
    /**
     * Verifica si el evento pertenece a este agregado.
     * 
     * @param event Evento a verificar
     * @return true si el evento pertenece a este agregado
     */
    protected boolean belongsToAggregate(DomainEvent event) {
        return event.getAggregateId().equals(aggregateId) && 
               event.getAggregateType().equals(aggregateType);
    }
} 