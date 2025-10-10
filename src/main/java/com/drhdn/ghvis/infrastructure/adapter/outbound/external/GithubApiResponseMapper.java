package com.drhdn.ghvis.infrastructure.adapter.outbound.external;

import com.drhdn.ghvis.domain.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Componente para mapear las respuestas de la API de GitHub (Map<String, Object>) a objetos de dominio.
 */
@Component
@Slf4j
public class GithubApiResponseMapper {

    /**
     * Convierte un mapa de respuesta de la API a un objeto Repository.
     *
     * @param map Mapa con datos del repositorio
     * @return Objeto Repository
     */
    public Repository mapToRepository(Map<String, Object> map) {
        // Extraer lenguaje principal
        String primaryLanguage = getString(map, "language");
        Map<String, Long> languageDistribution = null;
        
        // Si hay lenguaje, crear distribución simple (será el único lenguaje con 100%)
        if (primaryLanguage != null && !primaryLanguage.isBlank()) {
            languageDistribution = Map.of(primaryLanguage, 100L);
        }
        
        return Repository.builder()
                .id(getLong(map, "id"))
                .name(getString(map, "name"))
                .owner(getString(getNestedMap(map, "owner"), "login"))
                .description(getString(map, "description"))
                .url(getString(map, "html_url"))
                .defaultBranch(getString(map, "default_branch"))
                .createdAt(getInstant(map, "created_at"))
                .updatedAt(getInstant(map, "updated_at"))
                .pushedAt(getInstant(map, "pushed_at"))
                .stargazersCount(getInt(map, "stargazers_count"))
                .forksCount(getInt(map, "forks_count"))
                .watchersCount(getInt(map, "watchers_count"))
                .openIssuesCount(getInt(map, "open_issues_count"))
                .size(getInt(map, "size"))
                .fork(getBoolean(map, "fork"))
                .isPrivate(getBoolean(map, "private"))
                .archived(getBoolean(map, "archived"))
                .languageDistribution(languageDistribution)
                .topics(getStringList(map, "topics"))
                .build();
    }

    /**
     * Convierte un mapa de respuesta de la API a un objeto Commit.
     *
     * @param map Mapa con datos del commit
     * @return Objeto Commit
     */
    public Commit mapToCommit(Map<String, Object> map) {
        Map<String, Object> commitMap = getNestedMap(map, "commit");
        Map<String, Object> authorMap = getNestedMap(commitMap, "author");
        Map<String, Object> committerMap = getNestedMap(commitMap, "committer");

        return Commit.builder()
                .hash(getString(map, "sha"))
                .message(getString(commitMap, "message"))
                .author(getString(authorMap, "name"))
                .authorEmail(getString(authorMap, "email"))
                .authorAvatar(getString(getNestedMap(map, "author"), "avatar_url"))
                .timestamp(getInstant(committerMap, "date"))
                .stats(Commit.CommitStats.builder().build()) // Datos básicos sin estadísticas detalladas
                .build();
    }

    /**
     * Convierte un mapa de respuesta de la API a un objeto Commit con detalles.
     *
     * @param map Mapa con datos detallados del commit
     * @return Objeto Commit
     */
    public Commit mapToDetailedCommit(Map<String, Object> map) {
        Commit commit = mapToCommit(map);

        // Añadir estadísticas si están disponibles
        Map<String, Object> statsMap = getNestedMap(map, "stats");
        if (!statsMap.isEmpty()) {
            Commit.CommitStats stats = Commit.CommitStats.builder()
                    .additions(getInt(statsMap, "additions"))
                    .deletions(getInt(statsMap, "deletions"))
                    .filesChanged(getInt(statsMap, "total"))
                    .build();
            commit.setStats(stats);
        }

        // Añadir padres
        List<Map<String, Object>> parents = getNestedList(map, "parents");
        if (parents != null) {
            List<String> parentHashes = parents.stream()
                    .map(parent -> getString(parent, "sha"))
                    .collect(Collectors.toList());
            commit.setParents(parentHashes);
        }

        return commit;
    }

    /**
     * Convierte un mapa de respuesta de la API a un objeto PullRequest.
     *
     * @param map Mapa con datos del pull request
     * @return Objeto PullRequest
     */
    public PullRequest mapToPullRequest(Map<String, Object> map) {
        Map<String, Object> userMap = getNestedMap(map, "user");
        Map<String, Object> headMap = getNestedMap(map, "head");
        Map<String, Object> baseMap = getNestedMap(map, "base");

        return PullRequest.builder()
                .id(getLong(map, "id"))
                .number(getInt(map, "number"))
                .title(getString(map, "title"))
                .description(getString(map, "body"))
                .state(getString(map, "state"))
                .author(getString(userMap, "login"))
                .authorAvatar(getString(userMap, "avatar_url"))
                .timestamp(getInstant(map, "created_at"))
                .updatedAt(getInstant(map, "updated_at"))
                .closedAt(getInstant(map, "closed_at"))
                .mergedAt(getInstant(map, "merged_at"))
                .headBranch(getString(headMap, "ref"))
                .baseBranch(getString(baseMap, "ref"))
                .build();
    }

    /**
     * Convierte un mapa de respuesta de la API a un objeto Issue.
     *
     * @param map Mapa con datos del issue
     * @return Objeto Issue
     */
    public Issue mapToIssue(Map<String, Object> map) {
        Map<String, Object> userMap = getNestedMap(map, "user");

        // Mapear etiquetas
        List<Map<String, Object>> labelsData = getNestedList(map, "labels");
        List<Issue.Label> labels = null;
        if (labelsData != null) {
            labels = labelsData.stream()
                    .map(labelMap -> Issue.Label.builder()
                            .name(getString(labelMap, "name"))
                            .color(getString(labelMap, "color"))
                            .description(getString(labelMap, "description"))
                            .build())
                    .collect(Collectors.toList());
        }

        // Mapear asignados
        List<Map<String, Object>> assigneesData = getNestedList(map, "assignees");
        List<String> assignees = null;
        if (assigneesData != null) {
            assignees = assigneesData.stream()
                    .map(assigneeMap -> getString(assigneeMap, "login"))
                    .collect(Collectors.toList());
        }

        return Issue.builder()
                .id(getLong(map, "id"))
                .number(getInt(map, "number"))
                .title(getString(map, "title"))
                .description(getString(map, "body"))
                .state(getString(map, "state"))
                .author(getString(userMap, "login"))
                .authorAvatar(getString(userMap, "avatar_url"))
                .timestamp(getInstant(map, "created_at"))
                .updatedAt(getInstant(map, "updated_at"))
                .closedAt(getInstant(map, "closed_at"))
                .labels(labels)
                .assignees(assignees)
                .commentCount(getInt(map, "comments"))
                .build();
    }

    /**
     * Convierte un mapa de respuesta de la API a un objeto Readme.
     *
     * @param map Mapa con datos del readme
     * @return Objeto Readme
     */
    public Readme mapToReadme(Map<String, Object> map) {
        return Readme.builder()
                .type(getString(map, "type"))
                .encoding(getString(map, "encoding"))
                .size(getInt(map, "size"))
                .name(getString(map, "name"))
                .path(getString(map, "path"))
                .content(getString(map, "content"))
                .sha(getString(map, "sha"))
                .url(getString(map, "url"))
                .gitUrl(getString(map, "git_url"))
                .htmlUrl(getString(map, "html_url"))
                .downloadUrl(getString(map, "download_url"))
                .build();
    }

    // Métodos de utilidad para extraer valores de mapas (ahora en el mapper)

    protected String getString(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) ? String.valueOf(map.get(key)) : null;
    }

    protected Integer getInt(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    protected Long getLong(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0L;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    protected Boolean getBoolean(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return false;
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    protected Instant getInstant(Map<String, Object> map, String key) {
        String dateStr = getString(map, key);
        return dateStr != null ? Instant.parse(dateStr) : null;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return Collections.emptyMap();
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getNestedList(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return Collections.emptyList();
        Object value = map.get(key);
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.emptyList();
    }

    protected List<String> getStringList(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return Collections.emptyList();
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .filter(item -> item instanceof String)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}