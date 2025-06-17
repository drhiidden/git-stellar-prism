package com.drhdn.ghvis.service;

import com.drhdn.ghvis.entity.RepoCache;
import com.drhdn.ghvis.model.Commit;
import com.drhdn.ghvis.repository.RepoCacheRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Servicio que envuelve GithubService para usar caché en H2 para commits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommitCacheService {

    private final GithubService githubService;
    private final RepoCacheRepository repoCacheRepository;
    private final ObjectMapper objectMapper;

    // TTL de caché
    private static final Duration TTL = Duration.ofHours(1);

    public Flux<Commit> getCommits(String owner, String repo) {
        String repoKey = owner + "/" + repo;

        RepoCache cache = repoCacheRepository.findById(repoKey).orElse(null);
        if (cache != null && cache.getUpdatedAt() != null &&
                cache.getUpdatedAt().isAfter(Instant.now().minus(TTL))) {
            // devolver desde cache
            try {
                List<Commit> commits = objectMapper.readValue(cache.getCommitsJson(), new TypeReference<List<Commit>>() {});
                return Flux.fromIterable(commits);
            } catch (Exception e) {
                log.error("Error al deserializar commits del cache", e);
                // continuar para refrescar
            }
        }

        // Llamar a GitHub y almacenar
        return githubService.getCommits(owner, repo)
                .collectList()
                .doOnNext(list -> {
                    try {
                        String json = objectMapper.writeValueAsString(list);
                        RepoCache newCache = RepoCache.builder()
                                .repo(repoKey)
                                .commitsJson(json)
                                .updatedAt(Instant.now())
                                .build();
                        repoCacheRepository.save(newCache);
                    } catch (Exception ex) {
                        log.error("Error guardando commits en cache", ex);
                    }
                })
                .flatMapMany(Flux::fromIterable);
    }
} 