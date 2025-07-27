package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetRepositoryCommitsQuery;
import com.drhdn.ghvis.application.usecase.GetRepositoryCommitsUseCase;
import com.drhdn.ghvis.domain.entity.Commit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Handler para procesar {@link GetRepositoryCommitsQuery} siguiendo el patrón CQRS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryCommitsQueryHandler {

    private final GetRepositoryCommitsUseCase commitsUseCase;

    /**
     * Maneja la query devolviendo la lista de commits.
     *
     * @param query Query con los datos necesarios
     * @return Mono con lista de commits
     */
    public Mono<List<Commit>> handle(GetRepositoryCommitsQuery query) {
        log.info("🔍 Ejecutando query de commits para repositorio: {} (QueryId: {})",
                query.getRepositoryFullName(), query.getQueryId());

        return commitsUseCase.execute(query.getOwner(), query.getRepo(), query.getPrincipal())
                .collectList()
                .doOnSuccess(commits -> log.info("✅ Query de commits completada: {} commits para {} (QueryId: {})",
                        commits.size(), query.getRepositoryFullName(), query.getQueryId()))
                .doOnError(error -> log.error("❌ Error en query de commits para {} (QueryId: {}): {}",
                        query.getRepositoryFullName(), query.getQueryId(), error.getMessage()));
    }
} 