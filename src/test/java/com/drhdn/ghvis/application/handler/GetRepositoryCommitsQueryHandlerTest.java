package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryCommitsQuery;
import com.drhdn.ghvis.application.usecase.GetRepositoryCommitsUseCase;
import com.drhdn.ghvis.domain.entity.Commit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GetRepositoryCommitsQueryHandlerTest {

    @Mock
    private GetRepositoryCommitsUseCase useCase;

    @InjectMocks
    private GetRepositoryCommitsQueryHandler handler;

    private Principal principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "tester";
    }

    @Test
    @DisplayName("Debería devolver la lista de commits para un repositorio válido")
    void shouldReturnCommits() {
        // Arrange
        List<Commit> mockCommits = List.of(
                createMockCommit("abc123", "feat: feature 1"),
                createMockCommit("def456", "fix: bug 2")
        );
        when(useCase.execute(any(), any(), any())).thenReturn(Flux.fromIterable(mockCommits));

        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.create("owner", "repo", principal);

        // Act & Assert
        Mono<List<Commit>> resultMono = handler.handle(query);
        StepVerifier.create(resultMono)
                .expectNextMatches(list -> list.size() == 2 && list.get(0).getHash().equals("abc123"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería propagar errores del use case")
    void shouldPropagateError() {
        // Arrange
        when(useCase.execute(any(), any(), any()))
                .thenReturn(Flux.error(new RuntimeException("Error externo")));

        GetRepositoryCommitsQuery query = GetRepositoryCommitsQuery.create("owner", "repo", principal);

        // Act & Assert
        Mono<List<Commit>> resultMono = handler.handle(query);
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Error externo"))
                .verify();
    }

    private Commit createMockCommit(String hash, String message) {
        return Commit.builder()
                .hash(hash)
                .message(message)
                .author("tester")
                .authorEmail("tester@example.com")
                .timestamp(Instant.now())
                .branch("main")
                .build();
    }
} 