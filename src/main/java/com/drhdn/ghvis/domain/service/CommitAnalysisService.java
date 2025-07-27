package com.drhdn.ghvis.domain.service;

import com.drhdn.ghvis.domain.entity.Commit;
import com.drhdn.ghvis.domain.valueobject.CommitSha;
import com.drhdn.ghvis.domain.valueobject.RepositoryName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para análisis de commits.
 * 
 * Este servicio implementa lógica de negocio pura relacionada con el análisis
 * de commits, sin dependencias de infraestructura o frameworks externos.
 */
@Service
@Slf4j
public class CommitAnalysisService {

    /**
     * Analiza la frecuencia de commits en un repositorio.
     * 
     * @param commits Lista de commits a analizar
     * @return Mapa con estadísticas de frecuencia
     */
    public Map<String, Object> analyzeCommitFrequency(List<Commit> commits) {
        if (commits == null || commits.isEmpty()) {
            return Map.of(
                "total_commits", 0,
                "average_commits_per_day", 0.0,
                "most_active_day", "N/A",
                "commit_frequency", "LOW"
            );
        }

        // Agrupar commits por día
        Map<String, Long> commitsPerDay = commits.stream()
            .collect(Collectors.groupingBy(
                commit -> commit.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                Collectors.counting()
            ));

        // Calcular estadísticas
        long totalCommits = commits.size();
        double averageCommitsPerDay = commitsPerDay.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);

        String mostActiveDay = commitsPerDay.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");

        String frequency = determineCommitFrequency(averageCommitsPerDay);

        return Map.of(
            "total_commits", totalCommits,
            "average_commits_per_day", averageCommitsPerDay,
            "most_active_day", mostActiveDay,
            "commit_frequency", frequency,
            "days_with_commits", commitsPerDay.size()
        );
    }

    /**
     * Analiza la actividad de desarrolladores en un repositorio.
     * 
     * @param commits Lista de commits a analizar
     * @return Mapa con estadísticas de desarrolladores
     */
    public Map<String, Object> analyzeDeveloperActivity(List<Commit> commits) {
        if (commits == null || commits.isEmpty()) {
            return Map.of(
                "total_developers", 0,
                "top_contributor", "N/A",
                "developer_activity", Map.of()
            );
        }

        // Agrupar commits por autor
        Map<String, Long> commitsPerAuthor = commits.stream()
            .collect(Collectors.groupingBy(
                Commit::getAuthor,
                Collectors.counting()
            ));

        String topContributor = commitsPerAuthor.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");

        return Map.of(
            "total_developers", commitsPerAuthor.size(),
            "top_contributor", topContributor,
            "developer_activity", commitsPerAuthor
        );
    }

    /**
     * Valida si un SHA de commit es válido.
     * 
     * @param sha SHA a validar
     * @return true si es válido, false en caso contrario
     */
    public boolean isValidCommitSha(String sha) {
        if (sha == null || sha.trim().isEmpty()) {
            return false;
        }
        
        return sha.matches("^[a-fA-F0-9]{7,40}$");
    }

    /**
     * Valida si un nombre de repositorio es válido.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return true si es válido, false en caso contrario
     */
    public boolean isValidRepositoryName(String owner, String repo) {
        if (owner == null || owner.trim().isEmpty() || 
            repo == null || repo.trim().isEmpty()) {
            return false;
        }
        
        return owner.matches("^[a-zA-Z0-9_-]+$") && 
               repo.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Calcula la edad de un commit en días.
     * 
     * @param commit Commit a analizar
     * @return Edad en días
     */
    public long calculateCommitAge(Commit commit) {
        if (commit == null || commit.getTimestamp() == null) {
            return -1;
        }
        
        return Duration.between(commit.getTimestamp(), Instant.now()).toDays();
    }

    /**
     * Determina la frecuencia de commits basada en el promedio diario.
     * 
     * @param averageCommitsPerDay Promedio de commits por día
     * @return Categoría de frecuencia
     */
    private String determineCommitFrequency(double averageCommitsPerDay) {
        if (averageCommitsPerDay >= 10) {
            return "VERY_HIGH";
        } else if (averageCommitsPerDay >= 5) {
            return "HIGH";
        } else if (averageCommitsPerDay >= 2) {
            return "MEDIUM";
        } else if (averageCommitsPerDay >= 0.5) {
            return "LOW";
        } else {
            return "VERY_LOW";
        }
    }

    /**
     * Crea un CommitSha validado.
     * 
     * @param sha String del SHA
     * @return CommitSha validado
     * @throws IllegalArgumentException si el SHA no es válido
     */
    public CommitSha createCommitSha(String sha) {
        if (!isValidCommitSha(sha)) {
            throw new IllegalArgumentException("SHA de commit inválido: " + sha);
        }
        return new CommitSha(sha);
    }

    /**
     * Crea un RepositoryName validado.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @return RepositoryName validado
     * @throws IllegalArgumentException si el nombre no es válido
     */
    public RepositoryName createRepositoryName(String owner, String repo) {
        if (!isValidRepositoryName(owner, repo)) {
            throw new IllegalArgumentException("Nombre de repositorio inválido: " + owner + "/" + repo);
        }
        return new RepositoryName(owner, repo);
    }
} 