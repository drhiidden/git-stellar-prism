package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.command.CreateRepositoryAnalysisCommand;
import com.drhdn.ghvis.application.usecase.GetRepositoryAnalysisUseCase;
import com.drhdn.ghvis.domain.event.RepositoryAnalysisRequestedEvent;
import com.drhdn.ghvis.domain.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Command Handler para CreateRepositoryAnalysisCommand.
 * 
 * Maneja la ejecución de comandos de creación de análisis de repositorios,
 * integrando con Event-Driven Architecture y Use Cases.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateRepositoryAnalysisCommandHandler {
    
    private final GetRepositoryAnalysisUseCase getRepositoryAnalysisUseCase;
    private final EventPublisher eventPublisher;
    
    /**
     * Maneja el comando de creación de análisis de repositorio.
     * 
     * @param command Comando a ejecutar
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handle(CreateRepositoryAnalysisCommand command) {
        log.info("🎯 Ejecutando comando de análisis para {}/{} (RequestId: {})", 
            command.getOwner(), command.getRepo(), command.getRequestId());
        
        // Publicar evento de solicitud
        RepositoryAnalysisRequestedEvent requestEvent = new RepositoryAnalysisRequestedEvent(
            command.getOwner(), command.getRepo(), command.getPrincipal(),
            command.isIncludeCommits(), command.isIncludeIssues(),
            command.isIncludePullRequests(), command.isIncludeTechnicalSummary());
        
        return eventPublisher.publish(requestEvent)
            .then(Mono.defer(() -> {
                log.info("🔍 Iniciando análisis desde comando para {}/{}", 
                    command.getOwner(), command.getRepo());
                
                return getRepositoryAnalysisUseCase.execute(
                    command.getOwner(), command.getRepo(), command.getPrincipal(),
                    command.isIncludeCommits(), command.isIncludeIssues(),
                    command.isIncludePullRequests(), command.isIncludeTechnicalSummary()
                );
            }))
            .doOnSuccess(result -> {
                log.info("✅ Análisis completado exitosamente para {}/{} (RequestId: {})", 
                    command.getOwner(), command.getRepo(), command.getRequestId());
            })
            .doOnError(error -> {
                log.error("❌ Error en análisis para {}/{} (RequestId: {}): {}", 
                    command.getOwner(), command.getRepo(), command.getRequestId(), error.getMessage());
            });
    }
    
    /**
     * Maneja el comando de creación de análisis completo.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handleFullAnalysis(String owner, String repo, java.security.Principal principal) {
        CreateRepositoryAnalysisCommand command = CreateRepositoryAnalysisCommand.create(owner, repo, principal);
        return handle(command);
    }
    
    /**
     * Maneja el comando de creación de análisis personalizado.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @param includeCommits Incluir commits
     * @param includeIssues Incluir issues
     * @param includePullRequests Incluir pull requests
     * @param includeTechnicalSummary Incluir resumen técnico
     * @return Mono con el resultado del análisis
     */
    public Mono<Map<String, Object>> handleCustomAnalysis(String owner, String repo, java.security.Principal principal,
                                                        boolean includeCommits, boolean includeIssues,
                                                        boolean includePullRequests, boolean includeTechnicalSummary) {
        CreateRepositoryAnalysisCommand command = CreateRepositoryAnalysisCommand.createCustom(
            owner, repo, principal, includeCommits, includeIssues, includePullRequests, includeTechnicalSummary);
        return handle(command);
    }
} 