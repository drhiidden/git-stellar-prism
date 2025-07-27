package com.drhdn.ghvis.application.query;

import lombok.Builder;
import lombok.Value;

import java.security.Principal;
import java.util.UUID;

/**
 * Query para obtener la lista de commits de un repositorio.
 */
@Value
@Builder
public class GetRepositoryCommitsQuery {

    /** Propietario del repositorio */
    String owner;

    /** Nombre del repositorio */
    String repo;

    /** Usuario autenticado */
    Principal principal;

    /** Identificador único de la query */
    String queryId;

    /** Timestamp de creación */
    long timestamp;

    /**
     * Crea una nueva query para obtener commits de un repositorio.
     */
    public static GetRepositoryCommitsQuery create(String owner, String repo, Principal principal) {
        return GetRepositoryCommitsQuery.builder()
                .owner(owner)
                .repo(repo)
                .principal(principal)
                .queryId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** Nombre completo del repositorio */
    public String getRepositoryFullName() {
        return owner + "/" + repo;
    }

    /** Clave de cache sugerida */
    public String getCacheKey() {
        return String.format("repo:%s:%s:commits", owner, repo);
    }
} 