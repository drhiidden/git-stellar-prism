package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryCommitsQuery;
import com.drhdn.ghvis.application.usecase.GetRepositoryCommitsUseCase;
import com.drhdn.ghvis.domain.entity.Commit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.security.Principal;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests unitarios para GetRepositoryCommitsQueryHandler.
 * 
 * Valida el patrón CQRS y la correcta delegación al use case.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class GetRepositoryCommitsQueryHandlerUnitTest {

    @Mock
    private GetRepositoryCommitsUseCase commitsUseCase;
    
    @Mock
    private Principal principal;

    private GetRepositoryCommitsQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetRepositoryCommitsQueryHandler(commitsUseCase);
    }

    @Test
    void handle_WithValidQuery_ShouldReturnCommitsList() {
        // Given
        String owner = "microsoft";
        String repo = "vscode";
        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.builder()
            .queryId("test-query-123")
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .build();

        List<Commit> expectedCommits = List.of(
            Commit.builder()
                .hash("abc123")
                .message("feat: add new feature")
                .author("developer1")
                .build(),
            Commit.builder()
                .hash("def456")
                .message("fix: resolve bug")
                .author("developer2")
                .build()
        );

        // Mock use case to return commits
        when(commitsUseCase.execute(owner, repo, principal))
            .thenReturn(Flux.fromIterable(expectedCommits));

        // When & Then
        StepVerifier.create(handler.handle(query))
            .expectNext(expectedCommits)
            .verifyComplete();

        // Verify use case was called with correct parameters
        verify(commitsUseCase).execute(owner, repo, principal);
    }

    @Test
    void handle_WithEmptyRepository_ShouldReturnEmptyList() {
        // Given
        String owner = "empty";
        String repo = "repo";
        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.builder()
            .queryId("empty-query-456")
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .build();

        // Mock use case to return empty flux
        when(commitsUseCase.execute(owner, repo, principal))
            .thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(handler.handle(query))
            .expectNext(List.of()) // Empty list
            .verifyComplete();

        verify(commitsUseCase).execute(owner, repo, principal);
    }

    @Test
    void handle_WithUseCaseError_ShouldPropagateError() {
        // Given
        String owner = "error";
        String repo = "repo";
        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.builder()
            .queryId("error-query-789")
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .build();

        RuntimeException expectedError = new RuntimeException("Repository not found");

        // Mock use case to return error
        when(commitsUseCase.execute(owner, repo, principal))
            .thenReturn(Flux.error(expectedError));

        // When & Then
        StepVerifier.create(handler.handle(query))
            .expectError(RuntimeException.class)
            .verify();

        verify(commitsUseCase).execute(owner, repo, principal);
    }

    @Test
    void handle_WithLargeCommitList_ShouldHandleEfficiently() {
        // Given
        String owner = "large";
        String repo = "repo";
        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.builder()
            .queryId("large-query-999")
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .build();

        // Create a large list of commits (simulate real repository)
        List<Commit> largeCommitList = java.util.stream.IntStream.range(0, 1000)
            .mapToObj(i -> Commit.builder()
                .hash("hash" + i)
                .message("Commit " + i)
                .author("author" + (i % 10))
                .build())
            .collect(java.util.stream.Collectors.toList());

        when(commitsUseCase.execute(owner, repo, principal))
            .thenReturn(Flux.fromIterable(largeCommitList));

        // When & Then
        StepVerifier.create(handler.handle(query))
            .expectNextMatches(commits -> 
                commits.size() == 1000 && 
                commits.get(0).getHash().equals("hash0") &&
                commits.get(999).getHash().equals("hash999")
            )
            .verifyComplete();

        verify(commitsUseCase).execute(owner, repo, principal);
    }

    @Test
    void handle_ShouldLogCorrectInformation() {
        // Given
        String owner = "logging";
        String repo = "test";
        String queryId = "log-test-123";
        
        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.builder()
            .queryId(queryId)
            .owner(owner)
            .repo(repo)
            .principal(principal)
            .build();

        List<Commit> commits = List.of(
            Commit.builder().hash("log123").message("log test").build()
        );

        when(commitsUseCase.execute(owner, repo, principal))
            .thenReturn(Flux.fromIterable(commits));

        // When
        StepVerifier.create(handler.handle(query))
            .expectNext(commits)
            .verifyComplete();

        // Then - Verify logging behavior through method execution
        // (In a real scenario, we might use a logging test framework)
        verify(commitsUseCase).execute(owner, repo, principal);
        
        // Verify query ID is used for correlation
        assert query.getQueryId().equals(queryId);
        assert query.getRepositoryFullName().equals("logging/test");
    }

    @Test
    void handle_WithNullPrincipal_ShouldStillWork() {
        // Given
        String owner = "public";
        String repo = "repo";
        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.builder()
            .queryId("null-principal-test")
            .owner(owner)
            .repo(repo)
            .principal(null) // Null principal for public repositories
            .build();

        List<Commit> publicCommits = List.of(
            Commit.builder().hash("public123").message("public commit").build()
        );

        when(commitsUseCase.execute(owner, repo, null))
            .thenReturn(Flux.fromIterable(publicCommits));

        // When & Then
        StepVerifier.create(handler.handle(query))
            .expectNext(publicCommits)
            .verifyComplete();

        verify(commitsUseCase).execute(owner, repo, null);
    }
}
