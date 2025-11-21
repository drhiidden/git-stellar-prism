package com.drhdn.ghvis.application.service;

import com.drhdn.ghvis.domain.entity.TechnicalCV;
import com.drhdn.ghvis.application.service.RepositoryAnalyzer.TechnicalMetadata;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Servicio encargado de formatear y exportar el CV técnico a diferentes representaciones.
 * Separa la lógica de presentación (Markdown, HTML) del controlador.
 */
@Service
public class CVExportService {

    public String generateMarkdown(TechnicalCV cv, TechnicalMetadata techMetadata) {
        StringBuilder md = new StringBuilder();
        
        appendHeader(md, cv);
        appendContactInfo(md, cv);
        appendArchitectures(md, techMetadata);
        appendLanguages(md, techMetadata);
        appendFrameworks(md, techMetadata);
        appendCICD(md, techMetadata);
        appendOpenSource(md, techMetadata);
        
        return md.toString();
    }

    private void appendHeader(StringBuilder md, TechnicalCV cv) {
        md.append("# ").append(cv.getHeader().getName() != null ? cv.getHeader().getName() : cv.getHeader().getUsername()).append("\n");
        if (cv.getSummary().getHeadline() != null) {
            md.append("### ").append(cv.getSummary().getHeadline()).append("\n");
        }
        md.append("\n");
        
        if (cv.getSummary().getDescription() != null) {
            md.append(cv.getSummary().getDescription()).append("\n\n");
        } else if (cv.getHeader().getBio() != null) {
            md.append(cv.getHeader().getBio()).append("\n\n");
        }
    }

    private void appendContactInfo(StringBuilder md, TechnicalCV cv) {
        md.append("## 📧 Contacto\n\n");
        if (cv.getHeader().getLocation() != null) md.append("- 📍 **Ubicación**: ").append(cv.getHeader().getLocation()).append("\n");
        if (cv.getHeader().getEmail() != null) md.append("- 📧 **Email**: ").append(cv.getHeader().getEmail()).append("\n");
        md.append("- 💻 **GitHub**: [github.com/").append(cv.getHeader().getUsername()).append("](")
          .append(cv.getHeader().getGithub()).append(")\n\n");
    }

    private void appendArchitectures(StringBuilder md, TechnicalMetadata techMetadata) {
        if (!techMetadata.architectures().isEmpty()) {
            md.append("## 🏗️ Arquitectura & Patrones\n\n");
            techMetadata.architectures().forEach((name, stats) -> 
                md.append("- **").append(name).append("** (Detectado en ").append(stats.getProjectCount()).append(" proyectos)\n"));
            md.append("\n");
        }
    }

    private void appendLanguages(StringBuilder md, TechnicalMetadata techMetadata) {
        if (!techMetadata.languages().isEmpty()) {
            md.append("## 💻 Lenguajes Principales\n\n");
            techMetadata.languages().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getProjectCount(), e1.getValue().getProjectCount()))
                .limit(8)
                .forEach(entry -> md.append("- **").append(entry.getKey()).append("**: ").append(entry.getValue().getProjectCount()).append(" proyectos\n"));
            md.append("\n");
        }
    }

    private void appendFrameworks(StringBuilder md, TechnicalMetadata techMetadata) {
        if (!techMetadata.frameworks().isEmpty()) {
            md.append("## 🚀 Frameworks & Tecnologías\n\n");
            techMetadata.frameworks().entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getProjectCount(), e1.getValue().getProjectCount()))
                .limit(10)
                .forEach(entry -> md.append("- **").append(entry.getKey()).append("**: ").append(entry.getValue().getProjectCount()).append(" proyectos\n"));
            md.append("\n");
        }
    }

    private void appendCICD(StringBuilder md, TechnicalMetadata techMetadata) {
        if (!techMetadata.cicdTools().isEmpty()) {
            md.append("## ⚙️ DevOps & Tools\n\n");
            techMetadata.cicdTools().forEach(tool -> md.append("- ").append(tool).append("\n"));
            md.append("\n");
        }
    }

    private void appendOpenSource(StringBuilder md, TechnicalMetadata techMetadata) {
        if (!techMetadata.openSourceProjects().isEmpty()) {
            md.append("## 🌟 Proyectos Open Source Destacados\n\n");
            techMetadata.openSourceProjects().stream().limit(5).forEach(p -> {
                md.append("### ").append(p.name()).append("\n");
                md.append(p.description()).append("\n\n");
                md.append("- ⭐ ").append(p.stars()).append(" Stars | 🔗 [Ver Repositorio](").append(p.url()).append(")\n");
                if (p.topics() != null && !p.topics().isEmpty()) {
                    md.append("- *Stack*: ").append(String.join(", ", p.topics().subList(0, Math.min(5, p.topics().size())))).append("\n");
                }
                md.append("\n");
            });
        }
    }

    public String generateHtmlPreview(TechnicalCV cv) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; max-width: 800px; margin: 2rem auto; line-height: 1.6; color: #333; }
                    h1 { color: #2c3e50; margin-bottom: 0.5rem; }
                    h2 { color: #34495e; border-bottom: 2px solid #eee; padding-bottom: 0.5rem; margin-top: 2rem; }
                    h3 { color: #7f8c8d; margin-top: 0; font-weight: normal; }
                    .badge { background: #e1f5fe; color: #0277bd; padding: 0.2rem 0.6rem; border-radius: 12px; margin-right: 0.5rem; display: inline-block; margin-bottom: 0.5rem; font-size: 0.9rem; }
                    .header { text-align: center; background: #f9f9f9; padding: 2rem; border-radius: 8px; margin-bottom: 2rem; }
                    .stats { display: flex; justify-content: space-around; background: #f5f5f5; padding: 1rem; border-radius: 8px; margin-top: 1rem; }
                    .stat-box { text-align: center; }
                    .stat-value { font-size: 1.5rem; font-weight: bold; color: #2c3e50; }
                    .stat-label { font-size: 0.8rem; color: #7f8c8d; text-transform: uppercase; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>%s</h1>
                    <h3>%s</h3>
                    <p>📍 %s</p>
                </div>
                
                <h2>Professional Summary</h2>
                <p>%s</p>
                
                <h2>Top Skills</h2>
                <div>%s</div>
                
                <h2>Experience Stats</h2>
                <div class="stats">
                    <div class="stat-box">
                        <div class="stat-value">%d</div>
                        <div class="stat-label">Total Projects</div>
                    </div>
                    <div class="stat-box">
                        <div class="stat-value">%d</div>
                        <div class="stat-label">Public Projects</div>
                    </div>
                    <div class="stat-box">
                        <div class="stat-value">%d</div>
                        <div class="stat-label">Active Years</div>
                    </div>
                </div>
            </body>
            </html>
            """, 
            cv.getHeader().getName(),
            cv.getSummary().getHeadline(),
            cv.getHeader().getLocation() != null ? cv.getHeader().getLocation() : "Remote",
            cv.getSummary().getDescription(),
            cv.getSummary().getPrimaryTechnologies().stream().map(t -> "<span class='badge'>"+t+"</span>").reduce("", String::concat),
            cv.getSummary().getTotalProjects(),
            cv.getSummary().getPublicProjects(),
            cv.getSummary().getYearsActive()
        );
    }
}

