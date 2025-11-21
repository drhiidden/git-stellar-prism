package com.drhdn.ghvis.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entidad que representa un CV Técnico generado desde GitHub.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalCV {
    
    /**
     * Metadata del CV
     */
    private CVMetadata metadata;
    
    /**
     * Información personal
     */
    private CVHeader header;
    
    /**
     * Resumen ejecutivo
     */
    private CVSummary summary;
    
    /**
     * Sección de tecnologías
     */
    private CVTechnologies technologies;
    
    /**
     * Experiencia por tecnología
     */
    private List<CVExperience> experience;
    
    /**
     * Proyectos destacados
     */
    private List<CVProject> projects;
    
    /**
     * Actividad en GitHub
     */
    private CVActivity activity;
    
    /**
     * Habilidades derivadas
     */
    private CVSkills skills;
    
    /**
     * Estadísticas generales
     */
    private CVStatistics statistics;

    /**
     * Prompt optimizado para generar resumen con IA
     */
    private String aiPrompt;
    
    // ========== INNER CLASSES ==========
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVMetadata {
        private Instant generatedAt;
        private String version;
        private String source;
        private Integer totalRepositories;
        private String githubProfile;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVHeader {
        private String name;
        private String username;
        private String role;
        private String bio;
        private String location;
        private String email;
        private String website;
        private String avatarUrl;
        private String github;
        private Integer followers;
        private Integer following;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVSummary {
        private String role;
        private Integer yearsActive;
        private Integer totalProjects;
        private Integer publicProjects;
        private List<String> primaryTechnologies;
        private String headline;
        private String description;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVTechnologies {
        private TechSummary summary;
        private Map<String, TechCategory> byCategory;
        private TechByProficiency byProficiency;
        private Map<String, TechYearData> timeline;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechSummary {
        private Integer total;
        private Integer categories;
        private List<TechItem> primary;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechItem {
        private String name;
        private Integer count;
        private String icon;
        private String proficiency;
        private Double percentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechCategory {
        private String icon;
        private Integer priority;
        private List<TechItemDetailed> technologies;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechItemDetailed {
        private String name;
        private String icon;
        private Integer count;
        private String proficiency;
        private List<String> repos;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechByProficiency {
        private List<TechItem> expert;
        private List<TechItem> advanced;
        private List<TechItem> intermediate;
        private List<TechItem> basic;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechYearData {
        private List<String> languages;
        private List<String> frameworks;
        private Integer count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVExperience {
        private String technology;
        private String icon;
        private String category;
        private Integer yearsOfExperience;
        private Integer projectCount;
        private String proficiency;
        private List<String> repos;
        private String firstUsed;
        private String lastUsed;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVProject {
        private String name;
        private String fullName;
        private String description;
        private String url;
        private String homepage;
        private Integer stars;
        private Integer forks;
        private Integer watchers;
        private String language;
        private Map<String, Long> languages;
        private List<ProjectTech> technologies;
        private List<String> topics;
        private String createdAt;
        private String updatedAt;
        private Boolean isActive;
        private Boolean isFork;
        private String visibility;
        private Integer score;
        private List<String> highlights;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectTech {
        private String name;
        private String icon;
        private String category;
        private Double confidence;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVActivity {
        private Integer totalCommits;
        private Integer activeYears;
        private YearActivity mostActiveYear;
        private RecentActivity recentActivity;
        private Integer consistency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearActivity {
        private String year;
        private Integer projects;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private Integer last30Days;
        private Integer last90Days;
        private String status;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVSkills {
        private List<String> technical;
        private List<String> soft;
        private List<DomainExpertise> domain;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainExpertise {
        private String domain;
        private Integer projectCount;
        private String proficiency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CVStatistics {
        private Integer totalRepositories;
        private Integer publicRepositories;
        private Integer privateRepositories;
        private Integer forkedRepositories;
        private Integer originalRepositories;
        private Integer totalStars;
        private Integer totalForks;
        private Integer averageStarsPerRepo;
        private String mostStarredRepo;
        private List<LanguageDistribution> languageDistribution;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageDistribution {
        private String language;
        private Integer count;
        private Integer percentage;
    }
}

