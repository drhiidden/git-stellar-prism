package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.entity.Language;
import com.drhdn.ghvis.domain.port.LanguageRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

/**
 * Adapter de infraestructura para operaciones de lenguajes usando GitHub API.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubLanguageRepositoryAdapter implements LanguageRepository {
    
    private final GithubApiAdapter githubApiAdapter;
    
    @Override
    public Flux<Language> getLanguagesByRepository(String owner, String repo, Principal principal) {
        log.info("🔍 Obteniendo lenguajes para repositorio: {}/{}", owner, repo);
        
        return githubApiAdapter.getLanguages(owner, repo, principal)
            .flatMapMany(languagesMap -> Flux.fromIterable(languagesMap.entrySet()))
            .map(entry -> buildLanguage(owner, repo, entry.getKey(), entry.getValue()))
            .doOnComplete(() -> log.info("✅ Lenguajes obtenidos para repositorio: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo lenguajes para {}/{}: {}", owner, repo, error.getMessage()));
    }
    
    @Override
    public Mono<Map<String, Long>> getLanguagesMap(String owner, String repo, Principal principal) {
        log.info("🔍 Obteniendo mapa de lenguajes para repositorio: {}/{}", owner, repo);
        
        return githubApiAdapter.getLanguages(owner, repo, principal)
            .doOnSuccess(languages -> log.info("✅ Mapa de lenguajes obtenido para repositorio: {}/{}", owner, repo))
            .doOnError(error -> log.error("❌ Error obteniendo mapa de lenguajes para {}/{}: {}", owner, repo, error.getMessage()));
    }
    
    @Override
    public Mono<Language> getPrimaryLanguage(String owner, String repo, Principal principal) {
        log.info("🔍 Obteniendo lenguaje principal para repositorio: {}/{}", owner, repo);
        
        return getLanguagesMap(owner, repo, principal)
            .flatMap(languagesMap -> {
                if (languagesMap.isEmpty()) {
                    return Mono.empty();
                }
                
                // Encontrar el lenguaje con más bytes
                Map.Entry<String, Long> primaryEntry = languagesMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
                
                if (primaryEntry != null) {
                    Language primaryLanguage = buildLanguage(owner, repo, primaryEntry.getKey(), primaryEntry.getValue());
                    return Mono.just(primaryLanguage);
                }
                
                return Mono.empty();
            })
            .doOnSuccess(language -> {
                if (language != null) {
                    log.info("✅ Lenguaje principal obtenido: {} para {}/{}", language.getName(), owner, repo);
                } else {
                    log.info("ℹ️ No se encontró lenguaje principal para {}/{}", owner, repo);
                }
            })
            .doOnError(error -> log.error("❌ Error obteniendo lenguaje principal para {}/{}: {}", owner, repo, error.getMessage()));
    }
    
    @Override
    public Flux<LanguageStats> getLanguageStatsByUser(String username, Principal principal) {
        log.info("🔍 Obteniendo estadísticas de lenguajes para usuario: {}", username);
        
        // TODO: Implementar obtención de estadísticas de lenguajes por usuario
        // Esto requeriría obtener todos los repositorios del usuario y luego sus lenguajes
        return Flux.empty()
            .doOnComplete(() -> log.info("ℹ️ Estadísticas de lenguajes por usuario no implementadas aún para: {}", username));
    }
    
    @Override
    public Flux<Language> getTopLanguagesByUser(String username, Principal principal, int limit) {
        log.info("🔍 Obteniendo top {} lenguajes para usuario: {}", limit, username);
        
        // TODO: Implementar obtención de top lenguajes por usuario
        // Esto requeriría agregar todos los lenguajes de todos los repositorios del usuario
        return Flux.empty()
            .doOnComplete(() -> log.info("ℹ️ Top lenguajes por usuario no implementados aún para: {}", username));
    }
    
    @Override
    public Mono<Boolean> hasLanguages(String owner, String repo, Principal principal) {
        log.info("🔍 Verificando si el repositorio {}/{} tiene lenguajes", owner, repo);
        
        return getLanguagesMap(owner, repo, principal)
            .map(languagesMap -> !languagesMap.isEmpty())
            .onErrorReturn(false)
            .doOnSuccess(hasLanguages -> log.info("✅ Repositorio {}/{} tiene lenguajes: {}", owner, repo, hasLanguages))
            .doOnError(error -> log.error("❌ Error verificando lenguajes para {}/{}: {}", owner, repo, error.getMessage()));
    }
    
    /**
     * Construye una entidad Language basada en los datos de GitHub API.
     */
    private Language buildLanguage(String owner, String repo, String languageName, Long bytes) {
        return Language.builder()
            .name(languageName)
            .repositoryName(repo)
            .repositoryOwner(owner)
            .bytes(bytes)
            .percentage(calculatePercentage(bytes)) // TODO: Calcular porcentaje real
            .estimatedLines(estimateLines(bytes))
            .color(getLanguageColor(languageName))
            .type(getLanguageType(languageName))
            .isProgrammingLanguage(isProgrammingLanguage(languageName))
            .analyzedAt(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
    
    /**
     * Calcula el porcentaje del lenguaje (placeholder).
     */
    private Double calculatePercentage(Long bytes) {
        // TODO: Implementar cálculo real basado en total de bytes del repositorio
        return bytes != null ? 100.0 : 0.0;
    }
    
    /**
     * Estima el número de líneas basado en bytes.
     */
    private Long estimateLines(Long bytes) {
        if (bytes == null) return 0L;
        // Estimación aproximada: 1 línea = 50 bytes en promedio
        return bytes / 50;
    }
    
    /**
     * Obtiene el color del lenguaje (placeholder).
     */
    private String getLanguageColor(String languageName) {
        // TODO: Implementar mapeo real de colores de lenguajes
        return switch (languageName.toLowerCase()) {
            case "java" -> "#b07219";
            case "javascript" -> "#f1e05a";
            case "python" -> "#3572A5";
            case "typescript" -> "#2b7489";
            case "go" -> "#00ADD8";
            case "rust" -> "#dea584";
            case "c++" -> "#f34b7d";
            case "c#" -> "#178600";
            default -> "#000000";
        };
    }
    
    /**
     * Determina el tipo de lenguaje.
     */
    private String getLanguageType(String languageName) {
        return switch (languageName.toLowerCase()) {
            case "java", "javascript", "python", "typescript", "go", "rust", "c++", "c#" -> "programming";
            case "html", "css", "xml", "yaml", "json" -> "markup";
            case "sql", "csv" -> "data";
            default -> "other";
        };
    }
    
    /**
     * Determina si es un lenguaje de programación.
     */
    private Boolean isProgrammingLanguage(String languageName) {
        return "programming".equals(getLanguageType(languageName));
    }
} 