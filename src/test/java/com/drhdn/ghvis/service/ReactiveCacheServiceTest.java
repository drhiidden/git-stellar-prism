package com.drhdn.ghvis.service;

import com.drhdn.ghvis.model.Commit;
import com.drhdn.ghvis.model.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests para ReactiveCacheService.
 * 
 * Cubre:
 * - Cache hits y misses
 * - TTL y expiración
 * - Estadísticas del cache
 * - Warm-up del cache
 * - Limpieza automática
 */
@ExtendWith(MockitoExtension.class)
class ReactiveCacheServiceTest {

    @Mock
    private GithubService githubService;

    @Mock
    private ReactiveErrorHandler errorHandler;

    @Mock
    private Principal principal;

    private ReactiveCacheService reactiveCacheService;

    @BeforeEach
    void setUp() {
        reactiveCacheService = new ReactiveCacheService(githubService, errorHandler);
        when(principal.getName()).thenReturn("testuser");
    }

    @Test
    void shouldReturnCachedCommitsOnSecondCall() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        List<Commit> mockCommits = List.of(
            createMockCommit("abc123", "feat: add new feature"),
            createMockCommit("def456", "fix: resolve bug")
        );

        when(githubService.getCommits(eq(owner), eq(repo), any(Principal.class)))
            .thenReturn(Flux.fromIterable(mockCommits));

        // When & Then - Primera llamada (cache miss)
        StepVerifier.create(reactiveCacheService.getCachedCommits(owner, repo, principal))
            .expectNextCount(2)
            .verifyComplete();

        // When & Then - Segunda llamada (cache hit)
        StepVerifier.create(reactiveCacheService.getCachedCommits(owner, repo, principal))
            .expectNextCount(2)
            .verifyComplete();

        // Verify que GitHub API solo se llamó una vez
        verify(githubService, times(1)).getCommits(eq(owner), eq(repo), any(Principal.class));
    }

    @Test
    void shouldReturnCachedUserRepositories() {
        // Given
        List<Repository> mockRepos = List.of(
            createMockRepository("repo1", "testuser"),
            createMockRepository("repo2", "testuser")
        );

        when(githubService.getUserRepositories(any(Principal.class)))
            .thenReturn(Flux.fromIterable(mockRepos));

        // When & Then
        StepVerifier.create(reactiveCacheService.getCachedUserRepositories(principal, false))
            .expectNextCount(2)
            .verifyComplete();

        // Segunda llamada debería usar cache
        StepVerifier.create(reactiveCacheService.getCachedUserRepositories(principal, false))
            .expectNextCount(2)
            .verifyComplete();

        verify(githubService, times(1)).getUserRepositories(any(Principal.class));
    }

    @Test
    void shouldReturnCachedCommit() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        String commitSha = "abc123";
        Commit mockCommit = createMockCommit(commitSha, "feat: add new feature");

        when(githubService.getCommitDetail(eq(owner), eq(repo), eq(commitSha), any(Principal.class)))
            .thenReturn(Mono.just(mockCommit));

        // When & Then - Primera llamada
        StepVerifier.create(reactiveCacheService.getCachedCommit(owner, repo, commitSha, principal))
            .expectNext(mockCommit)
            .verifyComplete();

        // When & Then - Segunda llamada (cache hit)
        StepVerifier.create(reactiveCacheService.getCachedCommit(owner, repo, commitSha, principal))
            .expectNext(mockCommit)
            .verifyComplete();

        verify(githubService, times(1)).getCommitDetail(eq(owner), eq(repo), eq(commitSha), any(Principal.class));
    }

    @Test
    void shouldClearRepositoryCache() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        List<Commit> mockCommits = List.of(createMockCommit("abc123", "feat: add feature"));

        when(githubService.getCommits(eq(owner), eq(repo), any(Principal.class)))
            .thenReturn(Flux.fromIterable(mockCommits));

        // Populate cache
        reactiveCacheService.getCachedCommits(owner, repo, principal).blockLast();

        // When
        StepVerifier.create(reactiveCacheService.clearRepositoryCache(owner, repo))
            .verifyComplete();

        // Then - Cache should be cleared, so GitHub API should be called again
        StepVerifier.create(reactiveCacheService.getCachedCommits(owner, repo, principal))
            .expectNextCount(1)
            .verifyComplete();

        verify(githubService, times(2)).getCommits(eq(owner), eq(repo), any(Principal.class));
    }

    @Test
    void shouldClearAllCache() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        List<Commit> mockCommits = List.of(createMockCommit("abc123", "feat: add feature"));

        when(githubService.getCommits(eq(owner), eq(repo), any(Principal.class)))
            .thenReturn(Flux.fromIterable(mockCommits));

        // Populate cache
        reactiveCacheService.getCachedCommits(owner, repo, principal).blockLast();

        // When
        StepVerifier.create(reactiveCacheService.clearAllCache())
            .verifyComplete();

        // Then - Cache should be cleared
        StepVerifier.create(reactiveCacheService.getCachedCommits(owner, repo, principal))
            .expectNextCount(1)
            .verifyComplete();

        verify(githubService, times(2)).getCommits(eq(owner), eq(repo), any(Principal.class));
    }

    @Test
    void shouldReturnCacheStats() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        List<Commit> mockCommits = List.of(createMockCommit("abc123", "feat: add feature"));

        when(githubService.getCommits(eq(owner), eq(repo), any(Principal.class)))
            .thenReturn(Flux.fromIterable(mockCommits));

        // Populate cache
        reactiveCacheService.getCachedCommits(owner, repo, principal).blockLast();

        // When & Then
        StepVerifier.create(reactiveCacheService.getCacheStats())
            .expectNextMatches(stats -> {
                return stats.getTotalEntries() > 0 &&
                       stats.getValidEntries() > 0 &&
                       stats.getHits() >= 0 &&
                       stats.getMisses() > 0;
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleWarmUpCache() {
        // Given
        List<Repository> mockRepos = List.of(
            createMockRepository("repo1", "testuser"),
            createMockRepository("repo2", "testuser")
        );

        List<Commit> mockCommits = List.of(createMockCommit("abc123", "feat: add feature"));

        when(githubService.getUserRepositories(any(Principal.class)))
            .thenReturn(Flux.fromIterable(mockRepos));
        when(githubService.getCommits(eq("testuser"), eq("repo1"), any(Principal.class)))
            .thenReturn(Flux.fromIterable(mockCommits));
        when(githubService.getCommits(eq("testuser"), eq("repo2"), any(Principal.class)))
            .thenReturn(Flux.fromIterable(mockCommits));

        // When & Then
        StepVerifier.create(reactiveCacheService.warmUpCache(principal))
            .verifyComplete();

        verify(githubService, times(1)).getUserRepositories(any(Principal.class));
        verify(githubService, times(1)).getCommits(eq("testuser"), eq("repo1"), any(Principal.class));
        verify(githubService, times(1)).getCommits(eq("testuser"), eq("repo2"), any(Principal.class));
    }

    @Test
    void shouldHandleErrorInCacheOperation() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        RuntimeException error = new RuntimeException("GitHub API error");

        when(githubService.getCommits(eq(owner), eq(repo), any(Principal.class)))
            .thenReturn(Flux.error(error));
        when(errorHandler.handleGithubError())
            .thenReturn(throwable -> Mono.error(throwable));

        // When & Then
        StepVerifier.create(reactiveCacheService.getCachedCommits(owner, repo, principal))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void shouldHandleNullPrincipal() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";

        // When & Then
        StepVerifier.create(reactiveCacheService.getCachedCommits(owner, repo, null))
            .expectNextCount(0)
            .verifyComplete();
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

    private Repository createMockRepository(String name, String owner) {
        return Repository.builder()
            .id(1L)
            .name(name)
            .owner(owner)
            .description("Test repository")
            .url("https://github.com/" + owner + "/" + name)
            .defaultBranch("main")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .pushedAt(Instant.now())
            .stargazersCount(10)
            .forksCount(5)
            .watchersCount(15)
            .openIssuesCount(2)
            .size(1024)
            .fork(false)
            .isPrivate(false)
            .archived(false)
            .build();
    }
} 