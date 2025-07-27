package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.model.Commit;
import com.drhdn.ghvis.service.CommitCacheService;
import com.drhdn.ghvis.service.GithubService;
import com.drhdn.ghvis.service.OAuth2UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private GithubService githubService;

    @MockBean
    private CommitCacheService commitCacheService;

    @MockBean
    private OAuth2UserService oAuth2UserService;

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnCommitsForValidRepository() {
        // Given
        String repoParam = "microsoft/vscode";
        List<Commit> mockCommits = List.of(
            createMockCommit("abc123", "feat: add new feature"),
            createMockCommit("def456", "fix: resolve bug in editor")
        );

        when(commitCacheService.getCommits(eq("microsoft"), eq("vscode"), any()))
            .thenReturn(Flux.fromIterable(mockCommits));

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

        when(githubService.getCommitDetail(eq("microsoft"), eq("vscode"), eq(commitSha), any()))
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
        
        when(commitCacheService.getCommits(eq("nonexistent"), eq("repo"), any()))
            .thenReturn(Flux.error(new RuntimeException("Repository not found")));

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
        
        when(commitCacheService.getCommits(eq("microsoft"), eq("vscode"), any()))
            .thenReturn(Flux.error(new RuntimeException("Rate limit exceeded")));

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

        when(commitCacheService.getCommits(eq("microsoft"), eq("vscode"), any()))
            .thenReturn(Flux.fromIterable(largeCommitList));

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

        when(commitCacheService.getCommits(eq("user"), eq("repo-with-special-chars"), any()))
            .thenReturn(Flux.fromIterable(mockCommits));

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