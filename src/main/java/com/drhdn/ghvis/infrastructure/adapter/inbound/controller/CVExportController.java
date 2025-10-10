package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.service.CVService;
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
 * Genera formatos útiles para portfolio profesional basado en análisis de GitHub.
 * 
 * ESTRATEGIA EFICIENTE:
 * - 1 request a GitHub para obtener repos (ya cacheado)
 * - 0 requests adicionales para generar CV
 * - Procesamiento en frontend con CVGenerator.js
 * 
 * @author GitStellarPrism Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
@Slf4j
public class CVExportController {

    private final CVService cvService;
    private final RepositoryAnalyzer repositoryAnalyzer;
    private final com.drhdn.ghvis.domain.port.RepositoryRepository repositoryRepository;

    /**
     * Genera CV técnico completo del usuario autenticado.
     * 
     * EFICIENCIA:
     * - Usa datos ya cacheados (0 requests a GitHub)
     * - Procesamiento local
     * - Resultado cacheado por 24h
     * 
     * @param principal Usuario autenticado
     * @return CV técnico en formato JSON
     */
    @GetMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<TechnicalCV>> generateCV(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401).build());
        }

        String username = principal.getName();
        log.info("📄 Solicitud de generación de CV para: {}", username);

        return cvService.generateCV(username, principal)
            .map(cv -> {
                log.info("✅ CV generado exitosamente para: {}", username);
                return ResponseEntity.ok()
                    .header("X-Generated-At", cv.getMetadata().getGeneratedAt().toString())
                    .body(cv);
            })
            .doOnError(error -> log.error("❌ Error generando CV para {}: {}", username, error.getMessage()))
            .onErrorResume(error -> Mono.just(
                ResponseEntity.status(500).build()
            ));
    }
    
    /**
     * Obtiene resumen técnico consolidado del usuario autenticado.
     * Analiza TODOS los repositorios (públicos + privados) y genera estadísticas.
     * 
     * @param principal Usuario autenticado
     * @return Resumen técnico consolidado
     */
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getTechnicalSummary(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401)
                .body(Map.of("error", "Usuario no autenticado")));
        }

        String username = principal.getName();
        log.info("📊 Generando resumen técnico consolidado para usuario: {}", username);

        // Usar CV Service para generar resumen
        return cvService.generateCV(username, principal)
            .map(cv -> {
                Map<String, Object> summary = Map.of(
                    "metadata", cv.getMetadata(),
                    "header", cv.getHeader(),
                    "summary", cv.getSummary()
                );
                return ResponseEntity.ok(summary);
            })
            .doOnSuccess(response -> log.info("✅ Resumen técnico generado para: {}", username))
            .onErrorResume(error -> {
                log.error("❌ Error generando resumen técnico: {}", error.getMessage());
                return Mono.just(ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error generando resumen", "message", error.getMessage())));
            });
    }

    /**
     * Exporta resumen técnico en formato Markdown para CV.
     * 
     * @param principal Usuario autenticado
     * @return Archivo Markdown con resumen técnico
     */
    @GetMapping(value = "/export/markdown", produces = "text/markdown")
    public Mono<ResponseEntity<String>> exportToMarkdown(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401)
                .body("# Error\nUsuario no autenticado"));
        }

        String username = principal.getName();
        log.info("📄 Exportando CV a Markdown para usuario: {}", username);

        return Mono.zip(
            cvService.generateCV(username, principal),
            repositoryRepository.findByUser(principal).collectList()
        )
        .map(tuple -> {
            TechnicalCV cv = tuple.getT1();
            var repos = tuple.getT2();
            
            // Generar metadata técnica
            var techMetadata = repositoryAnalyzer.generateTechnicalMetadata(repos);
            
            String markdown = buildMarkdownCVWithMetadata(cv, techMetadata);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"cv-" + cv.getHeader().getUsername() + ".md\"")
                .body(markdown);
        })
        .doOnSuccess(response -> log.info("✅ CV exportado a Markdown para: {}", username))
        .onErrorResume(error -> {
            log.error("❌ Error exportando CV a Markdown: {}", error.getMessage());
            return Mono.just(ResponseEntity.internalServerError()
                .body("# Error\n" + error.getMessage()));
        });
    }
    
    /**
     * Construye el CV en formato Markdown con metadata técnica
     */
    private String buildMarkdownCVWithMetadata(TechnicalCV cv, RepositoryAnalyzer.TechnicalMetadata techMetadata) {
        StringBuilder md = new StringBuilder();
        
        // Header
        md.append("# ").append(cv.getHeader().getName() != null ? cv.getHeader().getName() : cv.getHeader().getUsername())
          .append("\n\n");
        
        if (cv.getHeader().getBio() != null && !cv.getHeader().getBio().isEmpty()) {
            md.append(cv.getHeader().getBio()).append("\n\n");
        }
        
        // Contact info
        md.append("## 📧 Contacto\n\n");
        if (cv.getHeader().getLocation() != null) {
            md.append("- 📍 **Ubicación**: ").append(cv.getHeader().getLocation()).append("\n");
        }
        if (cv.getHeader().getEmail() != null) {
            md.append("- 📧 **Email**: ").append(cv.getHeader().getEmail()).append("\n");
        }
        if (cv.getHeader().getGithub() != null) {
            md.append("- 💻 **GitHub**: [").append(cv.getHeader().getUsername()).append("](")
              .append(cv.getHeader().getGithub()).append(")\n");
        }
        if (cv.getHeader().getFollowers() != null) {
            md.append("- 👥 **Seguidores**: ").append(cv.getHeader().getFollowers()).append("\n");
        }
        md.append("\n");
        
        // Summary
        if (cv.getSummary() != null) {
            md.append("## 💼 Resumen Profesional\n\n");
            md.append("- **Proyectos totales**: ").append(cv.getSummary().getTotalProjects()).append("\n");
            md.append("- **Proyectos públicos**: ").append(cv.getSummary().getPublicProjects()).append("\n");
            
            if (cv.getSummary().getPrimaryTechnologies() != null && !cv.getSummary().getPrimaryTechnologies().isEmpty()) {
                md.append("- **Tecnologías principales**: ").append(String.join(", ", cv.getSummary().getPrimaryTechnologies())).append("\n");
            }
            md.append("\n");
        }
        
        // Lenguajes de Programación
        if (!techMetadata.languages().isEmpty()) {
            md.append("## 💻 Lenguajes de Programación\n\n");
            techMetadata.languages().entrySet().stream()
                .sorted(Map.Entry.<String, RepositoryAnalyzer.LanguageStats>comparingByValue(
                    java.util.Comparator.comparing(RepositoryAnalyzer.LanguageStats::getProjectCount).reversed()
                ))
                .forEach(entry -> {
                    md.append("- **").append(entry.getKey()).append("** - ")
                      .append(entry.getValue().getProjectCount()).append(" proyecto(s)\n");
                });
            md.append("\n");
        }
        
        // Frameworks y Librerías
        if (!techMetadata.frameworks().isEmpty()) {
            md.append("## 🚀 Frameworks & Librerías\n\n");
            techMetadata.frameworks().entrySet().stream()
                .sorted(Map.Entry.<String, RepositoryAnalyzer.FrameworkStats>comparingByValue(
                    java.util.Comparator.comparing(RepositoryAnalyzer.FrameworkStats::getProjectCount).reversed()
                ))
                .forEach(entry -> {
                    md.append("- **").append(entry.getKey()).append("** - ")
                      .append(entry.getValue().getProjectCount()).append(" proyecto(s)\n");
                });
            md.append("\n");
        }
        
        // CI/CD Tools
        if (!techMetadata.cicdTools().isEmpty()) {
            md.append("## ⚙️ CI/CD & DevOps\n\n");
            techMetadata.cicdTools().stream()
                .sorted()
                .forEach(tool -> md.append("- ").append(tool).append("\n"));
            md.append("\n");
        }
        
        // Proyectos Open Source
        if (!techMetadata.openSourceProjects().isEmpty()) {
            md.append("## 🌟 Proyectos Open Source\n\n");
            techMetadata.openSourceProjects().stream()
                .limit(10)
                .forEach(project -> {
                    md.append("### ").append(project.name()).append("\n\n");
                    if (project.description() != null && !project.description().isBlank()) {
                        md.append(project.description()).append("\n\n");
                    }
                    md.append("- ⭐ **").append(project.stars()).append(" stars**");
                    if (project.forks() > 0) {
                        md.append(" | 🔱 **").append(project.forks()).append(" forks**");
                    }
                    md.append("\n");
                    md.append("- 🔗 [Ver proyecto](").append(project.url()).append(")\n");
                    if (project.topics() != null && !project.topics().isEmpty()) {
                        md.append("- 🏷️ Tags: ").append(String.join(", ", project.topics())).append("\n");
                    }
                    md.append("\n");
                });
        }
        
        // Metadata
        if (cv.getMetadata() != null) {
            md.append("## 📊 Estadísticas GitHub\n\n");
            md.append("- **Total de repositorios**: ").append(cv.getMetadata().getTotalRepositories()).append("\n");
            md.append("- **Perfil**: [").append(cv.getMetadata().getGithubProfile()).append("](")
              .append(cv.getMetadata().getGithubProfile()).append(")\n");
            md.append("\n");
        }
        
        // Footer
        md.append("---\n\n");
        md.append("*CV generado por [GitStellarPrism](").append(cv.getMetadata().getSource()).append(") ");
        md.append("el ").append(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(cv.getMetadata().getGeneratedAt())).append("*\n");
        
        return md.toString();
    }

    /**
     * Exporta resumen técnico en formato JSON para portfolio web.
     * 
     * @param principal Usuario autenticado
     * @return JSON con datos estructurados para portfolio
     */
    @GetMapping(value = "/export/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> exportToJson(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401)
                .body(Map.of("error", "Usuario no autenticado")));
        }

        String username = principal.getName();
        log.info("📦 Exportando CV a JSON para usuario: {}", username);

        // TODO: Implementar lógica real
        Map<String, Object> mockJson = Map.of(
            "username", username,
            "status", "MOCK_DATA",
            "message", "Implementación pendiente",
            "structure", Map.of(
                "profile", "Información del usuario",
                "languages", "Lenguajes y porcentajes",
                "technologies", "Stack tecnológico completo",
                "topRepositories", "Proyectos destacados",
                "activityTimeline", "Timeline de commits",
                "statistics", "Métricas consolidadas"
            )
        );

        return Mono.just(ResponseEntity.ok(mockJson));
    }

    /**
     * Vista previa del CV en HTML renderizado.
     * 
     * @param principal Usuario autenticado
     * @return HTML con preview del CV
     */
    @GetMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<String>> previewCV(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401)
                .body("<h1>Error: Usuario no autenticado</h1>"));
        }

        String username = principal.getName();
        log.info("👁️ Generando preview de CV para usuario: {}", username);

        // TODO: Implementar lógica real con Thymeleaf template
        String mockHtml = String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s - CV Técnico</title>
                <style>
                    body { font-family: Arial, sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
                    h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }
                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .section { margin: 30px 0; }
                    .tech-badge { display: inline-block; background: #e3f2fd; padding: 5px 10px; 
                                 margin: 5px; border-radius: 3px; font-size: 14px; }
                </style>
            </head>
            <body>
                <h1>%s - Perfil Técnico</h1>
                
                <div class="warning">
                    <strong>⚠️ MOCK DATA - Implementación pendiente</strong>
                    <p>Este preview será generado automáticamente desde tus repositorios de GitHub.</p>
                </div>
                
                <div class="section">
                    <h2>🎯 Stack Tecnológico Principal</h2>
                    <div class="tech-badge">Java</div>
                    <div class="tech-badge">Spring Boot</div>
                    <div class="tech-badge">React</div>
                    <div class="tech-badge">[Detectados automáticamente]</div>
                </div>
                
                <div class="section">
                    <h2>🚀 Proyectos Destacados</h2>
                    <p>Se listarán tus repositorios más relevantes con:</p>
                    <ul>
                        <li>Tecnologías utilizadas</li>
                        <li>Número de commits</li>
                        <li>Descripción generada automáticamente</li>
                    </ul>
                </div>
                
                <div class="section">
                    <h2>📊 Estadísticas</h2>
                    <ul>
                        <li><strong>Total de repositorios:</strong> [Por calcular]</li>
                        <li><strong>Total de commits:</strong> [Por calcular]</li>
                        <li><strong>Años de experiencia:</strong> [Por calcular]</li>
                    </ul>
                </div>
                
                <div class="section">
                    <h2>🔧 Próximos pasos</h2>
                    <ol>
                        <li>Implementar análisis consolidado de repositorios</li>
                        <li>Generar estadísticas reales</li>
                        <li>Crear template HTML profesional</li>
                        <li>Añadir visualizaciones y gráficos</li>
                    </ol>
                </div>
            </body>
            </html>
            """, username, username);

        return Mono.just(ResponseEntity.ok(mockHtml));
    }
}

