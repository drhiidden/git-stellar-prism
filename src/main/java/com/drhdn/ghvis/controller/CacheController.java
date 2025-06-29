package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.service.CommitCacheService;
import com.drhdn.ghvis.service.UserRepositoryCacheService;
import com.drhdn.ghvis.service.GithubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador para gestión de caché y exportaciones de análisis.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/cache")
public class CacheController {

    private final UserRepositoryCacheService userRepositoryCacheService;
    private final CommitCacheService commitCacheService;
    private final GithubService githubService;

    /**
     * Obtiene estadísticas del caché de repositorios.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Object> getCacheStats(Principal principal) {
        if (principal == null) {
            return Mono.just(Map.of("error", "Usuario no autenticado"));
        }
        
        return userRepositoryCacheService.getCacheStats()
            .map(stats -> (Object) stats)
            .doOnNext(stats -> log.debug("Estadísticas de caché obtenidas"))
            .onErrorReturn(Map.of("error", "Error obteniendo estadísticas de caché"));
    }

    /**
     * Limpia el caché del usuario actual.
     */
    @DeleteMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Object> clearUserCache(Principal principal) {
        if (principal == null) {
            return Mono.just(Map.of("error", "Usuario no autenticado"));
        }
        
        return Mono.fromRunnable(() -> {
            userRepositoryCacheService.clearUserCache(principal.getName());
            log.info("Caché de usuario limpiado: {}", principal.getName());
        })
        .then(Mono.just((Object) Map.of(
            "success", true,
            "message", "Caché del usuario limpiado correctamente"
        )))
        .onErrorReturn(Map.of("error", "Error limpiando caché del usuario"));
    }

    /**
     * Limpia el caché de commits para un repositorio específico.
     */
    @DeleteMapping(value = "/commits/{owner}/{repo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Object> clearCommitCache(@PathVariable String owner, @PathVariable String repo, Principal principal) {
        if (principal == null) {
            return Mono.just(Map.of("error", "Usuario no autenticado"));
        }
        
        return Mono.fromRunnable(() -> {
            commitCacheService.clearCache(owner, repo);
            log.info("Caché de commits limpiado para: {}/{}", owner, repo);
        })
        .then(Mono.just((Object) Map.of(
            "success", true,
            "message", String.format("Caché de commits limpiado para %s/%s", owner, repo)
        )))
        .onErrorReturn(Map.of("error", "Error limpiando caché de commits"));
    }

    /**
     * Exporta análisis completo de un repositorio.
     * Incluye: commits, issues, pull requests, análisis técnico, timeline
     */
    @GetMapping(value = "/export/{owner}/{repo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Object> exportRepositoryAnalysis(@PathVariable String owner, @PathVariable String repo, 
                                                  @RequestParam(defaultValue = "all") String type,
                                                  Principal principal) {
        if (principal == null) {
            return Mono.just(Map.of("error", "Usuario no autenticado"));
        }
        
        return generateExportData(owner, repo, type, principal)
            .doOnNext(data -> log.info("Análisis exportado para {}/{} (tipo: {})", owner, repo, type))
            .onErrorReturn(Map.of("error", "Error generando exportación"));
    }

    /**
     * Genera los datos de exportación según el tipo solicitado.
     */
    private Mono<Object> generateExportData(String owner, String repo, String type, Principal principal) {
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("repository", owner + "/" + repo);
        exportData.put("exportType", type);
        exportData.put("timestamp", java.time.Instant.now().toString());
        exportData.put("user", principal.getName());

        switch (type.toLowerCase()) {
            case "commits":
                return exportCommitsOnly(owner, repo, principal, exportData);
            case "timeline":
                return exportTimelineOnly(owner, repo, principal, exportData);
            case "technical":
                return exportTechnicalOnly(owner, repo, principal, exportData);
            case "all":
            default:
                return exportCompleteAnalysis(owner, repo, principal, exportData);
        }
    }

    /**
     * Exporta solo commits.
     */
    private Mono<Object> exportCommitsOnly(String owner, String repo, Principal principal, Map<String, Object> exportData) {
        return commitCacheService.getCommits(owner, repo, principal)
            .collectList()
            .map(commits -> {
                exportData.put("commits", commits);
                exportData.put("totalCommits", commits.size());
                return (Object) exportData;
            });
    }

    /**
     * Exporta datos para timeline.
     */
    private Mono<Object> exportTimelineOnly(String owner, String repo, Principal principal, Map<String, Object> exportData) {
        return Mono.zip(
            commitCacheService.getCommits(owner, repo, principal).collectList(),
            githubService.getPullRequests(owner, repo, principal).collectList(),
            githubService.getIssues(owner, repo, principal).collectList()
        ).map(tuple -> {
            exportData.put("timelineData", Map.of(
                "commits", tuple.getT1(),
                "pullRequests", tuple.getT2(),
                "issues", tuple.getT3()
            ));
            return (Object) exportData;
        });
    }

    /**
     * Exporta solo análisis técnico.
     */
    private Mono<Object> exportTechnicalOnly(String owner, String repo, Principal principal, Map<String, Object> exportData) {
        return Mono.zip(
            githubService.getRepository(owner, repo, principal),
            githubService.getLanguages(owner, repo, principal)
        ).map(tuple -> {
            exportData.put("technicalAnalysis", Map.of(
                "repository", tuple.getT1(),
                "languages", tuple.getT2()
            ));
            return (Object) exportData;
        });
    }

    /**
     * Exporta análisis completo.
     */
    private Mono<Object> exportCompleteAnalysis(String owner, String repo, Principal principal, Map<String, Object> exportData) {
        return Mono.zip(
            githubService.getRepository(owner, repo, principal),
            commitCacheService.getCommits(owner, repo, principal).collectList(),
            githubService.getPullRequests(owner, repo, principal).collectList(),
            githubService.getIssues(owner, repo, principal).collectList(),
            githubService.getLanguages(owner, repo, principal)
        ).map(tuple -> {
            exportData.put("repository", tuple.getT1());
            exportData.put("commits", tuple.getT2());
            exportData.put("pullRequests", tuple.getT3());
            exportData.put("issues", tuple.getT4());
            exportData.put("languages", tuple.getT5());
            
            // Estadísticas generales
            exportData.put("statistics", Map.of(
                "totalCommits", tuple.getT2().size(),
                "totalPullRequests", tuple.getT3().size(),
                "totalIssues", tuple.getT4().size(),
                "totalLanguages", tuple.getT5().size()
            ));
            
            return (Object) exportData;
        });
    }
} 