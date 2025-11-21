package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.service.CVService;
import com.drhdn.ghvis.application.service.CVExportService;
import com.drhdn.ghvis.application.service.RepositoryAnalyzer;
import com.drhdn.ghvis.domain.entity.TechnicalCV;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Map;

/**
 * Controlador para exportación de CV y resumen técnico consolidado.
 * Delega la generación de formatos al CVExportService.
 * 
 * @author GitStellarPrism Team
 * @version 2.2.0
 */
@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
@Slf4j
public class CVExportController {

    private final CVService cvService;
    private final CVExportService cvExportService;
    private final RepositoryAnalyzer repositoryAnalyzer;
    private final com.drhdn.ghvis.domain.port.RepositoryRepository repositoryRepository;

    @GetMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<TechnicalCV>> generateCV(Principal principal) {
        if (principal == null) return Mono.just(ResponseEntity.status(401).build());

        String username = principal.getName();
        log.info("📄 Solicitud de generación de CV para: {}", username);

        return cvService.generateCV(username, principal)
            .map(cv -> ResponseEntity.ok()
                .header("X-Generated-At", cv.getMetadata().getGeneratedAt().toString())
                .body(cv))
            .onErrorResume(error -> {
                log.error("❌ Error generando CV: {}", error.getMessage());
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }
    
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getTechnicalSummary(Principal principal) {
        if (principal == null) return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Usuario no autenticado")));

        return cvService.generateCV(principal.getName(), principal)
            .map(cv -> {
                Map<String, Object> summary = Map.of(
                    "metadata", cv.getMetadata(),
                    "header", cv.getHeader(),
                    "summary", cv.getSummary()
                );
                return ResponseEntity.ok(summary);
            });
    }

    @GetMapping(value = "/export/markdown", produces = "text/markdown")
    public Mono<ResponseEntity<String>> exportToMarkdown(Principal principal) {
        if (principal == null) return Mono.just(ResponseEntity.status(401).body("# Error\nUsuario no autenticado"));

        String username = principal.getName();
        log.info("📄 Exportando CV a Markdown para usuario: {}", username);

        return Mono.zip(
            cvService.generateCV(username, principal),
            repositoryRepository.findByUser(principal).collectList()
        )
        .map(tuple -> {
            TechnicalCV cv = tuple.getT1();
            var repos = tuple.getT2();
            
            // Generamos metadata para el formateador
            var techMetadata = repositoryAnalyzer.generateTechnicalMetadata(repos);
            String markdown = cvExportService.generateMarkdown(cv, techMetadata);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"cv-" + cv.getHeader().getUsername() + ".md\"")
                .body(markdown);
        });
    }
    
    @GetMapping(value = "/export/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<TechnicalCV>> exportToJson(Principal principal) {
        if (principal == null) return Mono.just(ResponseEntity.status(401).build());
        return cvService.generateCV(principal.getName(), principal)
            .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<String>> previewCV(Principal principal) {
        if (principal == null) return Mono.just(ResponseEntity.status(401).body("<h1>Login Required</h1>"));
        
        return cvService.generateCV(principal.getName(), principal)
            .map(cv -> ResponseEntity.ok(cvExportService.generateHtmlPreview(cv)));
    }
}
