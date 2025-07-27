package com.drhdn.ghvis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entidad para cachear los repositorios del usuario con información detallada.
 * Separada del caché de commits para mejor organización y TTL independiente.
 */
@Entity
@Table(name = "user_repo_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRepoCache {

    @Id
    @Column(name = "username", nullable = false, length = 100)
    private String username; // usuario de GitHub

    @Lob
    @Column(name = "repositories_json", columnDefinition = "CLOB")
    private String repositoriesJson; // JSON con lista de repositorios

    @Column(name = "total_count")
    private Integer totalCount; // número total de repositorios

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "cache_type", length = 20)
    private String cacheType; // "basic" o "detailed"
} 