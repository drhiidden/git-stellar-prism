package com.drhdn.ghvis.infrastructure.adapter.outbound.eventsourcing;

import com.drhdn.ghvis.domain.event.DomainEvent;
import com.drhdn.ghvis.domain.event.EventStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del EventStore para entornos de desarrollo.
 * Almacena eventos en memoria y proporciona métodos para consultarlos.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@Profile({"dev", "test"})
@Slf4j
public class InMemoryEventStore implements EventStore {
    
    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();
    private final Sinks.Many<DomainEvent> eventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Map<String, Long> eventTypeCount = new ConcurrentHashMap<>();
    private final Map<String, Long> aggregateEventCount = new ConcurrentHashMap<>();
    
    @Override
    public Mono<Void> store(DomainEvent event) {
        return Mono.fromRunnable(() -> {
            events.add(event);
            updateStats(event);
            eventSink.tryEmitNext(event);
            log.debug("📝 Evento almacenado: {} ({})", event.getEventType(), event.getEventId());
        });
    }
    
    @Override
    public Mono<Void> storeAll(Flux<DomainEvent> eventsFlux) {
        return eventsFlux.flatMap(this::store).then();
    }
    
    @Override
    public Flux<DomainEvent> getByType(String eventType) {
        return Flux.fromIterable(events)
            .filter(event -> event.getEventType().equals(eventType))
            .doOnSubscribe(s -> log.debug("🔍 Consultando eventos por tipo: {}", eventType));
    }
    
    @Override
    public Flux<DomainEvent> getByAggregateId(String aggregateId) {
        return Flux.fromIterable(events)
            .filter(event -> event.getAggregateId().equals(aggregateId))
            .doOnSubscribe(s -> log.debug("🔍 Consultando eventos por agregado: {}", aggregateId));
    }
    
    @Override
    public Flux<DomainEvent> getByTypeAndAggregateId(String eventType, String aggregateId) {
        return Flux.fromIterable(events)
            .filter(event -> event.getEventType().equals(eventType) && 
                           event.getAggregateId().equals(aggregateId))
            .doOnSubscribe(s -> log.debug("🔍 Consultando eventos por tipo y agregado: {} / {}", 
                                        eventType, aggregateId));
    }
    
    @Override
    public Flux<DomainEvent> getByTimeRange(Instant start, Instant end) {
        return Flux.fromIterable(events)
            .filter(event -> !event.getTimestamp().isBefore(start) && 
                           !event.getTimestamp().isAfter(end))
            .doOnSubscribe(s -> log.debug("🔍 Consultando eventos por rango de tiempo: {} a {}", 
                                        start, end));
    }
    
    @Override
    public Mono<Map<String, Object>> getStats() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new ConcurrentHashMap<>();
            stats.put("totalEvents", events.size());
            stats.put("eventTypeDistribution", new HashMap<>(eventTypeCount));
            stats.put("aggregateEventDistribution", new HashMap<>(aggregateEventCount));
            
            // Estadísticas de eventos por hora en las últimas 24 horas
            Instant now = Instant.now();
            Instant oneDayAgo = now.minusSeconds(86400); // 24 horas
            
            Map<String, Long> hourlyStats = events.stream()
                .filter(event -> event.getTimestamp().isAfter(oneDayAgo))
                .collect(Collectors.groupingBy(
                    event -> event.getTimestamp().toString().substring(0, 13), // YYYY-MM-DDTHH
                    Collectors.counting()
                ));
            
            stats.put("hourlyDistribution", hourlyStats);
            
            return stats;
        });
    }
    
    /**
     * Obtiene un flujo de eventos en tiempo real.
     * 
     * @return Flux de eventos
     */
    public Flux<DomainEvent> getEventStream() {
        return eventSink.asFlux();
    }
    
    /**
     * Actualiza las estadísticas de eventos.
     * 
     * @param event Evento a contabilizar
     */
    private void updateStats(DomainEvent event) {
        // Actualizar conteo por tipo de evento
        eventTypeCount.compute(event.getEventType(), (k, v) -> (v == null) ? 1L : v + 1L);
        
        // Actualizar conteo por agregado
        String aggregateKey = event.getAggregateType() + ":" + event.getAggregateId();
        aggregateEventCount.compute(aggregateKey, (k, v) -> (v == null) ? 1L : v + 1L);
    }
    
    /**
     * Limpia todos los eventos (solo para pruebas).
     * 
     * @return Mono completado cuando se limpian los eventos
     */
    public Mono<Void> clear() {
        return Mono.fromRunnable(() -> {
            events.clear();
            eventTypeCount.clear();
            aggregateEventCount.clear();
            log.info("🧹 Eventos limpiados del almacén en memoria");
        });
    }
} 