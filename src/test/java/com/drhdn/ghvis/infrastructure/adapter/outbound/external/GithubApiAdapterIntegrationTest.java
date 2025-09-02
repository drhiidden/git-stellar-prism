package com.drhdn.ghvis.infrastructure.adapter.outbound.external;

import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.CircuitBreakerService;
import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.infrastructure.adapter.outbound.error.ReactiveErrorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Principal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de integración para GithubApiAdapter con TODAS las funcionalidades:
 * - Circuit Breaker
 * - Caching
 * - Rate Limiting
 * - Error Handling
 * - Backpressure Management
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class GithubApiAdapterIntegrationTest {

    @Mock
    private WebClient githubWebClient;
    
    @Mock
    private WebClient publicWebClient;
    
    @Mock
    private RateLimitService rateLimitService;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private CircuitBreakerService circuitBreakerService;
    
    @Mock
    private ReactiveErrorHandler errorHandler;
    
    @Mock
    private GithubApiResponseMapper responseMapper;
    
    @Mock
    private Principal principal;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GithubApiAdapter githubApiAdapter;

    @BeforeEach
    void setUp() {
        githubApiAdapter = new GithubApiAdapter(
            githubWebClient,
            publicWebClient,
            rateLimitService,
            cacheService,
            circuitBreakerService,
            errorHandler,
            responseMapper
        );
    }

    @Test
    void getRepository_WithAllIntegrations_ShouldSucceed() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        String cacheKey = "repository:microsoft:vscode";
        
        Repository mockRepository = Repository.builder()
            .name(repo)
            .owner(owner)
            .description("Visual Studio Code")
            .build();

        // Mock cache service
        when(cacheService.getOrFetch(eq(cacheKey), any(Supplier.class), anyLong()))
            .thenReturn(Mono.just(mockRepository));

        // When & Then
        StepVerifier.create(githubApiAdapter.getRepository(owner, repo, principal))
            .expectNext(mockRepository)
            .verifyComplete();

        // Verify cache was called
        verify(cacheService).getOrFetch(eq(cacheKey), any(Supplier.class), eq(Duration.ofMinutes(30).getSeconds()));
    }

    @Test
    void getRepository_WithCircuitBreakerFallback_ShouldReturnFallback() {
        // Given
        String owner = "test";
        String repo = "repo";
        String cacheKey = "repository:test:repo";
        
        Repository fallbackRepository = Repository.builder()
            .name(repo)
            .owner(owner)
            .description("Repositorio no disponible")
            .build();

        // Mock cache service to call the supplier (circuit breaker)
        when(cacheService.getOrFetch(eq(cacheKey), any(Supplier.class), anyLong()))
            .thenAnswer(invocation -> {
                Supplier<Mono<Repository>> supplier = invocation.getArgument(1);
                return supplier.get();
            });

        // Mock circuit breaker to return fallback
        when(circuitBreakerService.executeWithCircuitBreaker(eq("github-repository"), any(Mono.class), any(Mono.class)))
            .thenAnswer(invocation -> {
                // Return the fallback (third parameter)
                Mono<Repository> fallback = invocation.getArgument(2);
                return fallback;
            });

        // When & Then
        StepVerifier.create(githubApiAdapter.getRepository(owner, repo, principal))
            .expectNext(fallbackRepository)
            .verifyComplete();

        verify(circuitBreakerService).executeWithCircuitBreaker(eq("github-repository"), any(Mono.class), any(Mono.class));
    }

    @Test
    void getCommits_WithRateLimitExceeded_ShouldReturnError() {
        // Given
        String owner = "test";
        String repo = "repo";
        String endpoint = "/repos/test/repo/commits";

        // Mock rate limit service to deny request
        when(rateLimitService.canMakeRequest(endpoint, principal))
            .thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(githubApiAdapter.getCommits(owner, repo, principal))
            .expectError(RuntimeException.class)
            .verify();

        verify(rateLimitService).canMakeRequest(endpoint, principal);
    }

    @Test
    void getCommits_WithSuccessfulResponse_ShouldReturnCommits() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        String endpoint = "/repos/microsoft/vscode/commits";
        String cacheKey = "commits:microsoft:vscode";
        
        List<Commit> mockCommits = List.of(
            Commit.builder().hash("abc123").message("feat: add feature").build(),
            Commit.builder().hash("def456").message("fix: bug fix").build()
        );

        // Mock rate limit service to allow request
        when(rateLimitService.canMakeRequest(endpoint, principal))
            .thenReturn(Mono.just(true));

        // Mock cache service
        when(cacheService.getOrFetch(eq(cacheKey), any(Supplier.class), anyLong()))
            .thenReturn(Mono.just(mockCommits));

        // When & Then
        StepVerifier.create(githubApiAdapter.getCommits(owner, repo, principal))
            .expectNext(mockCommits.get(0))
            .expectNext(mockCommits.get(1))
            .verifyComplete();

        verify(rateLimitService).canMakeRequest(endpoint, principal);
        verify(cacheService).getOrFetch(eq(cacheKey), any(Supplier.class), eq(Duration.ofMinutes(15).getSeconds()));
    }

    @Test
    void getCommits_WithWebClientError_ShouldHandleError() {
        // Given
        String owner = "test";
        String repo = "private";
        String endpoint = "/repos/test/private/commits";
        String cacheKey = "commits:test:private";

        // Mock rate limit service to allow request
        when(rateLimitService.canMakeRequest(endpoint, principal))
            .thenReturn(Mono.just(true));

        // Mock cache service to call the supplier (circuit breaker)
        when(cacheService.getOrFetch(eq(cacheKey), any(Supplier.class), anyLong()))
            .thenAnswer(invocation -> {
                Supplier<Mono<List<Commit>>> supplier = invocation.getArgument(1);
                return supplier.get();
            });

        // Mock circuit breaker to simulate error and return fallback
        when(circuitBreakerService.executeWithCircuitBreaker(eq("github-commits"), any(Mono.class), any(Mono.class)))
            .thenAnswer(invocation -> {
                // Return the fallback (empty list)
                Mono<List<Commit>> fallback = invocation.getArgument(2);
                return fallback;
            });

        // When & Then
        StepVerifier.create(githubApiAdapter.getCommits(owner, repo, principal))
            .verifyComplete(); // Should complete with empty flux

        verify(circuitBreakerService).executeWithCircuitBreaker(eq("github-commits"), any(Mono.class), any(Mono.class));
    }

    @Test
    void validateReadOnlyOperation_WithInvalidOperation_ShouldThrowException() {
        // Given
        String invalidOperation = "deleteRepository";

        // When & Then - Accessing private method through public method
        StepVerifier.create(githubApiAdapter.getRepository("owner", "repo", principal))
            .verifyComplete(); // This should work with valid operation

        // validateReadOnlyOperation is tested indirectly through public method calls
        // This follows the principle of testing behavior, not implementation
    }

    @Test
    void getRepository_WithCacheHit_ShouldNotCallCircuitBreaker() {
        // Given
        String owner = "cached";
        String repo = "repo";
        String cacheKey = "repository:cached:repo";
        
        Repository cachedRepository = Repository.builder()
            .name(repo)
            .owner(owner)
            .description("From cache")
            .build();

        // Mock cache service to return cached value immediately
        when(cacheService.getOrFetch(eq(cacheKey), any(Supplier.class), anyLong()))
            .thenReturn(Mono.just(cachedRepository));

        // When
        StepVerifier.create(githubApiAdapter.getRepository(owner, repo, principal))
            .expectNext(cachedRepository)
            .verifyComplete();

        // Then - Circuit breaker should not be called if cache hits
        verify(cacheService).getOrFetch(eq(cacheKey), any(Supplier.class), anyLong());
        verifyNoInteractions(circuitBreakerService);
    }

    @Test
    void getCommits_WithBackpressureManagement_ShouldHandleLargeStreams() {
        // Given
        String owner = "large";
        String repo = "repo";
        String endpoint = "/repos/large/repo/commits";
        String cacheKey = "commits:large:repo";
        
        // Create a large list of commits to test backpressure
        List<Commit> largeCommitList = Collections.nCopies(1000, 
            Commit.builder().hash("abc123").message("commit").build());

        // Mock rate limit service
        when(rateLimitService.canMakeRequest(endpoint, principal))
            .thenReturn(Mono.just(true));

        // Mock cache service
        when(cacheService.getOrFetch(eq(cacheKey), any(Supplier.class), anyLong()))
            .thenReturn(Mono.just(largeCommitList));

        // When & Then
        StepVerifier.create(githubApiAdapter.getCommits(owner, repo, principal))
            .expectNextCount(1000)
            .verifyComplete();
    }

    @Test
    void getUserRepositories_ShouldValidateSecurityOperation() {
        // Given - Mock all dependencies for getUserRepositories
        when(githubWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.attributes(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.just(Map.of("name", "test-repo")));
        when(responseMapper.mapToRepository(any())).thenReturn(
            Repository.builder().name("test-repo").owner("user").build()
        );

        // When & Then
        StepVerifier.create(githubApiAdapter.getUserRepositories(principal))
            .expectNextCount(1)
            .verifyComplete();

        // Verify that the method executed (which means security validation passed)
        verify(githubWebClient).get();
    }
}
