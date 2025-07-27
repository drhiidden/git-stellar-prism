package com.drhdn.ghvis.infrastructure.persistence.repository;

import com.drhdn.ghvis.infrastructure.persistence.entity.RepoCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoCacheRepository extends JpaRepository<RepoCache, String> {
} 