package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.domain.event.DomainEvent;
import com.drhdn.ghvis.domain.event.EventStore;
import com.drhdn.ghvis.infrastructure.adapter.outbound.eventsourcing.InMemoryEventStore;
import com.drhdn.ghvis.infrastructure.adapter.outbound.eventsourcing.MongoEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Controlador REST para acceso al EventStore.
 * Proporciona endpoints para consultar eventos y estadísticas.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventStoreController {
    
    private final EventStore eventStore;
    
    /**
     * Obtiene estadísticas del EventStore.
     * 
     * @return Estadísticas del EventStore
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getStats() {
        log.info("📊 Solicitando estadísticas del EventStore");
        return eventStore.getStats()
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Estadísticas del EventStore enviadas"))
            .doOnError(error -> log.error("❌ Error obteniendo estadísticas: {}", error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().body(Map.of(
                "error", "Error obteniendo estadísticas",
                "message", error.getMessage()
            ))));
    }
    
    /**
     * Obtiene eventos por tipo.
     * 
     * @param type Tipo de evento
     * @return Lista de eventos del tipo especificado
     */
    @GetMapping("/type/{type}")
    public Flux<DomainEvent> getByType(@PathVariable String type) {
        log.info("🔍 Solicitando eventos por tipo: {}", type);
        return eventStore.getByType(type)
            .doOnComplete(() -> log.info("✅ Eventos por tipo enviados: {}", type))
            .doOnError(error -> log.error("❌ Error obteniendo eventos por tipo: {}", error.getMessage()));
    }
    
    /**
     * Obtiene eventos por agregado.
     * 
     * @param aggregateId ID del agregado
     * @return Lista de eventos del agregado
     */
    @GetMapping("/aggregate/{aggregateId}")
    public Flux<DomainEvent> getByAggregateId(@PathVariable String aggregateId) {
        log.info("🔍 Solicitando eventos por agregado: {}", aggregateId);
        return eventStore.getByAggregateId(aggregateId)
            .doOnComplete(() -> log.info("✅ Eventos por agregado enviados: {}", aggregateId))
            .doOnError(error -> log.error("❌ Error obteniendo eventos por agregado: {}", error.getMessage()));
    }
    
    /**
     * Obtiene eventos por tipo y agregado.
     * 
     * @param type Tipo de evento
     * @param aggregateId ID del agregado
     * @return Lista de eventos del tipo y agregado especificados
     */
    @GetMapping("/type/{type}/aggregate/{aggregateId}")
    public Flux<DomainEvent> getByTypeAndAggregateId(
            @PathVariable String type, 
            @PathVariable String aggregateId) {
        log.info("🔍 Solicitando eventos por tipo y agregado: {} / {}", type, aggregateId);
        return eventStore.getByTypeAndAggregateId(type, aggregateId)
            .doOnComplete(() -> log.info("✅ Eventos por tipo y agregado enviados: {} / {}", type, aggregateId))
            .doOnError(error -> log.error("❌ Error obteniendo eventos por tipo y agregado: {}", error.getMessage()));
    }
    
    /**
     * Obtiene eventos por rango de tiempo.
     * 
     * @param start Inicio del rango
     * @param end Fin del rango
     * @return Lista de eventos en el rango especificado
     */
    @GetMapping("/timerange")
    public Flux<DomainEvent> getByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Instant startInstant = start.toInstant(ZoneOffset.UTC);
        Instant endInstant = end.toInstant(ZoneOffset.UTC);
        log.info("🔍 Solicitando eventos por rango de tiempo: {} a {}", startInstant, endInstant);
        return eventStore.getByTimeRange(startInstant, endInstant)
            .doOnComplete(() -> log.info("✅ Eventos por rango de tiempo enviados"))
            .doOnError(error -> log.error("❌ Error obteniendo eventos por rango de tiempo: {}", error.getMessage()));
    }
    
    /**
     * Obtiene un flujo de eventos en tiempo real.
     * 
     * @return Flujo SSE de eventos
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<DomainEvent>> streamEvents() {
        log.info("🔌 Cliente conectado al stream de eventos");
        
        // Asumimos que la implementación del EventStore tiene un método getEventStream
        // Si no lo tiene, deberíamos adaptarlo
        if (eventStore instanceof InMemoryEventStore) {
            return ((InMemoryEventStore) eventStore).getEventStream()
                .map(event -> ServerSentEvent.<DomainEvent>builder()
                    .id(event.getEventId())
                    .event(event.getEventType())
                    .data(event)
                    .build())
                .doOnCancel(() -> log.info("🔌 Cliente desconectado del stream de eventos"));
        } else if (eventStore instanceof MongoEventStore) {
            return ((MongoEventStore) eventStore).getEventStream()
                .map(event -> ServerSentEvent.<DomainEvent>builder()
                    .id(event.getEventId())
                    .event(event.getEventType())
                    .data(event)
                    .build())
                .doOnCancel(() -> log.info("🔌 Cliente desconectado del stream de eventos"));
        } else {
            // Fallback: enviar un heartbeat cada 10 segundos
            return Flux.interval(Duration.ofSeconds(10))
                .map(seq -> ServerSentEvent.<DomainEvent>builder()
                    .id(String.valueOf(seq))
                    .event("heartbeat")
                    .data(null)
                    .build())
                .doOnSubscribe(s -> log.warn("⚠️ Stream de eventos no disponible, enviando solo heartbeats"));
        }
    }
} 