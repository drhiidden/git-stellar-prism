package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.entity.Language;
import com.drhdn.ghvis.domain.port.LanguageRepository;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Adapter para operaciones de lenguajes usando GitHub API.
 * Implementa el puerto LanguageRepository siguiendo arquitectura hexagonal.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubLanguageRepositoryAdapter implements LanguageRepository {

    private final GithubApiAdapter githubApiAdapter;
    private final RepositoryRepository repositoryRepository;

    @Override
    public Flux<Language> getLanguagesByRepository(String owner, String repo, Principal principal) {
        log.debug("🔍 Obteniendo lenguajes para repositorio: {}/{}", owner, repo);
        
        return githubApiAdapter.getLanguages(owner, repo, principal)
            .flatMapMany(languagesMap -> 
                Flux.fromIterable(languagesMap.entrySet())
                    .map(entry -> mapToLanguage(entry, owner, repo))
            )
            .doOnComplete(() -> log.debug("✅ Lenguajes obtenidos para: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo lenguajes para {}/{}: {}", 
                owner, repo, error.getMessage()));
    }

    @Override
    public Mono<Map<String, Long>> getLanguagesMap(String owner, String repo, Principal principal) {
        log.debug("🔍 Obteniendo mapa de lenguajes para repositorio: {}/{}", owner, repo);
        
        return githubApiAdapter.getLanguages(owner, repo, principal)
            .doOnSuccess(langMap -> log.debug("✅ Mapa de lenguajes obtenido para: {}/{} ({})", 
                owner, repo, langMap.size()))
            .doOnError(error -> log.error("❌ Error obteniendo mapa de lenguajes para {}/{}: {}", 
                owner, repo, error.getMessage()))
            .onErrorReturn(Collections.emptyMap());
    }

    @Override
    public Mono<Language> getPrimaryLanguage(String owner, String repo, Principal principal) {
        log.debug("🔍 Obteniendo lenguaje principal para repositorio: {}/{}", owner, repo);
        
        return getLanguagesMap(owner, repo, principal)
            .map(this::findPrimaryLanguage)
            .map(entry -> mapToLanguage(entry, owner, repo))
            .doOnSuccess(language -> log.debug("✅ Lenguaje principal obtenido para {}/{}: {}", 
                owner, repo, language.getName()))
            .doOnError(error -> log.error("❌ Error obteniendo lenguaje principal para {}/{}: {}", 
                owner, repo, error.getMessage()));
    }

    @Override
    public Flux<LanguageStats> getLanguageStatsByUser(String username, Principal principal) {
        log.debug("🔍 Obteniendo estadísticas de lenguajes para usuario: {}", username);
        
        return repositoryRepository.findByUser(principal)
            .flatMap(repository ->
                getLanguagesMap(repository.getOwner(), repository.getName(), principal)
                    .<LanguageRepository.LanguageStats>map(langMap -> new LanguageStatsImpl(repository, langMap))
                    .onErrorReturn((LanguageRepository.LanguageStats) new LanguageStatsImpl(repository, Collections.emptyMap()))
            )
            .doOnComplete(() -> log.debug("✅ Estadísticas de lenguajes obtenidas para usuario: {}", username))
            .doOnError(error -> log.error("❌ Error obteniendo estadísticas de lenguajes para usuario {}: {}", 
                username, error.getMessage()));
    }

    @Override
    public Flux<Language> getTopLanguagesByUser(String username, Principal principal, int limit) {
        log.debug("🔍 Obteniendo top {} lenguajes para usuario: {}", limit, username);
        
        return getLanguageStatsByUser(username, principal)
            .map(stats -> Language.builder()
                .name(stats.getPrimaryLanguage())
                .bytes(0L) // Se podría calcular sumando todos los repositorios
                .percentage(stats.getPrimaryLanguagePercentage())
                .repositoryOwner(stats.getRepositoryOwner())
                .repositoryName(stats.getRepositoryName())
                .analyzedAt(stats.getAnalyzedAt())
                .build())
            .distinct(Language::getName)
            .take(limit)
            .doOnComplete(() -> log.debug("✅ Top {} lenguajes obtenidos para usuario: {}", limit, username));
    }

    @Override
    public Mono<Boolean> hasLanguages(String owner, String repo, Principal principal) {
        log.debug("🔍 Verificando si repositorio {}/{} tiene lenguajes", owner, repo);
        
        return getLanguagesMap(owner, repo, principal)
            .map(langMap -> !langMap.isEmpty())
            .onErrorReturn(false)
            .doOnNext(hasLangs -> log.debug("✅ Repositorio {}/{} tiene lenguajes: {}", 
                owner, repo, hasLangs));
    }

    /**
     * Convierte una entrada del mapa de lenguajes a un objeto Language.
     * 
     * @param entry Entrada del mapa (nombre -> bytes)
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return Objeto Language
     */
    private Language mapToLanguage(Map.Entry<String, Long> entry, String owner, String repo) {
        return Language.builder()
            .name(entry.getKey())
            .bytes(entry.getValue())
            .percentage(0.0) // Se calcularía con el total
            .repositoryOwner(owner)
            .repositoryName(repo)
            .analyzedAt(Instant.now())
            .build();
    }

    /**
     * Encuentra el lenguaje principal (el que tiene más bytes).
     * 
     * @param languagesMap Mapa de lenguajes
     * @return Entrada del lenguaje principal
     */
    private Map.Entry<String, Long> findPrimaryLanguage(Map<String, Long> languagesMap) {
        return languagesMap.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(Map.entry("Unknown", 0L));
    }

    /**
     * Implementación de LanguageStats para estadísticas de lenguajes.
     */
    private static class LanguageStatsImpl implements LanguageRepository.LanguageStats {
        private final com.drhdn.ghvis.domain.entity.Repository repository;
        private final Map<String, Long> languagesMap;
        private final Map.Entry<String, Long> primaryLanguage;
        private final long totalBytes;

        public LanguageStatsImpl(com.drhdn.ghvis.domain.entity.Repository repository, 
                               Map<String, Long> languagesMap) {
            this.repository = repository;
            this.languagesMap = languagesMap;
            this.primaryLanguage = findPrimaryLanguageEntry(languagesMap);
            this.totalBytes = languagesMap.values().stream().mapToLong(Long::longValue).sum();
        }

        private Map.Entry<String, Long> findPrimaryLanguageEntry(Map<String, Long> langMap) {
            return langMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("Unknown", 0L));
        }

        @Override
        public String getRepositoryName() {
            return repository.getName();
        }

        @Override
        public String getRepositoryOwner() {
            return repository.getOwner();
        }

        @Override
        public String getPrimaryLanguage() {
            return primaryLanguage.getKey();
        }

        @Override
        public Double getPrimaryLanguagePercentage() {
            if (totalBytes == 0) return 0.0;
            return (primaryLanguage.getValue().doubleValue() / totalBytes) * 100.0;
        }

        @Override
        public Integer getTotalLanguages() {
            return languagesMap.size();
        }

        @Override
        public Long getTotalBytes() {
            return totalBytes;
        }

        @Override
        public Instant getAnalyzedAt() {
            return Instant.now();
        }
    }
} 