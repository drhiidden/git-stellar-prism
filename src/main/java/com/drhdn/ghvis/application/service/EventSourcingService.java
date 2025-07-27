package com.drhdn.ghvis.application.service;

import com.drhdn.ghvis.domain.event.DomainEvent;
import com.drhdn.ghvis.domain.event.EventSourcedAggregate;
import com.drhdn.ghvis.domain.event.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

/**
 * Servicio para gestionar agregados basados en Event Sourcing.
 * Proporciona métodos para cargar, guardar y reconstruir agregados.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventSourcingService {
    
    private final EventStore eventStore;
    
    /**
     * Carga un agregado a partir de su ID y tipo.
     * 
     * @param aggregateId ID del agregado
     * @param aggregateType Tipo del agregado
     * @param aggregateFactory Función para crear una instancia vacía del agregado
     * @return Mono con el agregado reconstruido
     */
    public <T extends EventSourcedAggregate> Mono<T> loadAggregate(
            String aggregateId, 
            String aggregateType,
            Function<String, T> aggregateFactory) {
        
        log.debug("🔄 Cargando agregado: {} ({})", aggregateType, aggregateId);
        
        // Crear instancia vacía del agregado
        T aggregate = aggregateFactory.apply(aggregateId);
        
        // Obtener eventos del agregado
        Flux<DomainEvent> events = eventStore.getByAggregateId(aggregateId)
            .filter(event -> event.getAggregateType().equals(aggregateType))
            .sort((e1, e2) -> Long.compare(e1.getVersion(), e2.getVersion()));
        
        // Reconstruir el agregado aplicando los eventos
        return aggregate.replay(events)
            .doOnSuccess(loaded -> log.debug("✅ Agregado cargado: {} ({}) - Versión: {}", 
                                           aggregateType, aggregateId, loaded.getVersion()))
            .doOnError(error -> log.error("❌ Error cargando agregado: {} ({}) - {}", 
                                        aggregateType, aggregateId, error.getMessage()));
    }
    
    /**
     * Guarda los eventos generados por un agregado.
     * 
     * @param aggregate Agregado que generó los eventos
     * @param events Lista de eventos a guardar
     * @return Mono completado cuando los eventos son guardados
     */
    public Mono<Void> saveEvents(EventSourcedAggregate aggregate, List<DomainEvent> events) {
        if (events.isEmpty()) {
            log.debug("ℹ️ No hay eventos para guardar para el agregado: {} ({})",
                     aggregate.getAggregateType(), aggregate.getAggregateId());
            return Mono.empty();
        }
        
        log.debug("💾 Guardando {} eventos para el agregado: {} ({})",
                events.size(), aggregate.getAggregateType(), aggregate.getAggregateId());
        
        return eventStore.storeAll(Flux.fromIterable(events))
            .doOnSuccess(v -> log.debug("✅ Eventos guardados para el agregado: {} ({})",
                                      aggregate.getAggregateType(), aggregate.getAggregateId()))
            .doOnError(error -> log.error("❌ Error guardando eventos para el agregado: {} ({}) - {}",
                                        aggregate.getAggregateType(), aggregate.getAggregateId(), error.getMessage()));
    }
    
    /**
     * Guarda un evento generado por un agregado.
     * 
     * @param event Evento a guardar
     * @return Mono completado cuando el evento es guardado
     */
    public Mono<Void> saveEvent(DomainEvent event) {
        log.debug("💾 Guardando evento: {} para el agregado: {} ({})",
                event.getEventType(), event.getAggregateType(), event.getAggregateId());
        
        return eventStore.store(event)
            .doOnSuccess(v -> log.debug("✅ Evento guardado: {} para el agregado: {} ({})",
                                      event.getEventType(), event.getAggregateType(), event.getAggregateId()))
            .doOnError(error -> log.error("❌ Error guardando evento: {} para el agregado: {} ({}) - {}",
                                        event.getEventType(), event.getAggregateType(), event.getAggregateId(), 
                                        error.getMessage()));
    }
} 