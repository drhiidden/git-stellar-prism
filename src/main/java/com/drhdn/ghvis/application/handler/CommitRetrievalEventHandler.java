package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.domain.event.CommitRetrievalCompletedEvent;
import com.drhdn.ghvis.domain.event.CommitRetrievalFailedEvent;
import com.drhdn.ghvis.domain.event.CommitRetrievalRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Event Handler para eventos de obtención de commits.
 * 
 * Procesa los eventos de dominio relacionados con la obtención
 * de commits, proporcionando logging, métricas y análisis.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommitRetrievalEventHandler {
    
    /**
     * Maneja eventos de solicitud de commits.
     * 
     * @param event Evento de solicitud
     * @return Mono completado cuando se procesa el evento
     */
    public Mono<Void> handleCommitRetrievalRequested(CommitRetrievalRequestedEvent event) {
        log.info("📥 Solicitud de commits recibida - Repo: {}, Usuario: {}, Resiliencia: {}, RequestId: {}", 
            event.getRepositoryFullName(), event.getUsername(), event.isResilienceEnabled(), event.getRequestId());
        
        // Aquí podrías agregar métricas, analytics, etc.
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de completación de obtención de commits.
     * 
     * @param event Evento de completación
     * @return Mono completado cuando se procesa el evento
     */
    public Mono<Void> handleCommitRetrievalCompleted(CommitRetrievalCompletedEvent event) {
        String status = event.isSuccessful() ? "✅" : "⚠️";
        String source = event.getSource();
        String resilience = event.isResilienceUsed() ? "🛡️" : "⚡";
        
        log.info("{} Commits obtenidos - Repo: {}, Count: {}, Duration: {}ms, Source: {}, Resilience: {}, RequestId: {}", 
            status, event.getRepositoryFullName(), event.getCommitCount(), event.getDurationMs(), 
            source, resilience, event.getRequestId());
        
        // Aquí podrías agregar métricas de performance, analytics, etc.
        if (event.getDurationMs() > 5000) {
            log.warn("⚠️ Operación lenta detectada - Repo: {}, Duration: {}ms", 
                event.getRepositoryFullName(), event.getDurationMs());
        }
        
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de fallo en la obtención de commits.
     * 
     * @param event Evento de fallo
     * @return Mono completado cuando se procesa el evento
     */
    public Mono<Void> handleCommitRetrievalFailed(CommitRetrievalFailedEvent event) {
        String resilience = event.isResilienceEnabled() ? "🛡️" : "⚡";
        String failure = getFailureEmoji(event.getFailureReason());
        
        log.error("{} Fallo en obtención de commits - Repo: {}, Error: {}, Type: {}, Duration: {}ms, Resilience: {}, RequestId: {}", 
            failure, event.getRepositoryFullName(), event.getErrorMessage(), event.getErrorType(), 
            event.getDurationMs(), resilience, event.getRequestId());
        
        // Aquí podrías agregar alertas, métricas de error, etc.
        if (event.isResilienceFailure()) {
            log.warn("🛡️ Fallo de resiliencia detectado - Repo: {}, Reason: {}", 
                event.getRepositoryFullName(), event.getFailureReason());
        }
        
        return Mono.empty();
    }
    
    /**
     * Obtiene el emoji apropiado para el tipo de fallo.
     * 
     * @param failureReason Razón del fallo
     * @return Emoji representativo
     */
    private String getFailureEmoji(String failureReason) {
        return switch (failureReason) {
            case "rate_limit" -> "🚫";
            case "api_error" -> "🔌";
            case "network" -> "🌐";
            case "timeout" -> "⏰";
            default -> "❌";
        };
    }
} 