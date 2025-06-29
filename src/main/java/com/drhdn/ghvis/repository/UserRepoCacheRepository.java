package com.drhdn.ghvis.repository;

import com.drhdn.ghvis.entity.UserRepoCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepoCacheRepository extends JpaRepository<UserRepoCache, String> {
    
    /**
     * Busca caché por username y tipo
     */
    Optional<UserRepoCache> findByUsernameAndCacheType(String username, String cacheType);
    
    /**
     * Elimina caché expirado
     */
    void deleteByUpdatedAtBefore(Instant expiredBefore);
    
    /**
     * Obtiene estadísticas de caché
     */
    @Query("SELECT COUNT(u) FROM UserRepoCache u WHERE u.updatedAt > :since")
    long countValidCacheEntries(Instant since);
    
    /**
     * Lista todos los usuarios con caché activo
     */
    @Query("SELECT u.username FROM UserRepoCache u WHERE u.updatedAt > :since")
    List<String> findActiveUsers(Instant since);
} 