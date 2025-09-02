package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryTimelineQuery;
import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.entity.Issue;
import com.drhdn.ghvis.domain.entity.PullRequest;
import com.drhdn.ghvis.domain.entity.TimelineEvent;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.CommitRepository;
import com.drhdn.ghvis.domain.port.IssueRepository;
import com.drhdn.ghvis.domain.port.PullRequestRepository;
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

@SuppressWarnings("unchecked") // Mock testing requires unchecked casts
class GetRepositoryTimelineQueryHandlerTest {

    @Mock
    private CommitRepository commitRepository;
    @Mock
    private PullRequestRepository pullRequestRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private GetRepositoryTimelineQueryHandler handler;

    private Principal principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "tester";
    }

    @Test
    @DisplayName("Debe fusionar y ordenar eventos de commits, PRs e issues")
    void shouldMergeAndSortEvents() {
        // Arrange: commit ayer, PR hoy, issue anteayer
        Commit commit = Commit.builder()
                .hash("c1")
                .message("feat: commit")
                .author("dev")
                .timestamp(Instant.now().minusSeconds(86400))
                .stats(Commit.CommitStats.builder().additions(10).deletions(2).build())
                .build();

        PullRequest pr = PullRequest.builder()
                .number(1)
                .title("PR title")
                .author("dev")
                .timestamp(Instant.now())
                .stats(PullRequest.PrStats.builder().additions(5).deletions(1).build())
                .state("open")
                .build();

        Issue issue = Issue.builder()
                .number(2)
                .title("Issue title")
                .author("qa")
                .timestamp(Instant.now().minusSeconds(172800))
                .state("open")
                .build();

        when(commitRepository.findByRepository(any(), any(), any()))
                .thenReturn(Flux.just(commit));
        when(pullRequestRepository.findByRepository(any(), any(), any()))
                .thenReturn(Flux.just(pr));
        when(issueRepository.findByRepository(any(), any(), any()))
                .thenReturn(Flux.just(issue));

        // CacheService -> devolver directamente supplier
        when(cacheService.getOrFetch(any(), any()))
                .then(inv -> ((java.util.function.Supplier<Mono<List<TimelineEvent>>>) inv.getArgument(1)).get());

        GetRepositoryTimelineQuery query = GetRepositoryTimelineQuery.createFullTimeline("owner", "repo", principal);

        // Act & Assert
        StepVerifier.create(handler.handle(query))
                .expectNextMatches(list -> list.size() == 3 && list.get(0).getType() == TimelineEvent.EventType.PULL_REQUEST)
                .verifyComplete();
    }
} 