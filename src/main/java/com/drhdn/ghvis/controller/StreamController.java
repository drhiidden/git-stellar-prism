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
     * Endpoint SSE básico que envía un latido cada 30s y, de momento, datos simulados.
     * Se deberá conectar a un broker o a lógica reactiva real más adelante.
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Event>> streamEvents(@RequestParam("repo") String repoParam) {
        Flux<ServerSentEvent<Event>> eventFlux = eventPublisherService.flux()
                .map(event -> ServerSentEvent.<Event>builder()
                        .id(UUID.randomUUID().toString())
                        .event(event.getType())
                        .data(event)
                        .build());

        // Heartbeat cada 15s
        Flux<ServerSentEvent<Event>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(seq -> ServerSentEvent.<Event>builder()
                        .comment("heartbeat")
                        .build());

        return Flux.merge(eventFlux, heartbeat);
    }
} 