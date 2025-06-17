package com.drhdn.ghvis.service;

import com.drhdn.ghvis.model.Event;
import lombok.Getter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Servicio que actúa como broker interno de eventos.
 */
@Service
@ConditionalOnProperty(name = "app.realtime.enabled", havingValue = "true")
public class EventPublisherService {

    private final Sinks.Many<Event> sink = Sinks.many().multicast().onBackpressureBuffer();

    /**
     * Publica un nuevo evento para los suscriptores SSE.
     */
    public void publish(Event event) {
        sink.tryEmitNext(event);
    }

    /**
     * Devuelve un Flux que emite los eventos publicados.
     */
    public Flux<Event> flux() {
        return sink.asFlux();
    }
} 