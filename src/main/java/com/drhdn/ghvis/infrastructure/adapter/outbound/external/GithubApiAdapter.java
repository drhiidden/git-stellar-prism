package com.drhdn.ghvis.infrastructure.adapter.outbound.external;

import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.entity.Readme;
import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.CircuitBreakerService;
import com.drhdn.ghvis.infrastructure.adapter.outbound.error.ReactiveErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.core.ParameterizedTypeReference;
import java.time.Duration;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

/**
 * Servicio profesional para interactuar con la API de GitHub.
 */
@Service
@Slf4j
public class GithubApiAdapter {

    private final WebClient githubWebClient;
    private final WebClient publicWebClient;
    private final RateLimitService rateLimitService;
    private final CacheService cacheService;
    private final CircuitBreakerService circuitBreakerService;
    private final ReactiveErrorHandler errorHandler;
    private final GithubApiResponseMapper githubApiResponseMapper;

    @Value("${github.fallback-token:}")
    private String fallbackGithubToken;

    public GithubApiAdapter(@Qualifier("githubWebClient") WebClient githubWebClient,
                            @Qualifier("publicWebClient") WebClient publicWebClient,
                            RateLimitService rateLimitService,
                            CacheService cacheService,
                            CircuitBreakerService circuitBreakerService,
                            ReactiveErrorHandler errorHandler,
                            GithubApiResponseMapper githubApiResponseMapper) {
        this.githubWebClient = githubWebClient;
        this.publicWebClient = publicWebClient;
        this.rateLimitService = rateLimitService;
        this.cacheService = cacheService;
        this.circuitBreakerService = circuitBreakerService;
        this.errorHandler = errorHandler;
        this.githubApiResponseMapper = githubApiResponseMapper;
    }

    /**
     * Lista las operaciones de solo lectura permitidas para la validación de seguridad.
     *
     * @param operation Nombre de la operación a validar
     */
    private void validateReadOnlyOperation(String operation) {
        Set<String> allowedOperations = Set.of(
            "getRepository", "getRepositoryPublic", "getCommits", "getPullRequests",
            "getIssues", "getLanguages", "getCommitDetail", "getPullRequestDetail",
            "getIssueDetail", "getCurrentUser", "getUserByLogin", "getUserById",
            "getUserRepositories", "getRepositoryTree", "getReadme"
        );
        if (!allowedOperations.contains(operation)) {
            log.warn("Operación no permitida detectada: {}", operation);
            throw new SecurityException("Operación no permitida: " + operation);
        }
    }

    /**
     * Obtiene información de un repositorio con TODAS las integraciones.
     * 🔒 OPERACIÓN SEGURA: Solo lectura, incluye repositorios privados
     *
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Mono con la información del repositorio
     */
    public Mono<Repository> getRepository(String owner, String repo, Principal principal) {
        validateReadOnlyOperation("getRepository");
        
        String cacheKey = String.format("repository:%s:%s", owner, repo);
        String url = String.format("/repos/%s/%s", owner, repo);
        
        log.info("🔍 GitHub API: GET {} (repository) - Con integraciones completas", url);
        
        return cacheService.getOrFetch(cacheKey, () ->
            circuitBreakerService.executeWithCircuitBreaker("github-repository",
                githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(url).build())
                    .attributes(clientRegistrationId("github"))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(githubApiResponseMapper::mapToRepository)
                    .onErrorResume(errorHandler.handleGithubError()),
                // Fallback: repositorio vacío
                Mono.just(Repository.builder()
                    .name(repo)
                    .owner(owner)
                    .description("Repositorio no disponible")
                    .build())
            ),
            Duration.ofMinutes(30).getSeconds() // TTL de 30 minutos para repositorios
        )
        .doOnSubscribe(s -> log.debug("🚀 Iniciando obtención de repositorio {}/{} (cache + circuit breaker)", owner, repo))
        .doOnSuccess(repository -> log.info("✅ GitHub API: GET {} - Success (repository: {}) con integraciones", 
                                url, repository.getName()))
        .doOnError(error -> log.error("❌ GitHub API: GET {} - Error final: {}", url, error.getMessage()));
    }
    
    /**
     * Obtiene información de un repositorio sin autenticación (API pública).
     *
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Mono con la información del repositorio
     */
    public Mono<Repository> getRepositoryPublic(String owner, String repo) {
        String url = String.format("/repos/%s/%s", owner, repo);
        log.info("🔍 GitHub API: GET {} (public repository)", url);
        
        return publicWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .headers(this::setFallbackAuthHeader)
                .retrieve()
                .bodyToMono(Map.class)
                .map(githubApiResponseMapper::mapToRepository) // Usar el mapper
                .doOnSuccess(repository -> log.info("✅ GitHub API: GET {} - Success (public repository: {})", 
                                        url, repository.getName()))
                .doOnError(error -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, error.getMessage()))
                .doOnSubscribe(s -> log.debug("🚀 GitHub API: Iniciando llamada GET {} (public repository)", url))
                .doOnSuccess(repository -> log.debug("Repositorio público obtenido: {}/{}", owner, repo))
                .doOnError(e -> log.error("Error al obtener el repositorio público {}/{}: {}", owner, repo, e.getMessage()));
    }
    
    /**
     * Obtiene los commits de un repositorio con TODAS las integraciones:
     * - Circuit Breaker para resiliencia
     * - Caching reactivo para performance
     * - Rate Limiting inteligente
     * - Error Handling avanzado
     * - Backpressure management
     *
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Flux de commits con manejo completo de errores y resiliencia
     */
    public Flux<Commit> getCommits(String owner, String repo, Principal principal) {
        validateReadOnlyOperation("getCommits");
        
        String endpoint = String.format("/repos/%s/%s/commits", owner, repo);
        String cacheKey = String.format("commits:%s:%s", owner, repo);
        String url = String.format("/repos/%s/%s/commits?per_page=100", owner, repo);
        
        log.info("🔍 GitHub API: GET {} (commits) - Con integraciones completas", url);
        
        // Implementación con Circuit Breaker + Cache + Rate Limiting + Error Handling
        return rateLimitService.canMakeRequest(endpoint, principal)
            .flatMap(canMake -> {
                if (!canMake) {
                    return Mono.error(new RuntimeException("Rate limit excedido para commits"));
                }
                
                return cacheService.getOrFetch(cacheKey, () ->
                    circuitBreakerService.executeWithCircuitBreaker("github-commits",
                        githubWebClient.get()
                            .uri(uriBuilder -> uriBuilder.path(url).build())
                            .attributes(clientRegistrationId("github"))
                            .retrieve()
                            .bodyToFlux(Map.class) // Simplificado
                            .map(githubApiResponseMapper::mapToCommit)
                            .collectList() // Convertir a Mono<List<Commit>>
                            .onErrorResume(errorHandler.handleGithubError()),
                        // Fallback: lista vacía
                        Mono.just(Collections.<Commit>emptyList())
                    ),
                    Duration.ofMinutes(15).getSeconds()
                );
            })
            .flatMapMany(Flux::fromIterable) // Convertir List<Commit> a Flux<Commit>
            // Backpressure management
            .onBackpressureBuffer(200)
            .delayElements(Duration.ofMillis(5))
            .retryWhen(rateLimitService.createRetryPolicy(endpoint, principal))
        .doOnSubscribe(subscription -> 
            log.debug("🚀 Iniciando obtención de commits para {}/{} (con cache + circuit breaker)", owner, repo))
        .doOnNext(commit -> log.trace("📝 Commit procesado: {}", commit.getHash()))
        .doOnComplete(() -> log.info("✅ GitHub API: GET {} - Success (commits obtenidos con integraciones)", url))
        .doOnError(e -> log.error("❌ GitHub API: GET {} - Error final: {}", url, e.getMessage()));
    }
    
    /**
     * Obtiene los pull requests de un repositorio con integraciones completas.
     *
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Flux de pull requests
     */
    public Flux<PullRequest> getPullRequests(String owner, String repo, Principal principal) {
        validateReadOnlyOperation("getPullRequests");
        
        String cacheKey = String.format("pullrequests:%s:%s", owner, repo);
        String url = String.format("/repos/%s/%s/pulls?state=all&per_page=100", owner, repo);
        
        log.info("🔍 GitHub API: GET {} (pull requests) - Con integraciones", url);
        
        return cacheService.getOrFetch(cacheKey, () ->
            circuitBreakerService.executeWithCircuitBreaker("github-pullrequests",
                githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(url).build())
                    .attributes(clientRegistrationId("github"))
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .map(githubApiResponseMapper::mapToPullRequest)
                    .collectList() // Convertir a Mono<List<PullRequest>>
                    .onErrorResume(errorHandler.handleGithubError()),
                Mono.just(Collections.<PullRequest>emptyList())
            ),
            Duration.ofMinutes(10).getSeconds()
        )
        .flatMapMany(Flux::fromIterable) // Convertir List<PullRequest> a Flux<PullRequest>
        .onBackpressureBuffer(100)
        .delayElements(Duration.ofMillis(10))
        .doOnSubscribe(subscription -> 
            log.debug("🚀 Obteniendo pull requests para {}/{} (cache + circuit breaker)", owner, repo))
        .doOnComplete(() -> log.info("✅ GitHub API: GET {} - Success (pull requests con integraciones)", url))
        .doOnError(e -> log.error("❌ GitHub API: GET {} - Error final: {}", url, e.getMessage()));
    }
    
    /**
     * Obtiene los issues de un repositorio con integraciones completas.
     *
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Flux de issues
     */
    public Flux<Issue> getIssues(String owner, String repo, Principal principal) {
        validateReadOnlyOperation("getIssues");
        
        String cacheKey = String.format("issues:%s:%s", owner, repo);
        String url = String.format("/repos/%s/%s/issues?state=all&per_page=100", owner, repo);
        
        log.info("🔍 GitHub API: GET {} (issues) - Con integraciones", url);
        
        return cacheService.getOrFetch(cacheKey, () ->
            circuitBreakerService.executeWithCircuitBreaker("github-issues",
                githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(url).build())
                    .attributes(clientRegistrationId("github"))
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .filter(map -> !map.containsKey("pull_request")) // Filtrar PRs
                    .map(githubApiResponseMapper::mapToIssue)
                    .collectList() // Convertir a Mono<List<Issue>>
                    .onErrorResume(errorHandler.handleGithubError()),
                Mono.just(Collections.<Issue>emptyList())
            ),
            Duration.ofMinutes(10).getSeconds()
        )
        .flatMapMany(Flux::fromIterable) // Convertir List<Issue> a Flux<Issue>
        .onBackpressureBuffer(100)
        .delayElements(Duration.ofMillis(10))
        .doOnSubscribe(subscription -> 
            log.debug("🚀 Obteniendo issues para {}/{} (cache + circuit breaker)", owner, repo))
        .doOnComplete(() -> log.info("✅ GitHub API: GET {} - Success (issues con integraciones)", url))
        .doOnError(e -> log.error("❌ GitHub API: GET {} - Error final: {}", url, e.getMessage()));
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
                .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {}) // Tipado explícito
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
                .map(githubApiResponseMapper::mapToDetailedCommit) // Usar el mapper
                .doOnSuccess(commit -> log.debug("Detalles del commit {} obtenidos", sha))
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
    }
    
    /**
     * Obtiene detalles de un Pull Request específico.
     *
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param number Número del pull request
     * @param principal Principal del usuario autenticado
     * @return Mono con el pull request
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
                .map(githubApiResponseMapper::mapToPullRequest) // Usar el mapper
                .doOnSuccess(pr -> log.debug("Detalles del PR #{} obtenidos", number))
                .doOnError(e -> log.error("❌ GitHub API: GET {} - Error: {}", 
                                    url, e.getMessage()));
    }

    /**
     * Obtiene detalles de un Issue específico.
     *
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param number Número del issue
     * @param principal Principal del usuario autenticado
     * @return Mono con el issue
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
                .map(githubApiResponseMapper::mapToIssue) // Usar el mapper
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
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}) // Tipado explícito
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
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}) // Tipado explícito
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
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}) // Tipado explícito
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
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}) // Tipado explícito
                .map(resp -> {
                    @SuppressWarnings("unchecked") // GitHub API response casting
                    List<Map<String, Object>> tree = (List<Map<String, Object>>) resp.getOrDefault("tree", Collections.emptyList());
                    return tree;
                })
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
     * Obtiene el contenido del archivo README.md de un repositorio.
     * 🔒 OPERACIÓN SEGURA: Solo lectura
     *
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Principal del usuario autenticado
     * @return Mono con el contenido del README
     */
    public Mono<Readme> getReadme(String owner, String repo, Principal principal) {
        validateReadOnlyOperation("getReadme");
        String url = String.format("/repos/%s/%s/readme", owner, repo);
        log.info("🔍 GitHub API: GET {} (readme)", url);

        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(githubApiResponseMapper::mapToReadme) // Usar el método de mapeo
                .doOnSuccess(readme -> log.info("✅ GitHub API: GET {} - Success (readme obtenido)", url))
                .doOnError(error -> log.error("❌ GitHub API: GET {} - Error: {}",
                                    url, error.getMessage()));
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
        String url = "/user/repos?visibility=all&sort=updated&per_page=50";
        log.info("🔍 GitHub API: GET {} (user repositories)", url);
        
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("visibility", "all")
                        .queryParam("sort", "updated")
                        .queryParam("per_page", "50")
                        .build())
                .attributes(clientRegistrationId("github"))
                .retrieve()
                .bodyToFlux(Map.class)
                .map(githubApiResponseMapper::mapToRepository)
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
                                .owner(repo.getOwner()) // Corregido: usa el owner original del repo
                                .description(repo.getDescription())
                                .url(repo.getUrl())
                                .defaultBranch(repo.getDefaultBranch())
                                .createdAt(repo.getCreatedAt())
                                .updatedAt(repo.getUpdatedAt())
                                .pushedAt(repo.getPushedAt())
                                .stargazersCount(repo.getStargazersCount())
                                .forksCount(repo.getForksCount()) // Restaurado el campo 'fork'
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

} 