package com.drhdn.ghvis.service;

import com.drhdn.ghvis.model.Commit;
import com.drhdn.ghvis.model.Issue;
import com.drhdn.ghvis.model.PullRequest;
import com.drhdn.ghvis.model.Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

/**
 * Servicio profesional para interactuar con la API de GitHub.
 * 
 * 🔒 POLÍTICA DE SEGURIDAD ESTRICTA:
 * - SOLO operaciones de LECTURA (GET requests)
 * - NUNCA operaciones de escritura/eliminación
 * - Scope 'repo' usado ÚNICAMENTE para acceso a repositorios privados
 * - PROHIBIDO: POST, PUT, DELETE, PATCH requests
 * 
 * Esta implementación utiliza OAuth2 para autenticación automática
 * y sigue las mejores prácticas de Spring WebFlux.
 */
@Service
@Slf4j
public class GithubService {

    private final WebClient githubWebClient;
    private final WebClient publicWebClient;
    
    @Value("${github.api.token:}")
    private String fallbackGithubToken;
    
    /**
     * Constructor que inyecta correctamente los WebClients configurados.
     */
    public GithubService(@Qualifier("githubWebClient") WebClient githubWebClient, 
                        @Qualifier("webClient") WebClient publicWebClient) {
        this.githubWebClient = githubWebClient;
        this.publicWebClient = publicWebClient;
    }
    
    /**
     * 🔒 SALVAGUARDA DE SEGURIDAD: Valida que solo se realicen operaciones de lectura.
     * 
     * Este método está diseñado para prevenir accidentalmente operaciones peligrosas.
     * NUNCA debe ser modificado para permitir operaciones de escritura.
     * 
     * @param operation Nombre de la operación a validar
     * @throws SecurityException Si la operación no está en la lista de operaciones seguras
     */
    private void validateReadOnlyOperation(String operation) {
        // Lista blanca de operaciones permitidas (SOLO LECTURA)
        Set<String> allowedOperations = Set.of(
            "getRepository", "getRepositoryPublic", "getCommits", 
            "getPullRequests", "getIssues", "getLanguages",
            "getCommitDetail", "getPullRequestDetail", "getIssueDetail",
            "getUserRepositories", "hasRepositoryAccess"
        );
        
        if (!allowedOperations.contains(operation)) {
            String errorMsg = String.format(
                "OPERACIÓN BLOQUEADA: '%s' no está en la lista de operaciones seguras. " +
                "Solo se permiten operaciones de lectura.", operation
            );
            log.error("🚨 INTENTO DE OPERACIÓN PELIGROSA: {}", errorMsg);
            throw new SecurityException(errorMsg);
        }
        
        log.debug("✅ Operación segura validada: {}", operation);
    }
    
    /**
     * Obtiene información de un repositorio usando OAuth2 automático.
     * 🔒 OPERACIÓN SEGURA: Solo lectura, incluye repositorios privados
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado (opcional)
     * @return Mono con la información del repositorio
     */
    public Mono<Repository> getRepository(String owner, String repo, Principal principal) {
        validateReadOnlyOperation("getRepository"); // 🔒 Salvaguarda de seguridad
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToRepository)
                .doOnSuccess(repository -> log.debug("Repositorio obtenido: {}/{}", owner, repo))
                .doOnError(e -> log.error("Error al obtener el repositorio {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene información de un repositorio sin autenticación (API pública).
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Mono con la información del repositorio
     */
    public Mono<Repository> getRepositoryPublic(String owner, String repo) {
        return publicWebClient.get()
                .uri("https://api.github.com/repos/{owner}/{repo}", owner, repo)
                .headers(this::setFallbackAuthHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToRepository)
                .doOnSuccess(repository -> log.debug("Repositorio público obtenido: {}/{}", owner, repo))
                .doOnError(e -> log.error("Error al obtener el repositorio público {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene los commits de un repositorio usando OAuth2.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Flux de commits
     */
    public Flux<Commit> getCommits(String owner, String repo, Principal principal) {
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/commits")
                        .queryParam("per_page", 100)
                        .build(owner, repo))
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToCommit)
                .doOnSubscribe(subscription -> 
                    log.debug("Obteniendo commits para repositorio: {}/{}", owner, repo))
                .doOnError(e -> log.error("Error al obtener los commits de {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene los pull requests de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Flux de pull requests
     */
    public Flux<PullRequest> getPullRequests(String owner, String repo, Principal principal) {
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/pulls")
                        .queryParam("state", "all")
                        .queryParam("per_page", 100)
                        .build(owner, repo))
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToPullRequest)
                .doOnSubscribe(subscription -> 
                    log.debug("Obteniendo pull requests para repositorio: {}/{}", owner, repo))
                .doOnError(e -> log.error("Error al obtener los PRs de {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene los issues de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Flux de issues
     */
    public Flux<Issue> getIssues(String owner, String repo, Principal principal) {
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues")
                        .queryParam("state", "all")
                        .queryParam("per_page", 100)
                        .build(owner, repo))
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToFlux(Map.class)
                .filter(map -> !map.containsKey("pull_request")) // Filtrar PRs que también aparecen como issues
                .map(this::mapToIssue)
                .doOnSubscribe(subscription -> 
                    log.debug("Obteniendo issues para repositorio: {}/{}", owner, repo))
                .doOnError(e -> log.error("Error al obtener los issues de {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene la distribución de lenguajes en un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Mono con mapa de lenguajes y bytes
     */
    public Mono<Map<String, Long>> getLanguages(String owner, String repo, Principal principal) {
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/languages", owner, repo)
                .attributes(clientRegistrationId("github"))
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
                .doOnSuccess(languages -> log.debug("Lenguajes obtenidos para {}/{}: {}", owner, repo, languages.keySet()))
                .doOnError(e -> log.error("Error al obtener los lenguajes de {}/{}: {}", owner, repo, e.getMessage()))
                .onErrorResume(e -> Mono.just(Collections.emptyMap()));
    }
    
    /**
     * Obtiene detalles de un commit específico.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param sha Hash del commit
     * @param principal Principal del usuario autenticado
     * @return Mono con el commit
     */
    public Mono<Commit> getCommitDetail(String owner, String repo, String sha, Principal principal) {
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/commits/{sha}", owner, repo, sha)
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToDetailedCommit)
                .doOnSuccess(commit -> log.debug("Detalles del commit {} obtenidos", sha))
                .doOnError(e -> log.error("Error al obtener detalles del commit {}: {}", sha, e.getMessage()));
    }
    
    /**
     * Obtiene detalles de un Pull Request específico.
     */
    public Mono<PullRequest> getPullRequestDetail(String owner, String repo, int number, Principal principal) {
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", owner, repo, number)
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToPullRequest)
                .doOnSuccess(pr -> log.debug("Detalles del PR #{} obtenidos", number))
                .doOnError(e -> log.error("Error al obtener PR #{} en {}/{}: {}", number, owner, repo, e.getMessage()));
    }

    /**
     * Obtiene detalles de un Issue específico.
     */
    public Mono<Issue> getIssueDetail(String owner, String repo, int number, Principal principal) {
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/issues/{number}", owner, repo, number)
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToIssue)
                .doOnSuccess(issue -> log.debug("Detalles del Issue #{} obtenidos", number))
                .doOnError(e -> log.error("Error al obtener Issue #{} en {}/{}: {}", number, owner, repo, e.getMessage()));
    }
    
    /**
     * Establece el header de autorización con el token de fallback.
     * Solo se usa para APIs públicas cuando no hay OAuth2 disponible.
     * 
     * @param headers Headers HTTP
     */
    private void setFallbackAuthHeader(HttpHeaders headers) {
        if (fallbackGithubToken != null && !fallbackGithubToken.isEmpty()) {
            headers.setBearerAuth(fallbackGithubToken);
            log.debug("Usando token de fallback para autenticación");
        }
    }
    
    /**
     * Obtiene los repositorios del usuario autenticado.
     * 🔒 OPERACIÓN SEGURA: Solo lectura, incluye repositorios privados
     * 
     * @param principal Principal del usuario autenticado
     * @return Flux de repositorios del usuario
     */
    public Flux<Repository> getUserRepositories(Principal principal) {
        validateReadOnlyOperation("getUserRepositories"); // 🔒 Salvaguarda de seguridad
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("visibility", "all")
                        .queryParam("sort", "updated")
                        .queryParam("per_page", 100)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToRepository)
                .doOnSubscribe(subscription -> 
                    log.debug("Obteniendo repositorios del usuario autenticado"))
                .doOnError(e -> log.error("Error al obtener repositorios del usuario: {}", e.getMessage()));
    }
    
    /**
     * Verifica si el usuario tiene acceso a un repositorio específico.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Mono<Boolean> indicando si tiene acceso
     */
    public Mono<Boolean> hasRepositoryAccess(String owner, String repo, Principal principal) {
        return getRepository(owner, repo, principal)
                .map(repository -> true)
                .onErrorReturn(false)
                .doOnNext(hasAccess -> 
                    log.debug("Usuario {} acceso a {}/{}: {}", 
                        principal != null ? principal.getName() : "anónimo", 
                        owner, repo, hasAccess));
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