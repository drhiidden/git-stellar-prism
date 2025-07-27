package com.drhdn.ghvis.infrastructure.adapter.outbound.events;

import com.drhdn.ghvis.application.handler.CommitRetrievalEventHandler;
import com.drhdn.ghvis.application.handler.RepositoryAnalysisEventHandler;
import com.drhdn.ghvis.domain.event.*;
import com.drhdn.ghvis.domain.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Adapter de infraestructura para publicación de eventos de dominio.
 * 
 * Implementa el puerto EventPublisher, integrando con Event Handlers
 * para procesar eventos de manera asíncrona y desacoplada.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisherAdapter implements EventPublisher {
    
    private final CommitRetrievalEventHandler commitRetrievalEventHandler;
    private final RepositoryAnalysisEventHandler repositoryAnalysisEventHandler;
    
    /**
     * Publica un evento de dominio.
     * 
     * @param event Evento a publicar
     * @param <T> Tipo del evento
     * @return Mono completado cuando el evento se publica
     */
    @Override
    public <T> Mono<Void> publish(T event) {
        log.debug("📤 Publicando evento: {}", event.getClass().getSimpleName());
        
        return Mono.defer(() -> {
            if (event instanceof CommitRetrievalRequestedEvent requestedEvent) {
                return commitRetrievalEventHandler.handleCommitRetrievalRequested(requestedEvent);
            } else if (event instanceof CommitRetrievalCompletedEvent completedEvent) {
                return commitRetrievalEventHandler.handleCommitRetrievalCompleted(completedEvent);
            } else if (event instanceof CommitRetrievalFailedEvent failedEvent) {
                return commitRetrievalEventHandler.handleCommitRetrievalFailed(failedEvent);
            } else if (event instanceof RepositoryAnalysisRequestedEvent analysisRequestedEvent) {
                return repositoryAnalysisEventHandler.handleRepositoryAnalysisRequested(analysisRequestedEvent);
            } else if (event instanceof RepositoryAnalysisCompletedEvent analysisCompletedEvent) {
                return repositoryAnalysisEventHandler.handleRepositoryAnalysisCompleted(analysisCompletedEvent);
            } else if (event instanceof IssueRetrievalRequestedEvent issueRequestedEvent) {
                return repositoryAnalysisEventHandler.handleIssueRetrievalRequested(issueRequestedEvent);
            } else if (event instanceof PullRequestRetrievalRequestedEvent prRequestedEvent) {
                return repositoryAnalysisEventHandler.handlePullRequestRetrievalRequested(prRequestedEvent);
            } else if (event instanceof TechnicalSummaryGeneratedEvent technicalSummaryEvent) {
                return repositoryAnalysisEventHandler.handleTechnicalSummaryGenerated(technicalSummaryEvent);
            } else if (event instanceof UserRepositoriesRequestedEvent userReposRequestedEvent) {
                return repositoryAnalysisEventHandler.handleUserRepositoriesRequested(userReposRequestedEvent);
            } else if (event instanceof UserRepositoriesRetrievedEvent userReposRetrievedEvent) {
                return repositoryAnalysisEventHandler.handleUserRepositoriesRetrieved(userReposRetrievedEvent);
            } else if (event instanceof UserRepositoriesRetrievalFailedEvent userReposFailedEvent) {
                return repositoryAnalysisEventHandler.handleUserRepositoriesRetrievalFailed(userReposFailedEvent);
            } else {
                log.warn("⚠️ Evento no manejado: {}", event.getClass().getSimpleName());
                return Mono.empty();
            }
        })
        .doOnSuccess(v -> log.debug("✅ Evento publicado exitosamente: {}", event.getClass().getSimpleName()))
        .doOnError(error -> log.error("❌ Error publicando evento {}: {}", 
            event.getClass().getSimpleName(), error.getMessage()));
    }
    
    /**
     * Publica múltiples eventos de dominio.
     * 
     * @param events Eventos a publicar
     * @param <T> Tipo de los eventos
     * @return Mono completado cuando todos los eventos se publican
     */
    @Override
    public <T> Mono<Void> publishAll(List<T> events) {
        log.debug("📤 Publicando {} eventos", events.size());
        
        return Mono.fromCallable(() -> events)
            .flatMapMany(Flux::fromIterable)
            .flatMap(this::publish)
            .then()
            .doOnSuccess(v -> log.debug("✅ Todos los eventos publicados exitosamente"))
            .doOnError(error -> log.error("❌ Error publicando eventos: {}", error.getMessage()));
    }
} 