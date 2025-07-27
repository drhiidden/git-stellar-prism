package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.port.CommitRepository;
import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.domain.entity.Commit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

/**
 * Adapter de infraestructura para CommitRepository usando GitHub API.
 * 
 * Implementa el puerto CommitRepository utilizando WebClient
 * y GitHub API, incluyendo rate limiting y manejo de errores.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubCommitRepositoryAdapter implements CommitRepository {
    
    private final WebClient githubWebClient;
    private final RateLimitService rateLimitService;
    
    @Override
    public Flux<Commit> findByRepository(String owner, String repo, Principal principal) {
        String endpoint = String.format("/repos/%s/%s/commits", owner, repo);
        
        return githubWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/repos/{owner}/{repo}/commits")
                .queryParam("per_page", 100)
                .build(owner, repo))
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
            .doOnError(e -> log.error("Error al obtener los commits de {}/{}: {}", 
                owner, repo, e.getMessage()));
    }
    
    @Override
    public Mono<Commit> findBySha(String owner, String repo, String sha, Principal principal) {
        String endpoint = String.format("/repos/%s/%s/commits/%s", owner, repo, sha);
        
        return githubWebClient.get()
            .uri("/repos/{owner}/{repo}/commits/{sha}", owner, repo, sha)
            .attributes(clientRegistrationId("github"))
            .retrieve()
            .bodyToMono(Map.class)
            .flatMap(response -> {
                // Actualizar rate limits basado en headers
                rateLimitService.updateLimits(endpoint, principal, new HttpHeaders())
                    .subscribe();
                
                return Mono.just(response);
            })
            .map(this::mapToDetailedCommit)
            .retryWhen(rateLimitService.createRetryPolicy(endpoint, principal))
            .doOnSubscribe(subscription -> 
                log.debug("Obteniendo detalles de commit: {}/{}:{}", owner, repo, sha))
            .doOnError(e -> log.error("Error al obtener detalles del commit {}/{}:{}: {}", 
                owner, repo, sha, e.getMessage()));
    }
    
    @Override
    public Mono<Boolean> hasAccess(String owner, String repo, Principal principal) {
        String endpoint = String.format("/repos/%s/%s", owner, repo);
        
        return githubWebClient.get()
            .uri("/repos/{owner}/{repo}", owner, repo)
            .attributes(clientRegistrationId("github"))
            .retrieve()
            .bodyToMono(Map.class)
            .flatMap(response -> {
                // Actualizar rate limits basado en headers
                rateLimitService.updateLimits(endpoint, principal, new HttpHeaders())
                    .subscribe();
                
                return Mono.just(true);
            })
            .retryWhen(rateLimitService.createRetryPolicy(endpoint, principal))
            .onErrorReturn(false)
            .doOnSubscribe(subscription -> 
                log.debug("Verificando acceso a repositorio: {}/{}", owner, repo));
    }
    
    /**
     * Mapea un Map a un objeto Commit básico.
     */
    private Commit mapToCommit(Map<String, Object> map) {
        return Commit.builder()
            .hash(getString(map, "sha"))
            .message(getString(map, "commit.message"))
            .author(getString(map, "commit.author.name"))
            .timestamp(getInstant(map, "commit.author.date"))
            .build();
    }
    
    /**
     * Mapea un Map a un objeto Commit detallado.
     */
    private Commit mapToDetailedCommit(Map<String, Object> map) {
        return Commit.builder()
            .hash(getString(map, "sha"))
            .message(getString(map, "commit.message"))
            .author(getString(map, "commit.author.name"))
            .timestamp(getInstant(map, "commit.author.date"))
            .build();
    }
    
    // Métodos auxiliares para mapeo
    private String getString(Map<String, Object> map, String key) {
        Object value = getNestedValue(map, key);
        return value != null ? value.toString() : null;
    }
    
    private java.time.Instant getInstant(Map<String, Object> map, String key) {
        Object value = getNestedValue(map, key);
        if (value instanceof String) {
            try {
                return java.time.Instant.parse((String) value);
            } catch (Exception e) {
                log.warn("Error parsing instant for key {}: {}", key, value);
                return null;
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String key) {
        String[] keys = key.split("\\.");
        Object current = map;
        
        for (String k : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(k);
            } else {
                return null;
            }
        }
        
        return current;
    }
} 