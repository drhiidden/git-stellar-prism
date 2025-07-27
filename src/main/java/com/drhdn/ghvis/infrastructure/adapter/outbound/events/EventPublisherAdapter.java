package com.drhdn.ghvis.infrastructure.adapter.outbound.events;

import com.drhdn.ghvis.domain.entity.Event;
import lombok.Getter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Adapter de infraestructura para publicación de eventos.
 * 
 * Implementa el patrón Publisher para eventos de GitHub,
 * permitiendo la comunicación en tiempo real mediante SSE.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@ConditionalOnProperty(name = "app.realtime.enabled", havingValue = "true")
public class EventPublisherAdapter {

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