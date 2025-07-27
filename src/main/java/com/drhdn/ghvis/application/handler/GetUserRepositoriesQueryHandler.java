package com.drhdn.ghvis.application.handler;

import com.drhdn.ghvis.application.query.GetUserRepositoriesQuery;
import com.drhdn.ghvis.application.usecase.GetUserRepositoriesUseCase;
import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.port.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Handler para procesar queries de repositorios de usuario.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetUserRepositoriesQueryHandler {
    
    private final GetUserRepositoriesUseCase getUserRepositoriesUseCase;
    private final CacheService cacheService;
    
    /**
     * Maneja la query para obtener repositorios de un usuario.
     * 
     * @param query La query a procesar
     * @return Flux con los repositorios del usuario
     */
    public Flux<Repository> handle(GetUserRepositoriesQuery query) {
        log.info("🔍 Ejecutando query de repositorios para usuario: {} (QueryId: {})",
            query.getUsername(), query.getQueryId());
        
        String cacheKey = query.getCacheKey();
        
        return cacheService.getOrFetch(cacheKey, () -> {
            log.info("🌐 Consultando repositorios frescos para usuario: {}", query.getUsername());
            return executeUseCase(query);
        })
        .flatMapMany(Flux::fromIterable)
        .doOnComplete(() -> {
            log.info("✅ Query completada exitosamente para usuario: {} (QueryId: {})",
                query.getUsername(), query.getQueryId());
        })
        .doOnError(error -> {
            log.error("❌ Error en query para usuario: {} (QueryId: {}): {}",
                query.getUsername(), query.getQueryId(), error.getMessage());
        });
    }
    
    /**
     * Maneja query para obtener todos los repositorios de un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @return Flux con todos los repositorios
     */
    public Flux<Repository> handleAllQuery(String username, java.security.Principal principal) {
        GetUserRepositoriesQuery query = GetUserRepositoriesQuery.createAll(username, principal);
        return handle(query);
    }
    
    /**
     * Maneja query para obtener repositorios públicos de un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @return Flux con los repositorios públicos
     */
    public Flux<Repository> handlePublicQuery(String username, java.security.Principal principal) {
        GetUserRepositoriesQuery query = GetUserRepositoriesQuery.createPublic(username, principal);
        return handle(query);
    }
    
    /**
     * Maneja query para obtener repositorios con detalles de un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @return Flux con los repositorios detallados
     */
    public Flux<Repository> handleDetailedQuery(String username, java.security.Principal principal) {
        GetUserRepositoriesQuery query = GetUserRepositoriesQuery.createDetailed(username, principal);
        return handle(query);
    }
    
    /**
     * Maneja query personalizada para obtener repositorios de un usuario.
     * 
     * @param username El nombre de usuario
     * @param principal El principal del usuario autenticado
     * @param repositoryType El tipo de repositorios
     * @param includeDetails Si incluir detalles
     * @return Flux con los repositorios
     */
    public Flux<Repository> handleCustomQuery(String username, java.security.Principal principal, 
                                            String repositoryType, boolean includeDetails) {
        GetUserRepositoriesQuery query = GetUserRepositoriesQuery.createCustom(
            username, principal, repositoryType, includeDetails);
        return handle(query);
    }
    
    /**
     * Limpia el cache de repositorios de un usuario.
     * 
     * @param username El nombre de usuario
     */
    public void clearCache(String username) {
        String[] cacheKeys = {
            String.format("user:%s:repositories:all", username),
            String.format("user:%s:repositories:public", username),
            String.format("user:%s:repositories:detailed", username)
        };
        
        for (String cacheKey : cacheKeys) {
            cacheService.clear(cacheKey);
        }
        
        log.info("🧹 Cache limpiado para usuario: {}", username);
    }
    
    /**
     * Ejecuta el use case apropiado basado en el tipo de query.
     * 
     * @param query La query a procesar
     * @return Mono con la lista de repositorios
     */
    private reactor.core.publisher.Mono<List<Repository>> executeUseCase(GetUserRepositoriesQuery query) {
        if (query.isDetailedRepositories()) {
            return getUserRepositoriesUseCase.executeWithDetails(query.getUsername(), query.getPrincipal())
                .collectList();
        } else if (query.isPublicRepositories()) {
            return getUserRepositoriesUseCase.executePublic(query.getUsername(), query.getPrincipal())
                .collectList();
        } else {
            return getUserRepositoriesUseCase.execute(query.getUsername(), query.getPrincipal())
                .collectList();
        }
    }
} 