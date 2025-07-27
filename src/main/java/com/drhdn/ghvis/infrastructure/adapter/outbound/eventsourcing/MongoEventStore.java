package com.drhdn.ghvis.infrastructure.adapter.outbound.eventsourcing;

import com.drhdn.ghvis.domain.event.DomainEvent;
import com.drhdn.ghvis.domain.event.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación MongoDB del EventStore para entornos de producción.
 * Almacena eventos en MongoDB y proporciona métodos para consultarlos.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class MongoEventStore implements EventStore {
    
    private final ReactiveMongoTemplate mongoTemplate;
    private final Sinks.Many<DomainEvent> eventSink = Sinks.many().multicast().onBackpressureBuffer();
    
    @Override
    public Mono<Void> store(DomainEvent event) {
        return mongoTemplate.save(event, "events")
            .doOnSuccess(savedEvent -> {
                eventSink.tryEmitNext(event);
                log.debug("📝 Evento almacenado en MongoDB: {} ({})", event.getEventType(), event.getEventId());
            })
            .doOnError(error -> log.error("❌ Error almacenando evento en MongoDB: {}", error.getMessage()))
            .then();
    }
    
    @Override
    public Mono<Void> storeAll(Flux<DomainEvent> events) {
        return events.flatMap(this::store).then();
    }
    
    @Override
    public Flux<DomainEvent> getByType(String eventType) {
        Query query = new Query(Criteria.where("eventType").is(eventType));
        return mongoTemplate.find(query, DomainEvent.class, "events")
            .doOnSubscribe(s -> log.debug("🔍 Consultando eventos por tipo desde MongoDB: {}", eventType));
    }
    
    @Override
    public Flux<DomainEvent> getByAggregateId(String aggregateId) {
        Query query = new Query(Criteria.where("aggregateId").is(aggregateId));
        return mongoTemplate.find(query, DomainEvent.class, "events")
            .doOnSubscribe(s -> log.debug("🔍 Consultando eventos por agregado desde MongoDB: {}", aggregateId));
    }
    
    @Override
    public Flux<DomainEvent> getByTypeAndAggregateId(String eventType, String aggregateId) {
        Query query = new Query(Criteria.where("eventType").is(eventType)
                              .and("aggregateId").is(aggregateId));
        return mongoTemplate.find(query, DomainEvent.class, "events")
            .doOnSubscribe(s -> log.debug("🔍 Consultando eventos por tipo y agregado desde MongoDB: {} / {}", 
                                        eventType, aggregateId));
    }
    
    @Override
    public Flux<DomainEvent> getByTimeRange(Instant start, Instant end) {
        Query query = new Query(Criteria.where("timestamp").gte(start).lte(end));
        return mongoTemplate.find(query, DomainEvent.class, "events")
            .doOnSubscribe(s -> log.debug("🔍 Consultando eventos por rango de tiempo desde MongoDB: {} a {}", 
                                        start, end));
    }
    
    @Override
    public Mono<Map<String, Object>> getStats() {
        // Obtener estadísticas básicas
        Mono<Long> totalCount = mongoTemplate.count(new Query(), "events");
        
        // TODO: Implementar agregaciones para estadísticas más detalladas
        // Esto requeriría usar operaciones de agregación de MongoDB
        
        return totalCount.map(count -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalEvents", count);
            stats.put("database", "MongoDB");
            stats.put("collection", "events");
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
} 