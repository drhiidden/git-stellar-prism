package com.drhdn.ghvis.infrastructure.adapter.outbound.external;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Interceptor para trackear y gestionar el rate limit de GitHub API.
 * 
 * GitHub permite 5,000 requests/hora con OAuth2.
 * Este interceptor monitorea los límites y provee warnings/errors antes de alcanzarlos.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class GitHubRateLimitInterceptor implements ExchangeFilterFunction {
    
    private final AtomicInteger remainingRequests = new AtomicInteger(5000);
    private final AtomicInteger requestLimit = new AtomicInteger(5000);
    private final AtomicLong resetTimeEpoch = new AtomicLong(0);
    private final AtomicInteger requestsThisSession = new AtomicInteger(0);
    
    // Umbrales de warning
    private static final int WARNING_THRESHOLD = 500;
    private static final int DANGER_THRESHOLD = 100;
    private static final int CRITICAL_THRESHOLD = 10;
    
    @Override
    @NonNull
    public Mono<ClientResponse> filter(@NonNull ClientRequest request, @NonNull ExchangeFunction next) {
        // Pre-request: Verificar si es seguro hacer el request
        int remaining = remainingRequests.get();
        
        if (remaining < CRITICAL_THRESHOLD && remaining > 0) {
            log.error("🔴 CRÍTICO: Solo {} requests restantes hasta rate limit. Reset en: {}", 
                remaining, getTimeUntilReset());
        } else if (remaining < DANGER_THRESHOLD) {
            log.warn("⚠️ PELIGRO: Solo {} requests restantes. Reset en: {}", 
                remaining, getTimeUntilReset());
        } else if (remaining < WARNING_THRESHOLD && remaining % 100 == 0) {
            log.warn("⚠️ Rate limit bajo: {} requests restantes", remaining);
        }
        
        // Incrementar contador de requests de esta sesión
        requestsThisSession.incrementAndGet();
        
        // Ejecutar request
        return next.exchange(request)
            .map(response -> {
                // Post-request: Actualizar contadores desde headers de respuesta
                updateRateLimitFromHeaders(response);
                return response;
            })
            .doOnError(error -> {
                log.error("❌ Error en request a GitHub API: {}", error.getMessage());
            });
    }
    
    /**
     * Actualiza los contadores de rate limit desde los headers de respuesta de GitHub
     */
    private void updateRateLimitFromHeaders(ClientResponse response) {
        response.headers().asHttpHeaders().forEach((key, values) -> {
            if (values == null || values.isEmpty()) return;
            
            try {
                switch (key.toLowerCase()) {
                    case "x-ratelimit-remaining":
                        int newRemaining = Integer.parseInt(values.get(0));
                        int oldRemaining = remainingRequests.getAndSet(newRemaining);
                        
                        // Log si hay un cambio significativo
                        if (oldRemaining - newRemaining > 1) {
                            log.debug("📊 Rate limit: {} → {} (-{} requests)", 
                                oldRemaining, newRemaining, oldRemaining - newRemaining);
                        }
                        break;
                        
                    case "x-ratelimit-limit":
                        requestLimit.set(Integer.parseInt(values.get(0)));
                        break;
                        
                    case "x-ratelimit-reset":
                        long resetTime = Long.parseLong(values.get(0));
                        resetTimeEpoch.set(resetTime);
                        
                        // Log cuando se actualiza el reset time
                        if (log.isDebugEnabled()) {
                            Duration timeUntilReset = Duration.between(
                                Instant.now(), 
                                Instant.ofEpochSecond(resetTime)
                            );
                            log.debug("⏰ Rate limit reset en: {}", formatDuration(timeUntilReset));
                        }
                        break;
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ No se pudo parsear header {}: {}", key, values.get(0));
            }
        });
    }
    
    /**
     * Obtiene el número de requests restantes
     */
    public int getRemainingRequests() {
        return remainingRequests.get();
    }
    
    /**
     * Obtiene el límite total de requests
     */
    public int getRequestLimit() {
        return requestLimit.get();
    }
    
    /**
     * Obtiene el tiempo hasta el reset del rate limit
     */
    public Duration getTimeUntilReset() {
        long resetTime = resetTimeEpoch.get();
        if (resetTime == 0) {
            return Duration.ZERO;
        }
        return Duration.between(Instant.now(), Instant.ofEpochSecond(resetTime));
    }
    
    /**
     * Obtiene el porcentaje de rate limit usado
     */
    public double getUsagePercentage() {
        int limit = requestLimit.get();
        if (limit == 0) return 0.0;
        
        int used = limit - remainingRequests.get();
        return (used / (double) limit) * 100.0;
    }
    
    /**
     * Obtiene el número de requests hechos en esta sesión
     */
    public int getRequestsThisSession() {
        return requestsThisSession.get();
    }
    
    /**
     * Verifica si es seguro hacer un request
     */
    public boolean isSafeToRequest() {
        return remainingRequests.get() > DANGER_THRESHOLD;
    }
    
    /**
     * Verifica si estamos en zona de peligro
     */
    public boolean isInDangerZone() {
        return remainingRequests.get() <= DANGER_THRESHOLD;
    }
    
    /**
     * Verifica si estamos en zona crítica
     */
    public boolean isInCriticalZone() {
        return remainingRequests.get() <= CRITICAL_THRESHOLD;
    }
    
    /**
     * Formatea una duración para logging legible
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Resetea los contadores de sesión (útil para testing)
     */
    public void resetSessionCounters() {
        requestsThisSession.set(0);
        log.info("🔄 Contadores de sesión reseteados");
    }
    
    /**
     * Obtiene un resumen del estado actual del rate limit
     */
    public RateLimitStatus getStatus() {
        return new RateLimitStatus(
            remainingRequests.get(),
            requestLimit.get(),
            resetTimeEpoch.get(),
            requestsThisSession.get(),
            getUsagePercentage(),
            getTimeUntilReset()
        );
    }
    
    /**
     * Record para exponer el estado del rate limit
     */
    public record RateLimitStatus(
        int remaining,
        int limit,
        long resetTimeEpoch,
        int requestsThisSession,
        double usagePercentage,
        Duration timeUntilReset
    ) {
        public boolean isSafe() {
            return remaining > DANGER_THRESHOLD;
        }
        
        public boolean isDanger() {
            return remaining <= DANGER_THRESHOLD && remaining > CRITICAL_THRESHOLD;
        }
        
        public boolean isCritical() {
            return remaining <= CRITICAL_THRESHOLD;
        }
        
        public String getStatusLevel() {
            if (isCritical()) return "CRITICAL";
            if (isDanger()) return "DANGER";
            if (remaining < WARNING_THRESHOLD) return "WARNING";
            return "OK";
        }
    }
}

