package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryAnalysisQuery;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.LanguageRepository;
import com.drhdn.ghvis.domain.port.TechnologyRepository;
import com.drhdn.ghvis.domain.event.EventStore;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Principal;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked") // Mock testing requires unchecked casts
class GetRepositoryAnalysisQueryHandlerStructureTest {

    @Mock private LanguageRepository languageRepository;
    @Mock private CacheService cacheService;
    @Mock private EventStore eventStore;
    @Mock private TechnologyRepository technologyRepository;
    @Mock private GithubApiAdapter githubApiAdapter;

    @InjectMocks
    private GetRepositoryAnalysisQueryHandler handler;

    private Principal principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        principal = () -> "tester";
    }

    @Test
    @DisplayName("Debe calcular totalFiles, totalFolders y maxDepth")
    void shouldCalculateProjectStructure() {
        // Mock árbol simple: 2 archivos en carpeta src, 1 archivo en raíz
        List<Map<String, Object>> tree = List.of(
                Map.of("path", "README.md", "type", "blob"),
                Map.of("path", "src/Main.java", "type", "blob"),
                Map.of("path", "src/utils", "type", "tree")
        );
        when(githubApiAdapter.getRepositoryTree(any(), any(), any(), any()))
                .thenReturn(Mono.just(tree));

        // Cache bypass
        when(cacheService.getOrFetch(any(), any()))
                .then(inv -> ((java.util.function.Supplier<Mono<Map<String, Object>>>) inv.getArgument(1)).get());

        GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createCustomAnalysis(
                "owner", "repo", principal, false, false, true, false);

        StepVerifier.create(handler.handle(query))
                .expectNextMatches(result -> {
                    Map<String, Object> structure = (Map<String, Object>) result.get("projectStructure");
                    return structure != null && (int) structure.get("totalFiles") == 2
                            && (int) structure.get("totalFolders") == 1
                            && (int) structure.get("maxDepth") == 2;
                })
                .verifyComplete();
    }
} 