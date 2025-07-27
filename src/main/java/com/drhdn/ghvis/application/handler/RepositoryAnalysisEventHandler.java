package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Event Handler para eventos de análisis de repositorio.
 * 
 * Procesa los eventos de dominio relacionados con el análisis completo
 * de repositorios, incluyendo issues, pull requests y resúmenes técnicos.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryAnalysisEventHandler {
    
    /**
     * Maneja eventos de solicitud de análisis de repositorio.
     */
    public Mono<Void> handleRepositoryAnalysisRequested(RepositoryAnalysisRequestedEvent event) {
        String analysisType = event.isFullAnalysis() ? "🔍 COMPLETO" : "📊 PARCIAL";
        String components = buildComponentsString(event);
        
        log.info("{} Análisis de repositorio solicitado - Repo: {}, Usuario: {}, Componentes: {}, RequestId: {}", 
            analysisType, event.getRepositoryFullName(), event.getUsername(), components, event.getRequestId());
        
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de completación de análisis de repositorio.
     */
    public Mono<Void> handleRepositoryAnalysisCompleted(RepositoryAnalysisCompletedEvent event) {
        String status = event.isSuccessful() ? "✅" : "⚠️";
        String resilience = event.isResilienceUsed() ? "🛡️" : "⚡";
        String cache = event.isFromCache() ? "💾" : "🌐";
        
        log.info("{} Análisis completado - Repo: {}, Items: {} (Commits: {}, Issues: {}, PRs: {}), " +
                "Duration: {}ms, Type: {}, Cache: {}, Resilience: {}, RequestId: {}", 
            status, event.getRepositoryFullName(), event.getTotalItems(), 
            event.getCommitCount(), event.getIssueCount(), event.getPullRequestCount(),
            event.getDurationMs(), event.getAnalysisType(), cache, resilience, event.getRequestId());
        
        // Alertas para análisis lentos
        if (event.getDurationMs() > 10000) {
            log.warn("⚠️ Análisis lento detectado - Repo: {}, Duration: {}ms", 
                event.getRepositoryFullName(), event.getDurationMs());
        }
        
        // Alertas para repositorios grandes
        if (event.getTotalItems() > 1000) {
            log.info("📈 Repositorio grande detectado - Repo: {}, Total Items: {}", 
                event.getRepositoryFullName(), event.getTotalItems());
        }
        
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de solicitud de issues.
     */
    public Mono<Void> handleIssueRetrievalRequested(IssueRetrievalRequestedEvent event) {
        String state = getStateEmoji(event.getState());
        String sort = getSortEmoji(event.getSort());
        
        log.info("{} Solicitud de issues - Repo: {}, Estado: {}, Orden: {}, Página: {}, RequestId: {}", 
            state, event.getRepositoryFullName(), event.getState(), sort, event.getPage(), event.getRequestId());
        
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de solicitud de pull requests.
     */
    public Mono<Void> handlePullRequestRetrievalRequested(PullRequestRetrievalRequestedEvent event) {
        String state = getStateEmoji(event.getState());
        String detail = event.isDetailedAnalysis() ? "🔍" : "📋";
        
        log.info("{} Solicitud de PRs - Repo: {}, Estado: {}, Análisis: {}, RequestId: {}", 
            state, event.getRepositoryFullName(), event.getState(), detail, event.getRequestId());
        
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de generación de resumen técnico.
     */
    public Mono<Void> handleTechnicalSummaryGenerated(TechnicalSummaryGeneratedEvent event) {
        String status = event.isSuccessful() ? "✅" : "⚠️";
        String complexity = event.isComplexProject() ? "🧠" : "📝";
        String multiLang = event.isMultiLanguage() ? "🌍" : "🔤";
        String cache = event.isFromCache() ? "💾" : "🌐";
        
        log.info("{} Resumen técnico generado - Repo: {}, Tecnologías: {}, Lenguajes: {}, " +
                "Archivos: {}, Complejidad: {}, Cache: {}, RequestId: {}", 
            status, event.getRepositoryFullName(), event.getTechnologyCount(), 
            event.getLanguageCount(), event.getTotalFiles(), complexity, cache, event.getRequestId());
        
        // Alertas para proyectos complejos
        if (event.isComplexProject()) {
            log.info("🧠 Proyecto complejo detectado - Repo: {}, Score: {}", 
                event.getRepositoryFullName(), event.getComplexityScore());
        }
        
        // Alertas para proyectos multi-lenguaje
        if (event.isMultiLanguage()) {
            log.info("🌍 Proyecto multi-lenguaje detectado - Repo: {}, Lenguajes: {}", 
                event.getRepositoryFullName(), event.getLanguages());
        }
        
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de solicitud de repositorios de usuario.
     */
    public Mono<Void> handleUserRepositoriesRequested(UserRepositoriesRequestedEvent event) {
        String type = getRepositoryTypeEmoji(event.getRequestType());
        String details = event.isIncludeDetails() ? "🔍" : "📋";
        
        log.info("{} Solicitud de repositorios de usuario - Usuario: {}, Tipo: {}, Detalles: {}, RequestId: {}", 
            type, event.getUsername(), event.getRequestType(), details, event.getRequestId());
        
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de obtención exitosa de repositorios de usuario.
     */
    public Mono<Void> handleUserRepositoriesRetrieved(UserRepositoriesRetrievedEvent event) {
        String status = event.isSuccessful() ? "✅" : "⚠️";
        String type = getRepositoryTypeEmoji(event.getRequestType());
        String duration = event.getFormattedDuration();
        
        log.info("{} Repositorios de usuario obtenidos - Usuario: {}, Tipo: {}, Duration: {}, RequestId: {}", 
            status, event.getUsername(), type, duration, event.getRequestId());
        
        return Mono.empty();
    }
    
    /**
     * Maneja eventos de fallo en obtención de repositorios de usuario.
     */
    public Mono<Void> handleUserRepositoriesRetrievalFailed(UserRepositoriesRetrievalFailedEvent event) {
        String errorType = getErrorTypeEmoji(event.getErrorType());
        String duration = event.getFormattedDuration();
        
        log.error("{} Error obteniendo repositorios de usuario - Usuario: {}, Tipo: {}, Error: {}, Duration: {}, RequestId: {}", 
            errorType, event.getUsername(), event.getRequestType(), event.getErrorMessage(), duration, event.getRequestId());
        
        return Mono.empty();
    }
    
    /**
     * Construye string de componentes para logging.
     */
    private String buildComponentsString(RepositoryAnalysisRequestedEvent event) {
        StringBuilder components = new StringBuilder();
        if (event.isIncludeCommits()) components.append("📝");
        if (event.isIncludeIssues()) components.append("🐛");
        if (event.isIncludePullRequests()) components.append("🔀");
        if (event.isIncludeTechnicalSummary()) components.append("🔧");
        return components.toString();
    }
    
    /**
     * Obtiene emoji para estado.
     */
    private String getStateEmoji(String state) {
        return switch (state) {
            case "open" -> "🟢";
            case "closed" -> "🔴";
            case "all" -> "🔄";
            default -> "❓";
        };
    }
    
    /**
     * Obtiene emoji para ordenamiento.
     */
    private String getSortEmoji(String sort) {
        return switch (sort) {
            case "created" -> "📅";
            case "updated" -> "🔄";
            case "comments" -> "💬";
            case "popularity" -> "⭐";
            case "long-running" -> "⏰";
            default -> "📊";
        };
    }
    
    /**
     * Obtiene emoji para tipo de repositorio.
     */
    private String getRepositoryTypeEmoji(String type) {
        return switch (type) {
            case "all" -> "📚";
            case "public" -> "🌐";
            case "private" -> "🔒";
            case "detailed" -> "🔍";
            default -> "📁";
        };
    }
    
    /**
     * Obtiene emoji para tipo de error.
     */
    private String getErrorTypeEmoji(String errorType) {
        return switch (errorType) {
            case "USER_NOT_FOUND" -> "👤";
            case "RATE_LIMIT_EXCEEDED" -> "⏰";
            case "UNAUTHORIZED" -> "🔐";
            case "TIMEOUT" -> "⏱️";
            case "NETWORK_ERROR" -> "🌐";
            default -> "❌";
        };
    }
} 