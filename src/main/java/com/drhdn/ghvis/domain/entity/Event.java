package com.drhdn.ghvis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Representa un evento de GitHub.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    /**
     * ID del evento
     */
    private String id;
    
    /**
     * Tipo de evento (push, pull_request, issue, etc.)
     */
    private String type;
    
    /**
     * Fecha y hora del evento
     */
    private Instant timestamp;
    
    /**
     * Actor que generó el evento
     */
    private Actor actor;
    
    /**
     * Repositorio asociado al evento
     */
    private String repositoryFullName;
    
    /**
     * Datos específicos del evento según su tipo
     */
    private Map<String, Object> payload;
    
    /**
     * Representa al actor que generó un evento
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Actor {
        /**
         * ID del actor
         */
        private Long id;
        
        /**
         * Nombre de usuario
         */
        private String login;
        
        /**
         * URL del avatar
         */
        private String avatarUrl;
    }
    
    /**
     * Tipos de eventos de GitHub
     */
    public static class EventType {
        public static final String PUSH = "push";
        public static final String PULL_REQUEST = "pull_request";
        public static final String PULL_REQUEST_REVIEW = "pull_request_review";
        public static final String PULL_REQUEST_REVIEW_COMMENT = "pull_request_review_comment";
        public static final String ISSUES = "issues";
        public static final String ISSUE_COMMENT = "issue_comment";
        public static final String CREATE = "create";
        public static final String DELETE = "delete";
        public static final String FORK = "fork";
        public static final String WATCH = "watch";
        public static final String STAR = "star";
        public static final String RELEASE = "release";
        public static final String COMMIT_COMMENT = "commit_comment";
    }
} 