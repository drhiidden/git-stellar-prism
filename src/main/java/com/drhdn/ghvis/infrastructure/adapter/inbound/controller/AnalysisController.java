package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;


import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para análisis de repositorios y generación de resúmenes.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final GithubApiAdapter githubApiAdapter;

    /**
     * Analiza un repositorio para obtener distribución de lenguajes, tecnologías y resumen técnico.
     * Endpoint consumido por summaryEditor.js
     */
    @GetMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> analyzeRepository(@RequestParam("repo") String repoParam, Principal principal) {
        String[] parts = repoParam.split("/");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Parámetro 'repo' inválido."));
        }
        String owner = parts[0];
        String repo = parts[1];

        // Distribución de lenguajes usando OAuth2 del usuario autenticado
        Mono<Map<String, Long>> languagesMono = githubApiAdapter.getLanguages(owner, repo, principal);

        // Para simplificar, dejamos tecnologías y resumen como listas vacías
        List<Object> technologies = Collections.emptyList();
        Map<String, Object> summary = Map.of(
            "repositoryName", repo,
            "repositoryOwner", owner,
            "projectPurpose", "Propósito no disponible",
            "mainTechnologies", Collections.emptyList(),
            "rolesAndResponsibilities", "",
            "achievements", Collections.emptyList(),
            "codeSnippets", Collections.emptyList(),
            "documentationQuality", ""
        );

        return languagesMono.map(langMap -> Map.of(
                "languageDistribution", langMap,
                "technologies", technologies,
                "projectStructure", Collections.emptyMap(),
                "technicalSummary", summary
        ));
    }
} 