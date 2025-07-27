package com.drhdn.ghvis.domain.entity;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Representa un evento unificado en el timeline del repositorio.
 * Abstrae diferentes tipos de eventos (commits, PRs, issues) en una estructura común.
 */
@Value
@Builder
public class TimelineEvent {

    /** ID único del evento */
    String id;

    /** Tipo de evento */
    EventType type;

    /** Título o mensaje descriptivo */
    String title;

    /** Descripción adicional (opcional) */
    String description;

    /** Autor del evento */
    String author;

    /** Email del autor (opcional) */
    String authorEmail;

    /** Fecha del evento */
    Instant date;

    /** URL para acceder al evento en GitHub */
    String url;

    /** Número de líneas añadidas (para commits) */
    Integer additions;

    /** Número de líneas eliminadas (para commits) */
    Integer deletions;

    /** Estado del evento (para PRs/Issues: open, closed, merged) */
    String status;

    /** Labels asociados (para PRs/Issues) */
    String[] labels;

    /**
     * Tipos de eventos soportados en el timeline
     */
    public enum EventType {
        COMMIT("commit"),
        PULL_REQUEST("pull_request"),
        ISSUE("issue");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Verifica si es un evento de código (commit)
     */
    public boolean isCodeEvent() {
        return type == EventType.COMMIT;
    }

    /**
     * Verifica si es un evento de colaboración (PR/Issue)
     */
    public boolean isCollaborationEvent() {
        return type == EventType.PULL_REQUEST || type == EventType.ISSUE;
    }

    /**
     * Obtiene el impacto del evento (líneas de código para commits)
     */
    public int getImpact() {
        if (additions != null && deletions != null) {
            return additions + deletions;
        }
        return 0;
    }

    /**
     * Obtiene una descripción amigable del evento
     */
    public String getFriendlyDescription() {
        return switch (type) {
            case COMMIT -> String.format("Commit: %s", title);
            case PULL_REQUEST -> String.format("Pull Request #%s: %s", 
                id.substring(id.lastIndexOf('/') + 1), title);
            case ISSUE -> String.format("Issue #%s: %s", 
                id.substring(id.lastIndexOf('/') + 1), title);
        };
    }
} 