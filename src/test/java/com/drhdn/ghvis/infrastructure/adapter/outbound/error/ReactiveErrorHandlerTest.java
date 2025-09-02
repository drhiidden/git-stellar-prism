package com.drhdn.ghvis.infrastructure.adapter.outbound.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Tests unitarios para ReactiveErrorHandler.
 * 
 * Valida el manejo correcto de diferentes tipos de errores:
 * - Errores HTTP específicos (404, 401, 403, 429, 500)
 * - Errores de red y timeouts
 * - Errores de validación
 * - Errores de OAuth2
 * - Errores de cache
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
class ReactiveErrorHandlerTest {

    private ReactiveErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = new ReactiveErrorHandler();
    }

    @Test
    void handleGithubError_With404NotFound_ShouldReturnRepositoryNotFoundException() {
        // Given
        WebClientResponseException notFoundException = WebClientResponseException.create(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(notFoundException))
            .expectError(RepositoryNotFoundException.class)
            .verify();
    }

    @Test
    void handleGithubError_With401Unauthorized_ShouldReturnTokenExpiredException() {
        // Given
        WebClientResponseException unauthorizedException = WebClientResponseException.create(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(unauthorizedException))
            .expectError(TokenExpiredException.class)
            .verify();
    }

    @Test
    void handleGithubError_With403Forbidden_ShouldReturnRateLimitExceededException() {
        // Given
        WebClientResponseException forbiddenException = WebClientResponseException.create(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(forbiddenException))
            .expectError(RateLimitExceededException.class)
            .verify();
    }

    @Test
    void handleGithubError_With429TooManyRequests_ShouldReturnRateLimitWithRetryInfo() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "60");
        
        WebClientResponseException tooManyRequestsException = WebClientResponseException.create(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "Too Many Requests",
            headers,
            new byte[0],
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(tooManyRequestsException))
            .expectErrorMatches(throwable -> 
                throwable instanceof RateLimitExceededException &&
                throwable.getMessage().contains("60 segundos")
            )
            .verify();
    }

    @Test
    void handleGithubError_With429WithoutRetryAfter_ShouldUseDefaultWaitTime() {
        // Given
        WebClientResponseException tooManyRequestsException = WebClientResponseException.create(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "Too Many Requests",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(tooManyRequestsException))
            .expectErrorMatches(throwable -> 
                throwable instanceof RateLimitExceededException &&
                throwable.getMessage().contains("60 segundos") // Default wait time
            )
            .verify();
    }

    @Test
    void handleGithubError_With500InternalServerError_ShouldReturnGitHubApiException() {
        // Given
        WebClientResponseException serverErrorException = WebClientResponseException.create(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(serverErrorException))
            .expectError(GitHubApiException.class)
            .verify();
    }

    @Test
    void handleGithubError_With502BadGateway_ShouldReturnGitHubApiException() {
        // Given
        WebClientResponseException badGatewayException = WebClientResponseException.create(
            HttpStatus.BAD_GATEWAY.value(),
            "Bad Gateway",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(badGatewayException))
            .expectError(GitHubApiException.class)
            .verify();
    }

    @Test
    void handleGithubError_WithUnknownHttpStatus_ShouldReturnGenericGitHubApiException() {
        // Given
        WebClientResponseException unknownException = WebClientResponseException.create(
            HttpStatus.I_AM_A_TEAPOT.value(), // 418 - Unusual status code
            "I'm a teapot",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(unknownException))
            .expectErrorMatches(throwable -> 
                throwable instanceof GitHubApiException &&
                throwable.getMessage().contains("418")
            )
            .verify();
    }

    @Test
    void handleGithubError_WithNonWebClientException_ShouldReturnUnexpectedException() {
        // Given
        RuntimeException genericException = new RuntimeException("Network timeout");
        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(genericException))
            .expectError(UnexpectedException.class)
            .verify();
    }

    @Test
    void handleValidationError_WithIllegalArgumentException_ShouldReturnValidationException() {
        // Given
        IllegalArgumentException validationError = new IllegalArgumentException("Invalid repository name");
        Function<Throwable, Mono<String>> handler = errorHandler.handleValidationError();

        // When & Then
        StepVerifier.create(handler.apply(validationError))
            .expectError(ValidationException.class)
            .verify();
    }

    @Test
    void handleValidationError_WithNonValidationException_ShouldPassThrough() {
        // Given
        RuntimeException nonValidationError = new RuntimeException("Not a validation error");
        Function<Throwable, Mono<String>> handler = errorHandler.handleValidationError();

        // When & Then
        StepVerifier.create(handler.apply(nonValidationError))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void handleOAuth2Error_WithAccessDenied_ShouldReturnOAuth2AccessDeniedException() {
        // Given
        RuntimeException oauthError = new RuntimeException("access_denied: User denied access");
        Function<Throwable, Mono<String>> handler = errorHandler.handleOAuth2Error();

        // When & Then
        StepVerifier.create(handler.apply(oauthError))
            .expectError(OAuth2AccessDeniedException.class)
            .verify();
    }

    @Test
    void handleOAuth2Error_WithInvalidScope_ShouldReturnOAuth2ScopeException() {
        // Given
        RuntimeException oauthError = new RuntimeException("invalid_scope: Requested scope is invalid");
        Function<Throwable, Mono<String>> handler = errorHandler.handleOAuth2Error();

        // When & Then
        StepVerifier.create(handler.apply(oauthError))
            .expectError(OAuth2ScopeException.class)
            .verify();
    }

    @Test
    void handleOAuth2Error_WithGenericOAuth2Error_ShouldReturnOAuth2Exception() {
        // Given
        RuntimeException oauthError = new RuntimeException("OAuth2 configuration error");
        Function<Throwable, Mono<String>> handler = errorHandler.handleOAuth2Error();

        // When & Then
        StepVerifier.create(handler.apply(oauthError))
            .expectError(OAuth2Exception.class)
            .verify();
    }

    @Test
    void handleCacheError_WithCacheException_ShouldReturnEmpty() {
        // Given
        CacheException cacheError = new CacheException("Cache connection failed");
        Function<Throwable, Mono<String>> handler = errorHandler.handleCacheError();

        // When & Then
        StepVerifier.create(handler.apply(cacheError))
            .verifyComplete(); // Should return empty Mono
    }

    @Test
    void handleCacheError_WithNonCacheException_ShouldReturnCacheException() {
        // Given
        RuntimeException nonCacheError = new RuntimeException("Database connection failed");
        Function<Throwable, Mono<String>> handler = errorHandler.handleCacheError();

        // When & Then
        StepVerifier.create(handler.apply(nonCacheError))
            .expectError(CacheException.class)
            .verify();
    }

    @Test
    void combineHandlers_WithMultipleHandlers_ShouldApplyFirstMatchingHandler() {
        // Given
        Function<Throwable, Mono<String>> handler1 = errorHandler.handleValidationError();
        Function<Throwable, Mono<String>> handler2 = errorHandler.handleGithubError();
        Function<Throwable, Mono<String>> combinedHandler = 
            ReactiveErrorHandler.combineHandlers(handler1, handler2);

        IllegalArgumentException validationError = new IllegalArgumentException("Validation failed");

        // When & Then
        StepVerifier.create(combinedHandler.apply(validationError))
            .expectError(ValidationException.class)
            .verify();
    }

    @Test
    void combineHandlers_WithNoMatchingHandlers_ShouldReturnOriginalError() {
        // Given
        Function<Throwable, Mono<String>> handler1 = errorHandler.handleValidationError();
        Function<Throwable, Mono<String>> combinedHandler = 
            ReactiveErrorHandler.combineHandlers(handler1);

        RuntimeException nonValidationError = new RuntimeException("Not handled");

        // When & Then
        StepVerifier.create(combinedHandler.apply(nonValidationError))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void handleGithubError_WithComplexErrorScenario_ShouldHandleCorrectly() {
        // Given - Simulate a complex error scenario with custom headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Remaining", "0");
        headers.add("X-RateLimit-Reset", "1640995200");
        
        WebClientResponseException complexException = WebClientResponseException.create(
            HttpStatus.FORBIDDEN.value(),
            "API rate limit exceeded",
            headers,
            "Rate limit exceeded".getBytes(),
            StandardCharsets.UTF_8
        );

        Function<Throwable, Mono<String>> handler = errorHandler.handleGithubError();

        // When & Then
        StepVerifier.create(handler.apply(complexException))
            .expectErrorMatches(throwable -> 
                throwable instanceof RateLimitExceededException &&
                throwable.getMessage().contains("Rate limit excedido")
            )
            .verify();
    }
}
