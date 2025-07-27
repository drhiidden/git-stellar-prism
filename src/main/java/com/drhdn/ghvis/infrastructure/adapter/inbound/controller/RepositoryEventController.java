package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.usecase.GetRepositoryWithEventSourcingUseCase;
import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.event.DomainEvent;
import com.drhdn.ghvis.domain.event.EventStore;
import com.drhdn.ghvis.infrastructure.adapter.outbound.eventsourcing.InMemoryEventStore;
import com.drhdn.ghvis.infrastructure.adapter.outbound.eventsourcing.MongoEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;

/**
 * Controlador REST para acceso a eventos de repositorios.
 * Proporciona endpoints para consultar eventos y reconstruir repositorios.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
@Slf4j
public class RepositoryEventController {
    
    private final EventStore eventStore;
    private final GetRepositoryWithEventSourcingUseCase getRepositoryWithEventSourcingUseCase;
    
    /**
     * Obtiene eventos de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Lista de eventos del repositorio
     */
    @GetMapping("/{owner}/{repo}/events")
    public Flux<DomainEvent> getRepositoryEvents(@PathVariable String owner, @PathVariable String repo) {
        String aggregateId = owner + "/" + repo;
        log.info("🔍 Solicitando eventos para repositorio: {}", aggregateId);
        
        return eventStore.getByAggregateId(aggregateId)
            .doOnComplete(() -> log.info("✅ Eventos enviados para repositorio: {}", aggregateId))
            .doOnError(error -> log.error("❌ Error obteniendo eventos para repositorio: {} - {}", 
                                        aggregateId, error.getMessage()));
    }
    
    /**
     * Obtiene un repositorio reconstruido a partir de eventos.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Repositorio reconstruido
     */
    @GetMapping("/{owner}/{repo}/event-sourced")
    public Mono<ResponseEntity<Repository>> getEventSourcedRepository(
            @PathVariable String owner, 
            @PathVariable String repo,
            Principal principal) {
        
        log.info("🔍 Solicitando repositorio reconstruido a partir de eventos: {}/{}", owner, repo);
        
        return getRepositoryWithEventSourcingUseCase.execute(owner, repo, principal)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Repositorio reconstruido enviado: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error reconstruyendo repositorio: {}/{} - {}", 
                                        owner, repo, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().build()));
    }
    
    /**
     * Obtiene un flujo de eventos de un repositorio en tiempo real.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Flujo SSE de eventos del repositorio
     */
    @GetMapping(value = "/{owner}/{repo}/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<DomainEvent>> streamRepositoryEvents(
            @PathVariable String owner, 
            @PathVariable String repo) {
        
        String aggregateId = owner + "/" + repo;
        log.info("🔌 Cliente conectado al stream de eventos para repositorio: {}", aggregateId);
        
        // Obtener eventos históricos
        Flux<DomainEvent> historicalEvents = eventStore.getByAggregateId(aggregateId);
        
        // Obtener eventos en tiempo real usando pattern matching
        Flux<DomainEvent> liveEvents = getLiveEventsStream(aggregateId);
        
        // Combinar eventos históricos y en tiempo real
        return Flux.concat(historicalEvents, liveEvents)
            .map(event -> ServerSentEvent.<DomainEvent>builder()
                .id(event.getEventId())
                .event(event.getEventType())
                .data(event)
                .build())
            .doOnCancel(() -> log.info("🔌 Cliente desconectado del stream de eventos para repositorio: {}", aggregateId));
    }

    private Flux<DomainEvent> getLiveEventsStream(String aggregateId) {
        if (eventStore instanceof InMemoryEventStore inMemoryStore) {
            return inMemoryStore.getEventStream()
                .filter(event -> event.getAggregateId().equals(aggregateId));
        } else if (eventStore instanceof MongoEventStore mongoStore) {
            return mongoStore.getEventStream()
                .filter(event -> event.getAggregateId().equals(aggregateId));
        }
        return Flux.empty();
    }
    
    /**
     * Obtiene estadísticas de eventos de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Estadísticas de eventos del repositorio
     */
    @GetMapping("/{owner}/{repo}/events/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getRepositoryEventsStats(
            @PathVariable String owner, 
            @PathVariable String repo) {
        
        String aggregateId = owner + "/" + repo;
        log.info("📊 Solicitando estadísticas de eventos para repositorio: {}", aggregateId);
        
        // Contar eventos por tipo
        return eventStore.getByAggregateId(aggregateId)
            .groupBy(DomainEvent::getEventType)
            .flatMap(group -> group.count().map(count -> Map.entry(group.key(), count)))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .map(eventTypeCounts -> {
                // Crear mapa de estadísticas
                Map<String, Object> stats = new java.util.HashMap<>();
                stats.put("repository", aggregateId);
                stats.put("eventTypeCounts", eventTypeCounts);
                stats.put("totalEvents", eventTypeCounts.values().stream().mapToLong(Long::longValue).sum());
                stats.put("timestamp", java.time.Instant.now().toString());
                return ResponseEntity.ok(stats);
            })
            .doOnSuccess(response -> log.info("✅ Estadísticas de eventos enviadas para repositorio: {}", aggregateId))
            .doOnError(error -> log.error("❌ Error obteniendo estadísticas de eventos para repositorio: {} - {}", 
                                        aggregateId, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().body(Map.of(
                "error", "Error obteniendo estadísticas de eventos",
                "message", error.getMessage()
            ))));
    }
} 