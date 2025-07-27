package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryAnalysisQuery;
import com.drhdn.ghvis.domain.entity.Technology;
import com.drhdn.ghvis.domain.event.EventStore;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.LanguageRepository;
import com.drhdn.ghvis.domain.port.TechnologyRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GetRepositoryAnalysisQueryHandlerSummaryTest {

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
    @DisplayName("Debe generar resumen técnico con lenguaje principal y tecnologías")
    void shouldGenerateTechnicalSummary() {
        // Mock languages map
        Map<String, Long> langMap = Map.of(
                "Java", 4000L,
                "JavaScript", 1000L
        );
        when(languageRepository.getLanguagesMap(any(), any(), any()))
                .thenReturn(Mono.just(langMap));

        // Mock technologies
        Technology spring = Technology.builder()
                .name("Spring Boot")
                .language("Java")
                .confidence(0.9)
                .repositoryOwner("owner")
                .repositoryName("repo")
                .detectedAt(Instant.now())
                .category("Framework")
                .build();

        when(technologyRepository.detectTechnologies(any(), any(), any()))
                .thenReturn(Flux.just(spring));

        // Cache bypass
        when(cacheService.getOrFetch(any(), any()))
                .then(inv -> ((java.util.function.Supplier<Mono<Map<String, Object>>>) inv.getArgument(1)).get());

        GetRepositoryAnalysisQuery query = GetRepositoryAnalysisQuery.createCustomAnalysis(
                "owner", "repo", principal,
                false, false, false, true);

        StepVerifier.create(handler.handle(query))
                .expectNextMatches(result -> {
                    Map<String, Object> summary = (Map<String, Object>) result.get("technicalSummary");
                    return summary != null && "Java".equals(summary.get("mainLanguage"))
                            && ((int) summary.get("languageCount")) == 2
                            && ((List<?>) summary.get("mainTechnologies")).size() == 1;
                })
                .verifyComplete();
    }
} 