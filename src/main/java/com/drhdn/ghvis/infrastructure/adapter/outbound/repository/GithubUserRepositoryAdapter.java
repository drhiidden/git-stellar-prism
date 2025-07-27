package com.drhdn.ghvis.infrastructure.adapter.outbound.repository;

import com.drhdn.ghvis.domain.entity.User;
import com.drhdn.ghvis.domain.port.UserRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

/**
 * Adapter de infraestructura para operaciones de usuario usando GitHub API.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GithubUserRepositoryAdapter implements UserRepository {
    
    private final GithubApiAdapter githubApiAdapter;
    
    @Override
    public Mono<User> getCurrentUser(Principal principal) {
        log.info("🔍 Obteniendo usuario actual para: {}", principal.getName());
        
        return githubApiAdapter.getCurrentUser(principal)
            .map(this::mapToUser)
            .doOnSuccess(user -> log.info("✅ Usuario actual obtenido: {}", user.getLogin()))
            .doOnError(error -> log.error("❌ Error obteniendo usuario actual: {}", error.getMessage()));
    }
    
    @Override
    public Mono<User> getUserByLogin(String login, Principal principal) {
        log.info("🔍 Obteniendo usuario por login: {}", login);
        
        return githubApiAdapter.getUserByLogin(login, principal)
            .map(this::mapToUser)
            .doOnSuccess(user -> log.info("✅ Usuario obtenido por login: {}", user.getLogin()))
            .doOnError(error -> log.error("❌ Error obteniendo usuario por login {}: {}", login, error.getMessage()));
    }
    
    @Override
    public Mono<User> getUserById(Long userId, Principal principal) {
        log.info("🔍 Obteniendo usuario por ID: {}", userId);
        
        return githubApiAdapter.getUserById(userId, principal)
            .map(this::mapToUser)
            .doOnSuccess(user -> log.info("✅ Usuario obtenido por ID: {}", user.getId()))
            .doOnError(error -> log.error("❌ Error obteniendo usuario por ID {}: {}", userId, error.getMessage()));
    }
    
    @Override
    public Mono<Boolean> userExists(String login, Principal principal) {
        log.info("🔍 Verificando existencia de usuario: {}", login);
        
        return getUserByLogin(login, principal)
            .map(user -> true)
            .onErrorReturn(false)
            .doOnSuccess(exists -> log.info("✅ Usuario {} existe: {}", login, exists))
            .doOnError(error -> log.error("❌ Error verificando existencia de usuario {}: {}", login, error.getMessage()));
    }
    
    @Override
    public Mono<UserStats> getUserStats(String login, Principal principal) {
        log.info("🔍 Obteniendo estadísticas de usuario: {}", login);
        
        return getUserByLogin(login, principal)
            .map(this::buildUserStats)
            .doOnSuccess(stats -> log.info("✅ Estadísticas obtenidas para usuario: {}", login))
            .doOnError(error -> log.error("❌ Error obteniendo estadísticas de usuario {}: {}", login, error.getMessage()));
    }
    
    /**
     * Mapea la respuesta de GitHub API a la entidad User del dominio.
     */
    private User mapToUser(Map<String, Object> githubUser) {
        return User.builder()
            .id(getLongValue(githubUser, "id"))
            .login(getStringValue(githubUser, "login"))
            .name(getStringValue(githubUser, "name"))
            .email(getStringValue(githubUser, "email"))
            .avatarUrl(getStringValue(githubUser, "avatar_url"))
            .bio(getStringValue(githubUser, "bio"))
            .location(getStringValue(githubUser, "location"))
            .publicRepos(getIntegerValue(githubUser, "public_repos"))
            .totalPrivateRepos(getIntegerValue(githubUser, "total_private_repos"))
            .followers(getIntegerValue(githubUser, "followers"))
            .following(getIntegerValue(githubUser, "following"))
            .createdAt(parseInstant(githubUser, "created_at"))
            .updatedAt(parseInstant(githubUser, "updated_at"))
            .htmlUrl(getStringValue(githubUser, "html_url"))
            .type(getStringValue(githubUser, "type"))
            .verified(getBooleanValue(githubUser, "verified"))
            .bot(getBooleanValue(githubUser, "bot"))
            .siteAdmin(getBooleanValue(githubUser, "site_admin"))
            .build();
    }
    
    /**
     * Construye estadísticas de usuario basadas en la entidad User.
     */
    private UserStats buildUserStats(User user) {
        return new UserStats() {
            @Override
            public Integer getTotalRepositories() {
                return user.getTotalRepos();
            }
            
            @Override
            public Integer getPublicRepositories() {
                return user.getPublicRepos();
            }
            
            @Override
            public Integer getPrivateRepositories() {
                return user.getTotalPrivateRepos();
            }
            
            @Override
            public Integer getFollowers() {
                return user.getFollowers();
            }
            
            @Override
            public Integer getFollowing() {
                return user.getFollowing();
            }
            
            @Override
            public Integer getTotalStars() {
                // TODO: Implementar obtención de stars totales
                return 0;
            }
            
            @Override
            public Integer getTotalForks() {
                // TODO: Implementar obtención de forks totales
                return 0;
            }
            
            @Override
            public java.time.Instant getAccountCreatedAt() {
                return user.getCreatedAt();
            }
            
            @Override
            public java.time.Instant getLastActivityAt() {
                return user.getUpdatedAt();
            }
        };
    }
    
    /**
     * Obtiene un valor Long de forma segura del mapa.
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    /**
     * Obtiene un valor String de forma segura del mapa.
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Obtiene un valor Integer de forma segura del mapa.
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    /**
     * Obtiene un valor Boolean de forma segura del mapa.
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
    
    /**
     * Parsea un Instant de forma segura del mapa.
     */
    private Instant parseInstant(Map<String, Object> map, String key) {
        String value = getStringValue(map, key);
        if (value != null) {
            try {
                return Instant.parse(value);
            } catch (Exception e) {
                log.warn("⚠️ Error parseando fecha para {}: {}", key, value);
            }
        }
        return null;
    }
} 