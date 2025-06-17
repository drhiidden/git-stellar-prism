package com.drhdn.ghvis.entity;

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

@Entity
@Table(name = "repo_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepoCache {

    @Id
    @Column(name = "repo", nullable = false, length = 200)
    private String repo; // formato owner/repo

    @Lob
    @Column(name = "commits_json", columnDefinition = "CLOB")
    private String commitsJson;

    @Column(name = "updated_at")
    private Instant updatedAt;
} 