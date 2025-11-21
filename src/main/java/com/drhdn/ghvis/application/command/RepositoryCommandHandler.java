package com.drhdn.ghvis.application.command;

import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import com.drhdn.ghvis.infrastructure.adapter.outbound.external.GithubApiAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Manejador de comandos relacionados con repositorios.
 * 
 * Responsabilidades:
 * - Refrescar datos desde GitHub API
 * - Invalidar cachés
 * - Validar comandos
 * - Publicar eventos de cambios
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryCommandHandler {
    
    private final GithubApiAdapter githubApiAdapter;
    private final RepositoryRepository repositoryRepository;
    private final CacheManager cacheManager;
    
    /**
     * Maneja el comando RefreshRepositories.
     * 
     * Flujo:
     * 1. Validar comando
     * 2. Obtener repos desde GitHub API
     * 3. Invalidar caché si forceRefresh
     * 4. Retornar lista de repos
     * 5. Publicar evento (futuro)
     */
    public Mono<List<Repository>> handle(RepositoryCommand.RefreshRepositories command) {
        log.info("🔄 Ejecutando comando: {} para usuario: {}", 
            command.getCommandName(), command.username());
        
        return validateRefreshRepositories(command)
            .then(Mono.defer(() -> {
                // Si forceRefresh, invalidar caché primero
                if (command.forceRefresh()) {
                    invalidateCacheFor(command.username());
                }
                
                // Obtener repos desde GitHub
                return githubApiAdapter.getUserRepositories(command.principal())
                    .collectList()
                    .doOnSuccess(repos -> {
                        log.info("✅ Comando RefreshRepositories completado: {} repos obtenidos", 
                            repos.size());
                        // TODO: Publicar evento RepositoriesRefreshed
                    })
                    .doOnError(error -> {
                        log.error("❌ Error ejecutando RefreshRepositories: {}", 
                            error.getMessage());
                    });
            }));
    }
    
    /**
     * Maneja el comando InvalidateCache.
     * 
     * Flujo:
     * 1. Validar comando
     * 2. Limpiar caché L1 (in-memory)
     * 3. Limpiar caché L2 (Redis) si existe
     * 4. Publicar evento (futuro)
     */
    public Mono<Void> handle(RepositoryCommand.InvalidateCache command) {
        log.info("🗑️ Ejecutando comando: InvalidateCache para usuario: {} - Razón: {}", 
            command.username(), command.reason());
        
        return validateInvalidateCache(command)
            .then(Mono.fromRunnable(() -> {
                invalidateCacheFor(command.username());
                log.info("✅ Caché invalidado para usuario: {}", command.username());
                // TODO: Publicar evento CacheInvalidated
            }));
    }
    
    /**
     * Maneja el comando RefreshSingleRepository.
     * 
     * Flujo:
     * 1. Validar comando
     * 2. Obtener repo específico desde GitHub
     * 3. Actualizar en caché
     * 4. Aplicar cambios si se especificaron
     */
    public Mono<Repository> handle(RepositoryCommand.RefreshSingleRepository command) {
        log.info("🔄 Ejecutando comando: RefreshSingleRepository para: {}", 
            command.getFullName());
        
        return validateRefreshSingleRepository(command)
            .then(repositoryRepository.findByOwnerAndName(
                command.owner(), 
                command.repoName(), 
                command.principal()
            ))
            .doOnSuccess(repo -> {
                log.info("✅ Repositorio {} refrescado correctamente", command.getFullName());
                
                // Aplicar cambios si se especificaron
                if (!command.changes().isEmpty()) {
                    log.debug("Aplicando {} cambios al repositorio", 
                        command.changes().size());
                    // TODO: Aplicar cambios dinámicamente
                }
                
                // TODO: Publicar evento RepositoryRefreshed
            })
            .doOnError(error -> {
                log.error("❌ Error refrescando repositorio {}: {}", 
                    command.getFullName(), error.getMessage());
            });
    }
    
    // ========== Validaciones ==========
    
    private Mono<Void> validateRefreshRepositories(RepositoryCommand.RefreshRepositories command) {
        if (command.username() == null || command.username().isBlank()) {
            return Mono.error(new IllegalArgumentException(
                "Username no puede ser nulo o vacío"));
        }
        
        if (command.principal() == null) {
            return Mono.error(new IllegalArgumentException(
                "Principal no puede ser nulo"));
        }
        
        return Mono.empty();
    }
    
    private Mono<Void> validateInvalidateCache(RepositoryCommand.InvalidateCache command) {
        if (command.username() == null || command.username().isBlank()) {
            return Mono.error(new IllegalArgumentException(
                "Username no puede ser nulo o vacío"));
        }
        
        return Mono.empty();
    }
    
    private Mono<Void> validateRefreshSingleRepository(
        RepositoryCommand.RefreshSingleRepository command) {
        
        if (command.owner() == null || command.owner().isBlank()) {
            return Mono.error(new IllegalArgumentException(
                "Owner no puede ser nulo o vacío"));
        }
        
        if (command.repoName() == null || command.repoName().isBlank()) {
            return Mono.error(new IllegalArgumentException(
                "RepoName no puede ser nulo o vacío"));
        }
        
        if (command.principal() == null) {
            return Mono.error(new IllegalArgumentException(
                "Principal no puede ser nulo"));
        }
        
        return Mono.empty();
    }
    
    // ========== Utilidades ==========
    
    /**
     * Invalida todos los cachés relacionados con un usuario.
     */
    private void invalidateCacheFor(String username) {
        try {
            // Caché de repositorios
            var repoCache = cacheManager.getCache("repositories");
            if (repoCache != null) {
                repoCache.evict(username);
                log.debug("📦 Caché 'repositories' invalidado para: {}", username);
            }
            
            // Caché de metadata
            var metadataCache = cacheManager.getCache("metadata");
            if (metadataCache != null) {
                metadataCache.evict("metadata:technical:" + username);
                log.debug("📦 Caché 'metadata' invalidado para: {}", username);
            }
            
            // Caché de lenguajes
            var languagesCache = cacheManager.getCache("languages");
            if (languagesCache != null) {
                languagesCache.evict("metadata:languages:" + username);
                log.debug("📦 Caché 'languages' invalidado para: {}", username);
            }
            
            // Caché de frameworks
            var frameworksCache = cacheManager.getCache("frameworks");
            if (frameworksCache != null) {
                frameworksCache.evict("metadata:frameworks:" + username);
                log.debug("📦 Caché 'frameworks' invalidado para: {}", username);
            }
            
            // Caché de CV
            var cvCache = cacheManager.getCache("technicalCV");
            if (cvCache != null) {
                cvCache.evict(username);
                log.debug("📦 Caché 'technicalCV' invalidado para: {}", username);
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Error invalidando caché para {}: {}", username, e.getMessage());
        }
    }
}

