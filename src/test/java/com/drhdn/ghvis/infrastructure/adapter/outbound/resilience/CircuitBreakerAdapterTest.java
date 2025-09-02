package com.drhdn.ghvis.infrastructure.adapter.outbound.resilience;

import com.drhdn.ghvis.domain.port.CircuitBreakerService.CircuitBreakerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

/**
 * Tests unitarios para CircuitBreakerAdapter.
 * 
 * Valida el comportamiento del Circuit Breaker en diferentes escenarios:
 * - Operaciones exitosas
 * - Fallos y fallbacks
 * - Estados del circuit breaker
 * - Estadísticas y métricas
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CircuitBreakerAdapterTest {

    private CircuitBreakerAdapter circuitBreakerAdapter;

    @BeforeEach
    void setUp() {
        circuitBreakerAdapter = new CircuitBreakerAdapter();
        
        // Configure test properties
        ReflectionTestUtils.setField(circuitBreakerAdapter, "failureRateThreshold", 50.0f);
        ReflectionTestUtils.setField(circuitBreakerAdapter, "waitDurationInOpenState", 5);
        ReflectionTestUtils.setField(circuitBreakerAdapter, "slidingWindowSize", 5);
        ReflectionTestUtils.setField(circuitBreakerAdapter, "minimumNumberOfCalls", 3);
    }

    @Test
    void executeWithCircuitBreaker_WithSuccessfulOperation_ShouldReturnResult() {
        // Given
        String circuitBreakerName = "test-success";
        String expectedResult = "success";
        Mono<String> operation = Mono.just(expectedResult);
        Mono<String> fallback = Mono.just("fallback");

        // When & Then
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, operation, fallback)
        )
        .expectNext(expectedResult)
        .verifyComplete();
    }

    @Test
    void executeWithCircuitBreaker_WithFailingOperation_ShouldUseFallback() {
        // Given
        String circuitBreakerName = "test-failure";
        RuntimeException error = new RuntimeException("Operation failed");
        Mono<String> operation = Mono.error(error);
        Mono<String> fallback = Mono.just("fallback-result");

        // When & Then
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, operation, fallback)
        )
        .expectNext("fallback-result")
        .verifyComplete();
    }

    @Test
    void executeWithCircuitBreaker_WithMultipleFailures_ShouldOpenCircuit() {
        // Given
        String circuitBreakerName = "test-multiple-failures";
        RuntimeException error = new RuntimeException("Persistent failure");
        Mono<String> failingOperation = Mono.error(error);
        Mono<String> fallback = Mono.just("fallback");

        // When - Execute multiple failing operations to trigger circuit opening
        // First 3 failures (minimum number of calls)
        for (int i = 0; i < 3; i++) {
            StepVerifier.create(
                circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, failingOperation, fallback)
            )
            .expectNext("fallback")
            .verifyComplete();
        }

        // Additional failures to exceed failure rate threshold
        for (int i = 0; i < 3; i++) {
            StepVerifier.create(
                circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, failingOperation, fallback)
            )
            .expectNext("fallback")
            .verifyComplete();
        }

        // Then - Circuit should be open, subsequent calls should use fallback immediately
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, Mono.just("should-not-execute"), fallback)
        )
        .expectNext("fallback")
        .verifyComplete();
    }

    @Test
    void getStats_ForExistingCircuitBreaker_ShouldReturnStats() {
        // Given
        String circuitBreakerName = "test-stats";
        Mono<String> operation = Mono.just("test");
        Mono<String> fallback = Mono.just("fallback");

        // Execute operation to create circuit breaker
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, operation, fallback)
        )
        .expectNext("test")
        .verifyComplete();

        // When & Then
        StepVerifier.create(circuitBreakerAdapter.getStats(circuitBreakerName))
            .expectNextMatches(stats -> 
                stats != null && 
                stats.getCircuitBreakerName().equals(circuitBreakerName) &&
                stats.getState() == CircuitBreakerState.CLOSED &&
                stats.getNumberOfSuccessfulCalls() >= 1
            )
            .verifyComplete();
    }

    @Test
    void getStats_ForNonExistentCircuitBreaker_ShouldReturnEmpty() {
        // Given
        String nonExistentName = "non-existent-circuit-breaker";

        // When & Then
        StepVerifier.create(circuitBreakerAdapter.getStats(nonExistentName))
            .verifyComplete(); // Should complete empty
    }

    @Test
    void reset_ForExistingCircuitBreaker_ShouldResetToClosedState() {
        // Given
        String circuitBreakerName = "test-reset";
        
        // First, create a circuit breaker by executing an operation
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(
                circuitBreakerName, 
                Mono.just("initial"), 
                Mono.just("fallback")
            )
        )
        .expectNext("initial")
        .verifyComplete();

        // When
        StepVerifier.create(circuitBreakerAdapter.reset(circuitBreakerName))
            .verifyComplete();

        // Then - Circuit breaker should be reset and operational
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(
                circuitBreakerName, 
                Mono.just("after-reset"), 
                Mono.just("fallback")
            )
        )
        .expectNext("after-reset")
        .verifyComplete();
    }

    @Test
    void reset_ForNonExistentCircuitBreaker_ShouldCompleteWithoutError() {
        // Given
        String nonExistentName = "non-existent-for-reset";

        // When & Then
        StepVerifier.create(circuitBreakerAdapter.reset(nonExistentName))
            .verifyComplete();
    }

    @Test
    void executeWithCircuitBreaker_WithDifferentCircuitBreakers_ShouldIsolateState() {
        // Given
        String circuitBreaker1 = "isolated-1";
        String circuitBreaker2 = "isolated-2";
        
        Mono<String> successOperation = Mono.just("success");
        Mono<String> failOperation = Mono.error(new RuntimeException("fail"));
        Mono<String> fallback = Mono.just("fallback");

        // When - Make circuit breaker 1 fail multiple times
        for (int i = 0; i < 5; i++) {
            StepVerifier.create(
                circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreaker1, failOperation, fallback)
            )
            .expectNext("fallback")
            .verifyComplete();
        }

        // Then - Circuit breaker 2 should still work normally
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreaker2, successOperation, fallback)
        )
        .expectNext("success")
        .verifyComplete();
    }

    @Test
    void executeWithCircuitBreaker_WithSlowOperation_ShouldHandleTimeout() {
        // Given
        String circuitBreakerName = "test-slow-operation";
        Mono<String> slowOperation = Mono.just("slow-result")
            .delayElement(Duration.ofSeconds(1)); // Simulate slow operation
        Mono<String> fallback = Mono.just("fast-fallback");

        // When & Then - Should complete with the slow operation result
        // (Circuit breaker doesn't implement timeout by default, just failure detection)
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, slowOperation, fallback)
        )
        .expectNext("slow-result")
        .expectComplete()
        .verify(Duration.ofSeconds(2)); // Allow enough time for slow operation
    }

    @Test
    void executeWithCircuitBreaker_WithReactiveChain_ShouldWorkWithComplexOperations() {
        // Given
        String circuitBreakerName = "test-reactive-chain";
        
        Mono<String> complexOperation = Mono.just("input")
            .map(s -> s.toUpperCase())
            .filter(s -> s.length() > 0)
            .map(s -> "processed-" + s);
            
        Mono<String> fallback = Mono.just("chain-fallback");

        // When & Then
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, complexOperation, fallback)
        )
        .expectNext("processed-INPUT")
        .verifyComplete();
    }

    @Test
    void getStats_AfterMultipleOperations_ShouldTrackCorrectMetrics() {
        // Given
        String circuitBreakerName = "test-metrics";
        Mono<String> successOperation = Mono.just("success");
        Mono<String> failOperation = Mono.error(new RuntimeException("fail"));
        Mono<String> fallback = Mono.just("fallback");

        // When - Execute mix of successful and failing operations
        // 2 successful operations
        for (int i = 0; i < 2; i++) {
            StepVerifier.create(
                circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, successOperation, fallback)
            )
            .expectNext("success")
            .verifyComplete();
        }

        // 1 failing operation
        StepVerifier.create(
            circuitBreakerAdapter.executeWithCircuitBreaker(circuitBreakerName, failOperation, fallback)
        )
        .expectNext("fallback")
        .verifyComplete();

        // Then - Check metrics
        StepVerifier.create(circuitBreakerAdapter.getStats(circuitBreakerName))
            .expectNextMatches(stats -> 
                stats.getNumberOfSuccessfulCalls() >= 2 &&
                stats.getNumberOfFailedCalls() >= 1 &&
                stats.getTotalNumberOfCalls() >= 3
            )
            .verifyComplete();
    }
}
