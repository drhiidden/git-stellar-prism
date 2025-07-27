package com.drhdn.ghvis.infrastructure.adapter.outbound.external;

import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.port.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

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
public class GithubApiAdapter {

    private final WebClient githubWebClient;
    private final WebClient publicWebClient;
    private final RateLimitService rateLimitService;
    
    @Value("${github.api.token:}")
    private String fallbackGithubToken;
    
    /**
     * Constructor que inyecta correctamente los WebClients configurados.
     */
    public GithubApiAdapter(@Qualifier("githubWebClient") WebClient githubWebClient, 
                        @Qualifier("webClient") WebClient publicWebClient,
                        RateLimitService rateLimitService) {
        this.githubWebClient = githubWebClient;
        this.publicWebClient = publicWebClient;
        this.rateLimitService = rateLimitService;
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
            "getUserRepositories", "hasRepositoryAccess", "getCurrentUser",
            "getUserByLogin", "getUserById", "getRepositoryTree"
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
        String url = String.format("/repos/%s/%s", owner, repo);
        log.info("🔍 GitHub API: GET {} (repository)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map)
                .doOnSuccess(response -> log.info("✅ GitHub API: GET {} - Success (repository: {})", 
                                        url, response.get("name")))
                .doOnError(error -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, error.getMessage()))
                .doOnSubscribe(s -> log.debug("🚀 GitHub API: Iniciando llamada GET {} (repository)", url))
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
        String url = String.format("https://api.github.com/repos/%s/%s", owner, repo);
        log.info("🔍 GitHub API: GET {} (public repository)", url);
        
        return publicWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .headers(this::setFallbackAuthHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map)
                .doOnSuccess(response -> log.info("✅ GitHub API: GET {} - Success (public repository: {})", 
                                        url, response.get("name")))
                .doOnError(error -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, error.getMessage()))
                .doOnSubscribe(s -> log.debug("🚀 GitHub API: Iniciando llamada GET {} (public repository)", url))
                .map(this::mapToRepository)
                .doOnSuccess(repository -> log.debug("Repositorio público obtenido: {}/{}", owner, repo))
                .doOnError(e -> log.error("Error al obtener el repositorio público {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene los commits de un repositorio usando OAuth2 con rate limiting integrado.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Flux de commits
     */
    public Flux<Commit> getCommits(String owner, String repo, Principal principal) {
        String endpoint = String.format("/repos/%s/%s/commits", owner, repo);
        String url = String.format("/repos/%s/%s/commits?per_page=100", owner, repo);
        log.info("🔍 GitHub API: GET {} (commits)", url);
        
        return rateLimitService.canMakeRequest(endpoint, principal)
            .flatMapMany(canMake -> {
                if (!canMake) {
                    return Flux.error(new RuntimeException("Rate limit excedido para commits"));
                }
                
                return githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(url)
                            .build())
                    .attributes(clientRegistrationId("github"))
                    .retrieve()
                    .toEntityFlux(Map.class)
                    .flatMapMany(response -> {
                        // Actualizar rate limits basado en headers
                        rateLimitService.updateLimits(endpoint, principal, response.getHeaders())
                            .subscribe();
                        
                        return response.getBody();
                    })
                    .map(this::mapToCommit)
                    .retryWhen(rateLimitService.createRetryPolicy(endpoint, principal))
                    .doOnSubscribe(subscription -> 
                        log.debug("Obteniendo commits para repositorio: {}/{}", owner, repo))
                    .doOnComplete(() -> log.info("✅ GitHub API: GET {} - Success (commits obtenidos)", url))
                    .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
            });
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
        String url = String.format("/repos/%s/%s/pulls?state=all&per_page=100", owner, repo);
        log.info("🔍 GitHub API: GET {} (pull requests)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToPullRequest)
                .doOnSubscribe(subscription -> 
                    log.debug("Obteniendo pull requests para repositorio: {}/{}", owner, repo))
                .doOnComplete(() -> log.info("✅ GitHub API: GET {} - Success (pull requests obtenidos)", url))
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
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
        String url = String.format("/repos/%s/%s/issues?state=all&per_page=100", owner, repo);
        log.info("🔍 GitHub API: GET {} (issues)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToFlux(Map.class)
                .filter(map -> !map.containsKey("pull_request")) // Filtrar PRs que también aparecen como issues
                .map(this::mapToIssue)
                .doOnSubscribe(subscription -> 
                    log.debug("Obteniendo issues para repositorio: {}/{}", owner, repo))
                .doOnComplete(() -> log.info("✅ GitHub API: GET {} - Success (issues obtenidos)", url))
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
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
        String url = String.format("/repos/%s/%s/languages", owner, repo);
        log.info("🔍 GitHub API: GET {} (languages)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
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
                .doOnSuccess(languages -> {
                    log.debug("Lenguajes obtenidos para {}/{}: {}", owner, repo, languages.keySet());
                    log.info("✅ GitHub API: GET {} - Success (languages obtenidos)", url);
                })
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()))
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
        String url = String.format("/repos/%s/%s/commits/%s", owner, repo, sha);
        log.info("🔍 GitHub API: GET {} (commit detail)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToDetailedCommit)
                .doOnSuccess(commit -> log.debug("Detalles del commit {} obtenidos", sha))
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
    }
    
    /**
     * Obtiene detalles de un Pull Request específico.
     */
    public Mono<PullRequest> getPullRequestDetail(String owner, String repo, int number, Principal principal) {
        String url = String.format("/repos/%s/%s/pulls/%d", owner, repo, number);
        log.info("🔍 GitHub API: GET {} (pull request detail)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToPullRequest)
                .doOnSuccess(pr -> log.debug("Detalles del PR #{} obtenidos", number))
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
    }

    /**
     * Obtiene detalles de un Issue específico.
     */
    public Mono<Issue> getIssueDetail(String owner, String repo, int number, Principal principal) {
        String url = String.format("/repos/%s/%s/issues/%d", owner, repo, number);
        log.info("🔍 GitHub API: GET {} (issue detail)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToIssue)
                .doOnSuccess(issue -> log.debug("Detalles del Issue #{} obtenidos", number))
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
    }
    
    /**
     * Obtiene información del usuario autenticado actual.
     * 🔒 OPERACIÓN SEGURA: Solo lectura
     * 
     * @param principal Principal del usuario autenticado
     * @return Mono con la información del usuario actual
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getCurrentUser(Principal principal) {
        validateReadOnlyOperation("getCurrentUser");
        String url = "/user";
        log.info("🔍 GitHub API: GET {} (current user)", url);
        
        return githubWebClient
            .get()
            .uri(uriBuilder -> uriBuilder
                    .path(url)
                    .build())
            .attributes(clientRegistrationId("github"))
            .retrieve()
            .bodyToMono(Map.class)
            .map(map -> (Map<String, Object>) map)
            .doOnSuccess(user -> log.info("✅ GitHub API: GET {} - Success (user: {})", 
                                        url, user.get("login")))
            .doOnError(error -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, error.getMessage()))
            .doOnSubscribe(s -> log.debug("🚀 GitHub API: Iniciando llamada GET {} (current user)", url));
    }
    
    /**
     * Obtiene información de un usuario específico por su login.
     * 🔒 OPERACIÓN SEGURA: Solo lectura
     * 
     * @param login Login del usuario
     * @param principal Principal del usuario autenticado (para rate limiting)
     * @return Mono con la información del usuario
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getUserByLogin(String login, Principal principal) {
        validateReadOnlyOperation("getUserByLogin");
        String url = String.format("/users/%s", login);
        log.info("🔍 GitHub API: GET {} (user by login)", url);
        
        return githubWebClient
            .get()
            .uri(uriBuilder -> uriBuilder
                    .path(url)
                    .build())
            .attributes(clientRegistrationId("github"))
            .retrieve()
            .bodyToMono(Map.class)
            .map(map -> (Map<String, Object>) map)
            .doOnSuccess(user -> log.info("✅ GitHub API: GET {} - Success (user: {})", 
                                        url, user.get("login")))
            .doOnError(error -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, error.getMessage()))
            .doOnSubscribe(s -> log.debug("🚀 GitHub API: Iniciando llamada GET {} (user by login)", url));
    }
    
    /**
     * Obtiene información de un usuario específico por su ID.
     * 🔒 OPERACIÓN SEGURA: Solo lectura
     * 
     * @param userId ID del usuario
     * @param principal Principal del usuario autenticado (para rate limiting)
     * @return Mono con la información del usuario
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getUserById(Long userId, Principal principal) {
        validateReadOnlyOperation("getUserById");
        String url = String.format("/user/%d", userId);
        log.info("🔍 GitHub API: GET {} (user by id)", url);
        
        return githubWebClient
            .get()
            .uri(uriBuilder -> uriBuilder
                    .path(url)
                    .build())
            .attributes(clientRegistrationId("github"))
            .retrieve()
            .bodyToMono(Map.class)
            .map(map -> (Map<String, Object>) map)
            .doOnSuccess(user -> log.info("✅ GitHub API: GET {} - Success (user: {})", 
                                        url, user.get("login")))
            .doOnError(error -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, error.getMessage()))
            .doOnSubscribe(s -> log.debug("🚀 GitHub API: Iniciando llamada GET {} (user by id)", url));
    }

    /**
     * Obtiene el árbol de archivos completo de un repositorio de forma recursiva (branch por defecto = main).
     * Solo lectura ✓
     */
    public Mono<List<Map<String, Object>>> getRepositoryTree(String owner, String repo, String branch, Principal principal) {
        validateReadOnlyOperation("getRepositoryTree");
        String branchRef = branch != null && !branch.isBlank() ? branch : "HEAD";
        String url = String.format("/repos/%s/%s/git/trees/%s?recursive=1", owner, repo, branchRef);
        log.info("🔍 GitHub API: GET {} (repo tree)", url);

        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map)
                .map(resp -> (List<Map<String, Object>>) resp.getOrDefault("tree", Collections.emptyList()))
                .doOnSuccess(list -> log.info("✅ GitHub API: GET {} - Success ({} nodes)", url, list.size()))
                .doOnError(error -> log.error("❌ GitHub API: GET {} - Error: {}", url, error.getMessage()));
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
        String url = "/user/repos?visibility=all&sort=updated&per_page=100";
        log.info("🔍 GitHub API: GET {} (user repositories)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToRepository)
                .doOnSubscribe(subscription -> 
                    log.debug("Obteniendo repositorios del usuario autenticado"))
                .doOnComplete(() -> log.info("✅ GitHub API: GET {} - Success (user repositories obtenidos)", url))
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
    }

    /**
     * Obtiene los repositorios del usuario con información detallada incluyendo lenguajes.
     * 🔒 OPERACIÓN SEGURA: Solo lectura, incluye repositorios privados
     * 
     * @param principal Principal del usuario autenticado
     * @return Flux de repositorios del usuario con información extendida
     */
    public Flux<Repository> getUserRepositoriesWithDetails(Principal principal) {
        validateReadOnlyOperation("getUserRepositories"); // 🔒 Salvaguarda de seguridad
        return getUserRepositories(principal)
                .flatMap(repo -> 
                    getLanguages(repo.getOwner(), repo.getName(), principal)
                        .defaultIfEmpty(Collections.emptyMap())
                        .map(languages -> {
                            // Crear una versión mejorada del repositorio con lenguajes
                            Repository enhancedRepo = Repository.builder()
                                .id(repo.getId())
                                .name(repo.getName())
                                .owner(repo.getOwner())
                                .description(repo.getDescription())
                                .url(repo.getUrl())
                                .defaultBranch(repo.getDefaultBranch())
                                .createdAt(repo.getCreatedAt())
                                .updatedAt(repo.getUpdatedAt())
                                .pushedAt(repo.getPushedAt())
                                .stargazersCount(repo.getStargazersCount())
                                .forksCount(repo.getForksCount())
                                .watchersCount(repo.getWatchersCount())
                                .openIssuesCount(repo.getOpenIssuesCount())
                                .size(repo.getSize())
                                .fork(repo.isFork())
                                .isPrivate(repo.isPrivate())
                                .archived(repo.isArchived())
                                .languageDistribution(languages)
                                .topics(repo.getTopics()) // Incluir topics del repositorio original
                                .build();
                            return enhancedRepo;
                        })
                        .onErrorReturn(repo) // Si falla la obtención de lenguajes, devolver repo básico
                )
                .doOnNext(repo -> log.debug("Repositorio con detalles obtenido: {}", repo.getName()))
                .doOnError(e -> log.error("Error al obtener repositorios con detalles: {}", e.getMessage()));
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
                .topics(getStringList(map, "topics"))
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
    
    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return Collections.emptyList();
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .filter(item -> item instanceof String)
                    .map(String.class::cast)
                    .toList();
        }
        return Collections.emptyList();
    }
} 