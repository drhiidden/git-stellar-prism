package com.drhdn.ghvis.infrastructure.adapter.outbound.resilience;

import com.drhdn.ghvis.domain.port.CircuitBreakerService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter de infraestructura para Circuit Breaker usando Resilience4j.
 * 
 * Implementa el patrón Circuit Breaker para proteger contra fallos
 * de servicios externos (GitHub API) y mejorar la resiliencia del sistema.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class CircuitBreakerAdapter implements CircuitBreakerService {

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    @Value("${app.circuit-breaker.failure-rate-threshold:50}")
    private float failureRateThreshold;
    
    @Value("${app.circuit-breaker.wait-duration-in-open-state:30}")
    private int waitDurationInOpenState;
    
    @Value("${app.circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;
    
    @Value("${app.circuit-breaker.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;

    /**
     * Ejecuta una operación protegida por Circuit Breaker.
     * 
     * @param circuitBreakerName Nombre del circuit breaker
     * @param operation Operación a ejecutar
     * @param fallback Operación de fallback
     * @param <T> Tipo de retorno
     * @return Mono con el resultado
     */
    @Override
    public <T> Mono<T> executeWithCircuitBreaker(String circuitBreakerName, 
                                                Mono<T> operation, 
                                                Mono<T> fallback) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(circuitBreakerName);
        
        return Mono.defer(() -> {
            if (circuitBreaker.tryAcquirePermission()) {
                return operation
                    .doOnSuccess(result -> {
                        circuitBreaker.onSuccess(0, java.util.concurrent.TimeUnit.NANOSECONDS);
                        log.debug("Circuit Breaker '{}' - Operación exitosa", circuitBreakerName);
                    })
                    .doOnError(throwable -> {
                        circuitBreaker.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, throwable);
                        log.warn("Circuit Breaker '{}' - Error en operación: {}", 
                            circuitBreakerName, throwable.getMessage());
                    })
                    .onErrorResume(throwable -> {
                        if (throwable instanceof CallNotPermittedException) {
                            log.warn("Circuit Breaker '{}' - Circuito abierto, usando fallback", 
                                circuitBreakerName);
                            return fallback;
                        }
                        return Mono.error(throwable);
                    });
            } else {
                log.warn("Circuit Breaker '{}' - Permiso denegado, usando fallback", circuitBreakerName);
                return fallback;
            }
        });
    }

    /**
     * Obtiene estadísticas del Circuit Breaker.
     * 
     * @param circuitBreakerName Nombre del circuit breaker
     * @return Mono con las estadísticas
     */
    @Override
    public Mono<CircuitBreakerStats> getStats(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(circuitBreakerName);
        if (circuitBreaker == null) {
            return Mono.empty();
        }
        
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        
        CircuitBreakerStats stats = new CircuitBreakerStats() {
            @Override
            public String getCircuitBreakerName() { return circuitBreakerName; }
            
            @Override
            public CircuitBreakerState getState() { 
                return mapResilience4jState(circuitBreaker.getState()); 
            }
            
            @Override
            public float getFailureRate() { return metrics.getFailureRate(); }
            
            @Override
            public long getNumberOfFailedCalls() { return metrics.getNumberOfFailedCalls(); }
            
            @Override
            public long getNumberOfSuccessfulCalls() { return metrics.getNumberOfSuccessfulCalls(); }
            
            @Override
            public long getNumberOfNotPermittedCalls() { return metrics.getNumberOfNotPermittedCalls(); }
            
            @Override
            public long getTotalNumberOfCalls() { 
                return metrics.getNumberOfFailedCalls() + metrics.getNumberOfSuccessfulCalls(); 
            }
        };
        
        return Mono.just(stats);
    }

    /**
     * Resetea el Circuit Breaker a estado cerrado.
     * 
     * @param circuitBreakerName Nombre del circuit breaker
     * @return Mono completado cuando se resetea
     */
    @Override
    public Mono<Void> reset(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(circuitBreakerName);
        if (circuitBreaker != null) {
            circuitBreaker.reset();
            log.info("Circuit Breaker '{}' reseteado", circuitBreakerName);
        }
        return Mono.empty();
    }

    /**
     * Obtiene o crea un Circuit Breaker con la configuración especificada.
     * 
     * @param circuitBreakerName Nombre del circuit breaker
     * @return Circuit Breaker configurado
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String circuitBreakerName) {
        return circuitBreakers.computeIfAbsent(circuitBreakerName, name -> {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenState))
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build();
            
            CircuitBreaker circuitBreaker = CircuitBreaker.of(name, config);
            
            // Event listeners para logging
            circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.info("Circuit Breaker '{}' cambió de estado: {} → {}", 
                        name, event.getStateTransition().getFromState(), 
                        event.getStateTransition().getToState());
                })
                .onFailureRateExceeded(event -> {
                    log.warn("Circuit Breaker '{}' - Tasa de fallos excedida: {}%", 
                        name, event.getFailureRate());
                });
            
            log.info("Circuit Breaker '{}' creado con configuración: failureRate={}%, waitDuration={}s, windowSize={}", 
                name, failureRateThreshold, waitDurationInOpenState, slidingWindowSize);
            
            return circuitBreaker;
        });
    }
    
    /**
     * Mapea el estado de Resilience4j al estado del dominio.
     * 
     * @param resilience4jState Estado de Resilience4j
     * @return Estado del dominio
     */
    private CircuitBreakerState mapResilience4jState(CircuitBreaker.State resilience4jState) {
        return switch (resilience4jState.name()) {
            case "CLOSED" -> CircuitBreakerState.CLOSED;
            case "OPEN", "FORCED_OPEN" -> CircuitBreakerState.OPEN;
            case "HALF_OPEN" -> CircuitBreakerState.HALF_OPEN;
            case "DISABLED" -> CircuitBreakerState.CLOSED;
            default -> CircuitBreakerState.CLOSED;
        };
    }
} 