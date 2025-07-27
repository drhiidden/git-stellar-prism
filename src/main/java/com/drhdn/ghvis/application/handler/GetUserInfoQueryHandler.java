package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetUserInfoQuery;
import com.drhdn.ghvis.domain.entity.User;
import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Handler para procesar queries de información de usuario.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetUserInfoQueryHandler {
    
    private final UserRepository userRepository;
    private final CacheService cacheService;
    
    /**
     * Maneja la query para obtener información de un usuario.
     * 
     * @param query La query a procesar
     * @return Mono con la información del usuario
     */
    public Mono<User> handle(GetUserInfoQuery query) {
        log.info("🔍 Ejecutando query de información para usuario: {} (QueryId: {})",
            query.getUsername(), query.getQueryId());
        
        String cacheKey = query.getCacheKey();
        
        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando información fresca para usuario: {}", query.getUsername());
            return userRepository.getCurrentUser(query.getPrincipal());
        })
        .doOnSuccess(user -> {
            log.info("✅ Query completada exitosamente para usuario: {} (QueryId: {})",
                query.getUsername(), query.getQueryId());
        })
        .doOnError(error -> {
            log.error("❌ Error en query para usuario: {} (QueryId: {}): {}",
                query.getUsername(), query.getQueryId(), error.getMessage());
        });
    }
    
    /**
     * Maneja query para obtener información del usuario actual.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @return Mono con la información del usuario
     */
    public Mono<User> handleCurrentUserQuery(String username, java.security.Principal principal) {
        GetUserInfoQuery query = GetUserInfoQuery.create(username, principal);
        return handle(query);
    }
    
    /**
     * Limpia el cache de información de un usuario.
     * 
     * @param username El nombre de usuario
     */
    public void clearCache(String username) {
        String cacheKey = String.format("user:%s:info", username);
        cacheService.clear(cacheKey);
        log.info("🧹 Cache limpiado para usuario: {}", username);
    }
} 