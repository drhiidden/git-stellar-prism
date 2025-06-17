package com.drhdn.ghvis.service;

import com.drhdn.ghvis.model.Commit;
import com.drhdn.ghvis.model.Issue;
import com.drhdn.ghvis.model.PullRequest;
import com.drhdn.ghvis.model.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Servicio para interactuar con la API de GitHub.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GithubService {

    private final WebClient webClient;
    
    @Value("${github.api.token:}")
    private String githubToken;
    
    private static final String GITHUB_API_URL = "https://api.github.com";
    
    /**
     * Obtiene información de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Mono con la información del repositorio
     */
    public Mono<Repository> getRepository(String owner, String repo) {
        return webClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}", owner, repo)
                .headers(this::setAuthHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToRepository)
                .doOnError(e -> log.error("Error al obtener el repositorio {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene los commits de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Flux de commits
     */
    public Flux<Commit> getCommits(String owner, String repo) {
        return webClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/commits", owner, repo)
                .headers(this::setAuthHeader)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToCommit)
                .doOnError(e -> log.error("Error al obtener los commits de {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene los pull requests de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param state Estado de los PRs (open, closed, all)
     * @return Flux de pull requests
     */
    public Flux<PullRequest> getPullRequests(String owner, String repo, String state) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(GITHUB_API_URL + "/repos/{owner}/{repo}/pulls")
                        .queryParam("state", state)
                        .build(owner, repo))
                .headers(this::setAuthHeader)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToPullRequest)
                .doOnError(e -> log.error("Error al obtener los PRs de {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene los issues de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param state Estado de los issues (open, closed, all)
     * @return Flux de issues
     */
    public Flux<Issue> getIssues(String owner, String repo, String state) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(GITHUB_API_URL + "/repos/{owner}/{repo}/issues")
                        .queryParam("state", state)
                        .build(owner, repo))
                .headers(this::setAuthHeader)
                .retrieve()
                .bodyToFlux(Map.class)
                .filter(map -> !map.containsKey("pull_request")) // Filtrar PRs que también aparecen como issues
                .map(this::mapToIssue)
                .doOnError(e -> log.error("Error al obtener los issues de {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene la distribución de lenguajes en un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Mono con mapa de lenguajes y bytes
     */
    public Mono<Map<String, Long>> getLanguages(String owner, String repo) {
        return webClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/languages", owner, repo)
                .headers(this::setAuthHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .cast(Map.class)
                .map(map -> {
                    // Convertir valores a Long
                    Map<String, Long> result = new java.util.HashMap<>();
                    map.forEach((k, v) -> {
                        if (v instanceof Number) {
                            result.put((String) k, ((Number) v).longValue());
                        }
                    });
                    return result;
                })
                .doOnError(e -> {
                    log.error("Error al obtener los lenguajes de {}/{}: {}", owner, repo, e.getMessage());
                    return Mono.just(Collections.emptyMap());
                });
    }
    
    /**
     * Obtiene detalles de un commit específico.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param sha Hash del commit
     * @return Mono con el commit
     */
    public Mono<Commit> getCommitDetail(String owner, String repo, String sha) {
        return webClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/commits/{sha}", owner, repo, sha)
                .headers(this::setAuthHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToDetailedCommit)
                .doOnError(e -> log.error("Error al obtener detalles del commit {}: {}", sha, e.getMessage()));
    }
    
    /**
     * Obtiene detalles de un Pull Request específico.
     */
    public Mono<PullRequest> getPullRequestDetail(String owner, String repo, int number) {
        return webClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/pulls/{number}", owner, repo, number)
                .headers(this::setAuthHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToPullRequest)
                .doOnError(e -> log.error("Error al obtener PR #{} en {}/{}: {}", number, owner, repo, e.getMessage()));
    }

    /**
     * Obtiene detalles de un Issue específico.
     */
    public Mono<Issue> getIssueDetail(String owner, String repo, int number) {
        return webClient.get()
                .uri(GITHUB_API_URL + "/repos/{owner}/{repo}/issues/{number}", owner, repo, number)
                .headers(this::setAuthHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToIssue)
                .doOnError(e -> log.error("Error al obtener Issue #{} en {}/{}: {}", number, owner, repo, e.getMessage()));
    }
    
    /**
     * Configura las cabeceras de autenticación si hay un token disponible.
     * 
     * @param headers Cabeceras HTTP
     */
    private void setAuthHeader(HttpHeaders headers) {
        if (githubToken != null && !githubToken.isEmpty()) {
            headers.setBearerAuth(githubToken);
        }
    }
    
    /**
     * Convierte un mapa de respuesta de la API a un objeto Repository.
     * 
     * @param map Mapa con datos del repositorio
     * @return Objeto Repository
     */
    private Repository mapToRepository(Map<String, Object> map) {
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
                .build();
    }
    
    /**
     * Convierte un mapa de respuesta de la API a un objeto Commit.
     * 
     * @param map Mapa con datos del commit
     * @return Objeto Commit
     */
    private Commit mapToCommit(Map<String, Object> map) {
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
    private Commit mapToDetailedCommit(Map<String, Object> map) {
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
                    .toList();
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
    private PullRequest mapToPullRequest(Map<String, Object> map) {
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
    private Issue mapToIssue(Map<String, Object> map) {
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
                    .toList();
        }
        
        // Mapear asignados
        List<Map<String, Object>> assigneesData = getNestedList(map, "assignees");
        List<String> assignees = null;
        if (assigneesData != null) {
            assignees = assigneesData.stream()
                    .map(assigneeMap -> getString(assigneeMap, "login"))
                    .toList();
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
    
    // Métodos de utilidad para extraer valores de mapas
    
    private String getString(Map<String, Object> map, String key) {
        return map != null && map.containsKey(key) ? String.valueOf(map.get(key)) : null;
    }
    
    private Integer getInt(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
    
    private Long getLong(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0L;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
    
    private Boolean getBoolean(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return false;
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : false;
    }
    
    private Instant getInstant(Map<String, Object> map, String key) {
        String dateStr = getString(map, key);
        return dateStr != null ? Instant.parse(dateStr) : null;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return Collections.emptyMap();
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getNestedList(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return Collections.emptyList();
        Object value = map.get(key);
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.emptyList();
    }
} 