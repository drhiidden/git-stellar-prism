package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.application.handler.GetRepositoryCommitsQueryHandler;
import com.drhdn.ghvis.application.handler.GetRepositoryDetailQueryHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests reactivos para RepositoryController usando WebTestClient.
 * 
 * Estos tests demuestran el uso correcto de testing reactivo en Spring WebFlux.
 */
@WebFluxTest(RepositoryController.class)
class RepositoryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GetRepositoryCommitsQueryHandler getRepositoryCommitsQueryHandler;

    @MockBean
    private GetRepositoryDetailQueryHandler getRepositoryDetailQueryHandler;

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnCommitsForValidRepository() {
        // Given
        String repoParam = "microsoft/vscode";
        List<Commit> mockCommits = List.of(
            createMockCommit("abc123", "feat: add new feature"),
            createMockCommit("def456", "fix: resolve bug in editor")
        );

        when(getRepositoryCommitsQueryHandler.handle(any()))
            .thenReturn(reactor.core.publisher.Mono.just(mockCommits));

        // When & Then
        webTestClient.get()
            .uri("/api/repository/commits?repo=" + repoParam)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(Commit.class)
            .hasSize(2)
            .contains(mockCommits.toArray(new Commit[0]));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleInvalidRepositoryFormat() {
        // Given
        String invalidRepoParam = "invalid-format";

        // When & Then
        webTestClient.get()
            .uri("/api/repository/commits?repo=" + invalidRepoParam)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Parámetro 'repo' inválido. Debe ser 'owner/repo'.");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleEmptyRepositoryParameter() {
        // When & Then
        webTestClient.get()
            .uri("/api/repository/commits")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnCommitDetails() {
        // Given
        String repoParam = "microsoft/vscode";
        String commitSha = "abc123";
        Commit mockCommit = createMockCommit(commitSha, "feat: add new feature");

        when(getRepositoryDetailQueryHandler.handleCommitQuery(any()))
            .thenReturn(reactor.core.publisher.Mono.just(mockCommit));

        // When & Then
        webTestClient.get()
            .uri("/api/repository/details/commit/" + commitSha + "?repo=" + repoParam)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Commit.class)
            .isEqualTo(mockCommit);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleRepositoryNotFound() {
        // Given
        String repoParam = "nonexistent/repo";
        
        when(getRepositoryCommitsQueryHandler.handle(any()))
            .thenReturn(reactor.core.publisher.Mono.error(new RuntimeException("Repository not found")));

        // When & Then
        webTestClient.get()
            .uri("/api/repository/commits?repo=" + repoParam)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleRateLimitExceeded() {
        // Given
        String repoParam = "microsoft/vscode";
        
        when(getRepositoryCommitsQueryHandler.handle(any()))
            .thenReturn(reactor.core.publisher.Mono.error(new RuntimeException("Rate limit exceeded")));

        // When & Then
        webTestClient.get()
            .uri("/api/repository/commits?repo=" + repoParam)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    void shouldRequireAuthentication() {
        // When & Then - Sin autenticación
        webTestClient.get()
            .uri("/api/repository/commits?repo=microsoft/vscode")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleLargeRepository() {
        // Given
        String repoParam = "microsoft/vscode";
        List<Commit> largeCommitList = createLargeCommitList(100);

        when(getRepositoryCommitsQueryHandler.handle(any()))
            .thenReturn(reactor.core.publisher.Mono.just(largeCommitList));

        // When & Then
        webTestClient.get()
            .uri("/api/repository/commits?repo=" + repoParam)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Commit.class)
            .hasSize(100);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleSpecialCharactersInRepositoryName() {
        // Given
        String repoParam = "user/repo-with-special-chars";
        List<Commit> mockCommits = List.of(createMockCommit("abc123", "feat: special chars"));

        when(getRepositoryCommitsQueryHandler.handle(any()))
            .thenReturn(reactor.core.publisher.Mono.just(mockCommits));

        // When & Then
        webTestClient.get()
            .uri("/api/repository/commits?repo=" + repoParam)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Commit.class)
            .hasSize(1);
    }

    // Métodos de utilidad para crear datos de prueba

    private Commit createMockCommit(String hash, String message) {
        return Commit.builder()
            .hash(hash)
            .message(message)
            .author("Test Author")
            .authorEmail("test@example.com")
            .timestamp(Instant.now())
            .build();
    }

    private List<Commit> createLargeCommitList(int size) {
        return java.util.stream.IntStream.range(0, size)
            .mapToObj(i -> createMockCommit(
                "commit" + i, 
                "commit message " + i
            ))
            .toList();
    }
} 