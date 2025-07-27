package com.drhdn.ghvis.domain.port;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida para Circuit Breaker.
 * 
 * Define el contrato para implementar el patrón Circuit Breaker,
 * protegiendo contra fallos de servicios externos y mejorando la resiliencia.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface CircuitBreakerService {
    
    /**
     * Ejecuta una operación protegida por Circuit Breaker.
     * 
     * @param circuitBreakerName Nombre del circuit breaker
     * @param operation Operación a ejecutar
     * @param fallback Operación de fallback
     * @param <T> Tipo de retorno
     * @return Mono con el resultado
     */
    <T> Mono<T> executeWithCircuitBreaker(String circuitBreakerName, 
                                         Mono<T> operation, 
                                         Mono<T> fallback);
    
    /**
     * Obtiene estadísticas del Circuit Breaker.
     * 
     * @param circuitBreakerName Nombre del circuit breaker
     * @return Mono con las estadísticas
     */
    Mono<CircuitBreakerStats> getStats(String circuitBreakerName);
    
    /**
     * Resetea el Circuit Breaker a estado cerrado.
     * 
     * @param circuitBreakerName Nombre del circuit breaker
     * @return Mono completado cuando se resetea
     */
    Mono<Void> reset(String circuitBreakerName);
    
    /**
     * Estadísticas del Circuit Breaker.
     */
    interface CircuitBreakerStats {
        /**
         * Nombre del circuit breaker.
         */
        String getCircuitBreakerName();
        
        /**
         * Estado actual del circuit breaker.
         */
        CircuitBreakerState getState();
        
        /**
         * Tasa de fallos en porcentaje.
         */
        float getFailureRate();
        
        /**
         * Número de llamadas fallidas.
         */
        long getNumberOfFailedCalls();
        
        /**
         * Número de llamadas exitosas.
         */
        long getNumberOfSuccessfulCalls();
        
        /**
         * Número de llamadas no permitidas.
         */
        long getNumberOfNotPermittedCalls();
        
        /**
         * Total de llamadas realizadas.
         */
        long getTotalNumberOfCalls();
    }
    
    /**
     * Estados del Circuit Breaker.
     */
    enum CircuitBreakerState {
        CLOSED,     // Funcionando normalmente
        OPEN,       // Circuito abierto, fallback activo
        HALF_OPEN   // Probando si el servicio se recuperó
    }
} 