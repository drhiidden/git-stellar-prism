package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.domain.event.*;
import com.drhdn.ghvis.domain.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador para emitir eventos en tiempo real mediante Server-Sent Events (SSE).
 * 
 * Integra con la Event-Driven Architecture para proporcionar eventos
 * en tiempo real sobre análisis de repositorios.
 * 
 * @author GitStellarPrism Team
 * @version 2.0.0
 */
@RestController
@ConditionalOnProperty(name = "app.realtime.enabled", havingValue = "true")
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Slf4j
public class StreamController {

    private final EventPublisher eventPublisher;
    
    // Sink para eventos en tiempo real
    private final Sinks.Many<Map<String, Object>> eventSink = Sinks.many().multicast().onBackpressureBuffer();

    /**
     * Endpoint SSE que envía eventos en tiempo real para un repositorio específico.
     * Incluye eventos de conexión inicial y heartbeats informativos.
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> streamEvents(@RequestParam("repo") String repoParam) {
        log.info("🔗 Nueva conexión SSE para repositorio: {}", repoParam);
        
        // Evento de conexión inicial
        Map<String, Object> connectionEvent = Map.of(
            "id", UUID.randomUUID().toString(),
            "type", "CONNECTION_ESTABLISHED",
            "timestamp", java.time.Instant.now().toString(),
            "repository", repoParam,
            "message", "Conexión establecida para: " + repoParam
        );
        
        log.info("✅ Enviando evento de conexión inicial para: {}", repoParam);
        
        Flux<ServerSentEvent<Map<String, Object>>> initialEvent = Flux.just(
            ServerSentEvent.<Map<String, Object>>builder()
                .id(connectionEvent.get("id").toString())
                .event("connection")
                .data(connectionEvent)
                .build()
        );
        
        // Flujo de eventos del sink
        Flux<ServerSentEvent<Map<String, Object>>> eventFlux = eventSink.asFlux()
            .map(event -> ServerSentEvent.<Map<String, Object>>builder()
                .id(UUID.randomUUID().toString())
                .event("domain-event")
                .data(event)
                .build());

        // Heartbeat cada 30s con información del repositorio
        Flux<ServerSentEvent<Map<String, Object>>> heartbeat = Flux.interval(Duration.ofSeconds(30))
            .map(seq -> {
                log.debug("💓 Enviando heartbeat #{} para: {}", seq, repoParam);
                Map<String, Object> heartbeatEvent = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "type", "HEARTBEAT",
                    "timestamp", java.time.Instant.now().toString(),
                    "repository", repoParam,
                    "message", "Conexión activa",
                    "sequence", seq
                );
                return ServerSentEvent.<Map<String, Object>>builder()
                    .id(heartbeatEvent.get("id").toString())
                    .event("heartbeat")
                    .data(heartbeatEvent)
                    .build();
            });

        // Evento de prueba cada 60s para verificar funcionamiento
        Flux<ServerSentEvent<Map<String, Object>>> testEvent = Flux.interval(Duration.ofSeconds(60))
            .map(seq -> {
                log.debug("🧪 Enviando evento de prueba #{} para: {}", seq, repoParam);
                Map<String, Object> test = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "type", "TEST_EVENT",
                    "timestamp", java.time.Instant.now().toString(),
                    "repository", repoParam,
                    "message", "Evento de prueba #" + seq,
                    "sequence", seq
                );
                return ServerSentEvent.<Map<String, Object>>builder()
                    .id(test.get("id").toString())
                    .event("test")
                    .data(test)
                    .build();
            });

        return Flux.merge(initialEvent, eventFlux, heartbeat, testEvent)
            .doOnCancel(() -> log.info("🔌 Conexión SSE cancelada para: {}", repoParam))
            .doOnError(error -> log.error("❌ Error en SSE para {}: {}", repoParam, error.getMessage()));
    }
    
    /**
     * Endpoint para enviar eventos de prueba al stream.
     * 
     * @param eventType Tipo de evento
     * @param repository Repositorio
     * @param message Mensaje del evento
     * @return Confirmación de envío
     */
    @PostMapping("/events/test")
    public Flux<String> sendTestEvent(@RequestParam String eventType, 
                                    @RequestParam String repository, 
                                    @RequestParam String message) {
        Map<String, Object> testEvent = Map.of(
            "id", UUID.randomUUID().toString(),
            "type", eventType,
            "timestamp", java.time.Instant.now().toString(),
            "repository", repository,
            "message", message
        );
        
        eventSink.tryEmitNext(testEvent);
        log.info("📤 Evento de prueba enviado: {} - {}", eventType, message);
        
        return Flux.just("Evento enviado exitosamente");
    }
    
    /**
     * Endpoint para obtener estadísticas del stream.
     * 
     * @return Estadísticas del stream
     */
    @GetMapping("/stats")
    public Flux<Map<String, Object>> getStreamStats() {
        Map<String, Object> stats = Map.of(
            "active_connections", "N/A", // TODO: Implementar contador
            "events_sent", "N/A", // TODO: Implementar contador
            "timestamp", java.time.Instant.now().toString(),
            "status", "ACTIVE"
        );
        
        return Flux.just(stats);
    }
} 