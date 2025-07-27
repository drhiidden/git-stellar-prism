package com.drhdn.ghvis.application.query;

import lombok.Builder;
import lombok.Value;

import java.security.Principal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Query para obtener timeline de eventos de un repositorio.
 * Sigue el patrón CQRS establecido en el proyecto.
 */
@Value
@Builder
public class GetRepositoryTimelineQuery {

    /** Propietario del repositorio */
    String owner;

    /** Nombre del repositorio */
    String repo;

    /** Tipos de eventos a incluir */
    Set<EventType> eventTypes;

    /** Fecha desde (opcional) */
    Instant since;

    /** Fecha hasta (opcional) */
    Instant until;

    /** Límite de eventos */
    int limit;

    /** Usuario autenticado */
    Principal principal;

    /** ID único de la query */
    String queryId;

    /** Timestamp de creación */
    long timestamp;

    /**
     * Tipos de eventos disponibles en el timeline
     */
    public enum EventType {
        COMMITS, PULL_REQUESTS, ISSUES
    }

    /**
     * Crea query para timeline completo (todos los tipos).
     */
    public static GetRepositoryTimelineQuery createFullTimeline(String owner, String repo, Principal principal) {
        return GetRepositoryTimelineQuery.builder()
                .owner(owner)
                .repo(repo)
                .eventTypes(Set.of(EventType.COMMITS, EventType.PULL_REQUESTS, EventType.ISSUES))
                .limit(100)
                .principal(principal)
                .queryId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Crea query para timeline específico con filtros.
     */
    public static GetRepositoryTimelineQuery createFiltered(String owner, String repo, 
            Set<EventType> eventTypes, Instant since, Instant until, 
            int limit, Principal principal) {
        return GetRepositoryTimelineQuery.builder()
                .owner(owner)
                .repo(repo)
                .eventTypes(eventTypes)
                .since(since)
                .until(until)
                .limit(limit)
                .principal(principal)
                .queryId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** Nombre completo del repositorio */
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }

    /** Clave de cache para esta query */
    public String getCacheKey() {
        String eventTypesKey = String.join("-", eventTypes.stream()
                .map(Enum::name).sorted().toArray(String[]::new));
        return String.format("repo:%s:%s:timeline:%s:%d", owner, repo, eventTypesKey, limit);
    }
} 