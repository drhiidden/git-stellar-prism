package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryTimelineQuery;
import com.drhdn.ghvis.domain.entity.TimelineEvent;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.CommitRepository;
import com.drhdn.ghvis.domain.port.IssueRepository;
import com.drhdn.ghvis.domain.port.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

/**
 * Handler para procesar queries de timeline de repositorio.
 * Consolida eventos de commits, PRs e issues en timeline unificado.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryTimelineQueryHandler {

    private final CommitRepository commitRepository;
    private final PullRequestRepository pullRequestRepository;
    private final IssueRepository issueRepository;
    private final CacheService cacheService;

    /**
     * Maneja la query para obtener timeline del repositorio.
     * Principio SRP: responsabilidad única de consolidar timeline.
     */
    public Mono<List<TimelineEvent>> handle(GetRepositoryTimelineQuery query) {
        log.info("🔍 Ejecutando query de timeline para repositorio: {} (QueryId: {})",
                query.getRepositoryFullName(), query.getQueryId());

        String cacheKey = query.getCacheKey();

        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando timeline fresco para: {}/{}", 
                query.getOwner(), query.getRepo());

            return buildTimelineFlux(query)
                .sort(Comparator.comparing(TimelineEvent::getDate).reversed())
                .take(query.getLimit())
                .collectList();
        })
        .doOnSuccess(events -> log.info("✅ Timeline completado para: {}/{} - {} eventos (QueryId: {})",
                query.getOwner(), query.getRepo(), events.size(), query.getQueryId()))
        .doOnError(error -> log.error("❌ Error en timeline para: {}/{} (QueryId: {}): {}",
                query.getOwner(), query.getRepo(), query.getQueryId(), error.getMessage()));
    }

    /**
     * Construye el flux unificado de eventos según tipos solicitados.
     * Principio OCP: extensible para nuevos tipos de eventos.
     */
    private Flux<TimelineEvent> buildTimelineFlux(GetRepositoryTimelineQuery query) {
        Flux<TimelineEvent> eventsFlux = Flux.empty();

        // Principio DRY: evitamos duplicar lógica de filtrado
        if (query.getEventTypes().contains(GetRepositoryTimelineQuery.EventType.COMMITS)) {
            eventsFlux = eventsFlux.mergeWith(getCommitEvents(query));
        }

        if (query.getEventTypes().contains(GetRepositoryTimelineQuery.EventType.PULL_REQUESTS)) {
            eventsFlux = eventsFlux.mergeWith(getPullRequestEvents(query));
        }

        if (query.getEventTypes().contains(GetRepositoryTimelineQuery.EventType.ISSUES)) {
            eventsFlux = eventsFlux.mergeWith(getIssueEvents(query));
        }

        return eventsFlux
            .filter(event -> isWithinDateRange(event, query));
    }

    /**
     * Obtiene eventos de commits transformados a TimelineEvent.
     * Principio ISP: interface segregation, cada repository maneja su dominio.
     */
    private Flux<TimelineEvent> getCommitEvents(GetRepositoryTimelineQuery query) {
        return commitRepository.findByRepository(query.getOwner(), query.getRepo(), query.getPrincipal())
            .map(commit -> TimelineEvent.builder()
                .id(commit.getHash())
                .type(TimelineEvent.EventType.COMMIT)
                .title(commit.getMessage())
                .author(commit.getAuthor())
                .authorEmail(commit.getAuthorEmail())
                .date(commit.getTimestamp())
                .url(String.format("https://github.com/%s/%s/commit/%s", 
                    query.getOwner(), query.getRepo(), commit.getHash()))
                .additions(commit.getStats() != null ? commit.getStats().getAdditions() : null)
                .deletions(commit.getStats() != null ? commit.getStats().getDeletions() : null)
                .status("committed")
                .build())
            .doOnComplete(() -> log.debug("✅ Eventos de commits obtenidos para: {}/{}", 
                query.getOwner(), query.getRepo()));
    }

    /**
     * Obtiene eventos de Pull Requests transformados a TimelineEvent.
     */
    private Flux<TimelineEvent> getPullRequestEvents(GetRepositoryTimelineQuery query) {
        return pullRequestRepository.findByRepository(query.getOwner(), query.getRepo(), query.getPrincipal())
            .map(pr -> TimelineEvent.builder()
                .id(String.valueOf(pr.getNumber()))
                .type(TimelineEvent.EventType.PULL_REQUEST)
                .title(pr.getTitle())
                .description(pr.getDescription())
                .author(pr.getAuthor())
                .date(pr.getTimestamp())
                .url(String.format("https://github.com/%s/%s/pull/%d", query.getOwner(), query.getRepo(), pr.getNumber()))
                .additions(pr.getStats() != null ? pr.getStats().getAdditions() : null)
                .deletions(pr.getStats() != null ? pr.getStats().getDeletions() : null)
                .status(pr.getState())
                .build())
            .doOnComplete(() -> log.debug("✅ Eventos de PRs obtenidos para: {}/{}", 
                query.getOwner(), query.getRepo()));
    }

    /**
     * Obtiene eventos de Issues transformados a TimelineEvent.
     */
    private Flux<TimelineEvent> getIssueEvents(GetRepositoryTimelineQuery query) {
        return issueRepository.findByRepository(query.getOwner(), query.getRepo(), query.getPrincipal())
            .map(issue -> TimelineEvent.builder()
                .id(String.valueOf(issue.getNumber()))
                .type(TimelineEvent.EventType.ISSUE)
                .title(issue.getTitle())
                .description(issue.getDescription())
                .author(issue.getAuthor())
                .date(issue.getTimestamp())
                .url(String.format("https://github.com/%s/%s/issues/%d", query.getOwner(), query.getRepo(), issue.getNumber()))
                .status(issue.getState())
                .labels(issue.getLabels() != null ? issue.getLabels().stream().map(l -> l.getName()).toArray(String[]::new) : new String[0])
                .build())
            .doOnComplete(() -> log.debug("✅ Eventos de issues obtenidos para: {}/{}", 
                query.getOwner(), query.getRepo()));
    }

    /**
     * Filtra eventos por rango de fechas si está especificado.
     * Principio SRP: responsabilidad única de filtrado temporal.
     */
    private boolean isWithinDateRange(TimelineEvent event, GetRepositoryTimelineQuery query) {
        if (query.getSince() != null && event.getDate().isBefore(query.getSince())) {
            return false;
        }
        if (query.getUntil() != null && event.getDate().isAfter(query.getUntil())) {
            return false;
        }
        return true;
    }

    /**
     * Limpia cache de timeline para un repositorio.
     */
    public void clearCache(String owner, String repo) {
        String cachePattern = String.format("repo:%s:%s:timeline:*", owner, repo);
        cacheService.clear(cachePattern).subscribe();
        log.info("🧹 Cache de timeline limpiado para: {}/{}", owner, repo);
    }
} 