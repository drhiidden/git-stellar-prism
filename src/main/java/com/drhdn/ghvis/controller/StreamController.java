package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.model.Event;
import com.drhdn.ghvis.service.EventPublisherService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;
import java.util.UUID;

/**
 * Controlador para emitir eventos en tiempo real mediante Server-Sent Events (SSE).
 */
@RestController
@ConditionalOnProperty(name = "app.realtime.enabled", havingValue = "true")
@RequestMapping("/api/stream")
public class StreamController {

    private final EventPublisherService eventPublisherService;

    public StreamController(EventPublisherService eventPublisherService) {
        this.eventPublisherService = eventPublisherService;
    }

    /**
     * Endpoint SSE que envía eventos en tiempo real para un repositorio específico.
     * Incluye eventos de conexión inicial y heartbeats informativos.
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Event>> streamEvents(@RequestParam("repo") String repoParam) {
        System.out.println("🔗 Nueva conexión SSE para repositorio: " + repoParam);
        System.out.println("🔗 EventPublisherService disponible: " + (eventPublisherService != null));
        
        // Evento de conexión inicial
        Event connectionEvent = Event.builder()
                .id(UUID.randomUUID().toString())
                .type("CONNECTION_ESTABLISHED")
                .timestamp(java.time.Instant.now())
                .repositoryFullName(repoParam)
                .payload(java.util.Map.of("message", "Conexión establecida para: " + repoParam))
                .build();
        
        System.out.println("✅ Enviando evento de conexión inicial para: " + repoParam);
        
        Flux<ServerSentEvent<Event>> initialEvent = Flux.just(
                ServerSentEvent.<Event>builder()
                        .id(connectionEvent.getId())
                        .event("connection")
                        .data(connectionEvent)
                        .build()
        );
        
        // Flujo de eventos del publisher
        Flux<ServerSentEvent<Event>> eventFlux = eventPublisherService.flux()
                .map(event -> ServerSentEvent.<Event>builder()
                        .id(UUID.randomUUID().toString())
                        .event(event.getType())
                        .data(event)
                        .build());

        // Heartbeat cada 10s con información del repositorio (para pruebas rápidas)
        Flux<ServerSentEvent<Event>> heartbeat = Flux.interval(Duration.ofSeconds(10))
                .map(seq -> {
                    System.out.println("💓 Enviando heartbeat #" + seq + " para: " + repoParam);
                    Event heartbeatEvent = Event.builder()
                            .id(UUID.randomUUID().toString())
                            .type("HEARTBEAT")
                            .timestamp(java.time.Instant.now())
                            .repositoryFullName(repoParam)
                            .payload(java.util.Map.of("message", "Conexión activa", "sequence", seq))
                            .build();
                    return ServerSentEvent.<Event>builder()
                            .id(heartbeatEvent.getId())
                            .event("heartbeat")
                            .data(heartbeatEvent)
                            .build();
                });

        // También enviar un evento de prueba cada 20s para verificar funcionamiento
        Flux<ServerSentEvent<Event>> testEvent = Flux.interval(Duration.ofSeconds(20))
                .map(seq -> {
                    System.out.println("🧪 Enviando evento de prueba #" + seq + " para: " + repoParam);
                    Event test = Event.builder()
                            .id(UUID.randomUUID().toString())
                            .type("TEST_EVENT")
                            .timestamp(java.time.Instant.now())
                            .repositoryFullName(repoParam)
                            .payload(java.util.Map.of("message", "Evento de prueba #" + seq, "sequence", seq))
                            .build();
                    return ServerSentEvent.<Event>builder()
                            .id(test.getId())
                            .event("test")
                            .data(test)
                            .build();
                });

        return Flux.merge(initialEvent, eventFlux, heartbeat, testEvent)
                .doOnCancel(() -> System.out.println("🔌 Conexión SSE cancelada para: " + repoParam))
                .doOnError(error -> System.err.println("❌ Error en SSE para " + repoParam + ": " + error.getMessage()));
    }
} 