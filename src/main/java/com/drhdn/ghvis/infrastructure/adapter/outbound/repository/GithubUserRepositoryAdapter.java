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
 * Adapter para operaciones de usuario usando GitHub API.
 * Implementa el puerto UserRepository siguiendo arquitectura hexagonal.
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
        log.debug("🔍 Obteniendo usuario actual para: {}", 
            principal != null ? principal.getName() : "anonymous");
        
        return githubApiAdapter.getCurrentUser(principal)
            .map(this::mapToUser)
            .doOnSuccess(user -> log.debug("✅ Usuario actual obtenido: {}", user.getLogin()))
            .doOnError(error -> log.error("❌ Error obteniendo usuario actual: {}", error.getMessage()))
            .onErrorResume(error -> Mono.error(new RuntimeException("Error al obtener usuario actual", error)));
    }

    @Override
    public Mono<User> getUserByLogin(String login, Principal principal) {
        log.debug("🔍 Obteniendo usuario por login: {}", login);
        
        return githubApiAdapter.getUserByLogin(login, principal)
            .map(this::mapToUser)
            .doOnSuccess(user -> log.debug("✅ Usuario obtenido por login: {}", login))
            .doOnError(error -> log.error("❌ Error obteniendo usuario por login {}: {}", login, error.getMessage()))
            .onErrorResume(error -> Mono.error(new RuntimeException("Error al obtener usuario: " + login, error)));
    }

    @Override
    public Mono<User> getUserById(Long userId, Principal principal) {
        log.debug("🔍 Obteniendo usuario por ID: {}", userId);
        
        return githubApiAdapter.getUserById(userId, principal)
            .map(this::mapToUser)
            .doOnSuccess(user -> log.debug("✅ Usuario obtenido por ID: {}", userId))
            .doOnError(error -> log.error("❌ Error obteniendo usuario por ID {}: {}", userId, error.getMessage()))
            .onErrorResume(error -> Mono.error(new RuntimeException("Error al obtener usuario ID: " + userId, error)));
    }

    @Override
    public Mono<Boolean> userExists(String login, Principal principal) {
        log.debug("🔍 Verificando existencia de usuario: {}", login);
        
        return getUserByLogin(login, principal)
            .map(user -> true)
            .onErrorReturn(false)
            .doOnNext(exists -> log.debug("✅ Usuario {} existe: {}", login, exists));
    }

    @Override
    public Mono<UserRepository.UserStats> getUserStats(String login, Principal principal) {
        log.debug("🔍 Obteniendo estadísticas de usuario: {}", login);
        
        return getUserByLogin(login, principal)
            .<UserRepository.UserStats>map(user -> new UserStatsImpl(user, principal))
            .doOnSuccess(stats -> log.debug("✅ Estadísticas obtenidas para usuario: {}", login))
            .doOnError(error -> log.error("❌ Error obteniendo estadísticas de usuario {}: {}", login, error.getMessage()));
    }

    /**
     * Convierte un mapa de respuesta de la API a un objeto User.
     * 
     * @param userMap Mapa con datos del usuario desde GitHub API
     * @return Objeto User
     */
    private User mapToUser(Map<String, Object> userMap) {
        return User.builder()
            .id(getLong(userMap, "id"))
            .login(getString(userMap, "login"))
            .name(getString(userMap, "name"))
            .email(getString(userMap, "email"))
            .avatarUrl(getString(userMap, "avatar_url"))
            .bio(getString(userMap, "bio"))
            .location(getString(userMap, "location"))
            .publicRepos(getInt(userMap, "public_repos"))
            .followers(getInt(userMap, "followers"))
            .following(getInt(userMap, "following"))
            .createdAt(getInstant(userMap, "created_at"))
            .updatedAt(getInstant(userMap, "updated_at"))
            .htmlUrl(getString(userMap, "html_url"))
            .build();
    }

    /**
     * Implementación de UserStats que calcula estadísticas del usuario.
     */
    private static class UserStatsImpl implements UserRepository.UserStats {
        private final User user;
        private final Principal principal;

        public UserStatsImpl(User user, Principal principal) {
            this.user = user;
            this.principal = principal;
        }

        @Override
        public Integer getTotalRepositories() {
            return user.getPublicRepos(); // Solo repos públicos por ahora
        }

        @Override
        public Integer getPublicRepositories() {
            return user.getPublicRepos();
        }

        @Override
        public Integer getPrivateRepositories() {
            return 0; // No disponible en la API pública
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
            return 0; // Requiere consulta adicional
        }

        @Override
        public Integer getTotalForks() {
            return 0; // Requiere consulta adicional
        }

        @Override
        public Instant getAccountCreatedAt() {
            return user.getCreatedAt();
        }

        @Override
        public Instant getLastActivityAt() {
            return user.getUpdatedAt();
        }
    }

    // Métodos de utilidad para extraer valores de mapas

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    private Instant getInstant(Map<String, Object> map, String key) {
        String dateStr = getString(map, key);
        try {
            return dateStr != null ? Instant.parse(dateStr) : null;
        } catch (Exception e) {
            log.warn("⚠️ Error parseando fecha {}: {}", key, dateStr);
            return null;
        }
    }
} 