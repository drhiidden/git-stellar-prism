package com.drhdn.ghvis.domain.port;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida para publicación de eventos de dominio.
 * 
 * Define el contrato para publicar eventos de dominio de manera
 * asíncrona y desacoplada, permitiendo que otros componentes
 * reaccionen a estos eventos.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface EventPublisher {
    
    /**
     * Publica un evento de dominio.
     * 
     * @param event Evento a publicar
     * @param <T> Tipo del evento
     * @return Mono completado cuando el evento se publica
     */
    <T> Mono<Void> publish(T event);
    
    /**
     * Publica múltiples eventos de dominio.
     * 
     * @param events Eventos a publicar
     * @param <T> Tipo de los eventos
     * @return Mono completado cuando todos los eventos se publican
     */
    <T> Mono<Void> publishAll(java.util.List<T> events);
} 