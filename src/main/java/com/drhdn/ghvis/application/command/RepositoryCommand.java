package com.drhdn.ghvis.application.command;

import java.security.Principal;
import java.util.Map;

/**
 * Comandos relacionados con operaciones de escritura en repositorios.
 * 
 * Estos comandos modifican el estado del sistema:
 * - Refrescar datos desde GitHub
 * - Actualizar repositorio específico
 * - Invalidar cachés
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public sealed interface RepositoryCommand extends Command 
    permits RepositoryCommand.RefreshRepositories,
            RepositoryCommand.InvalidateCache,
            RepositoryCommand.RefreshSingleRepository {
    
    /**
     * Comando para refrescar todos los repositorios de un usuario desde GitHub API.
     * 
     * Este comando:
     * - Hace request a GitHub API
     * - Actualiza datos en caché
     * - Publica evento de repositorios actualizados
     * 
     * @param username Nombre de usuario de GitHub
     * @param principal Principal de autenticación
     * @param forceRefresh Si true, ignora caché y fuerza refresh desde API
     */
    record RefreshRepositories(
        String username,
        Principal principal,
        boolean forceRefresh
    ) implements RepositoryCommand {
        
        public RefreshRepositories(String username, Principal principal) {
            this(username, principal, false);
        }
        
        @Override
        public String getCommandName() {
            return forceRefresh ? "RefreshRepositories[FORCE]" : "RefreshRepositories";
        }
    }
    
    /**
     * Comando para invalidar cachés de repositorios de un usuario.
     * 
     * Este comando:
     * - Limpia caché L1 (in-memory)
     * - Limpia caché L2 (Redis) si existe
     * - NO hace requests a GitHub
     * 
     * @param username Nombre de usuario
     * @param reason Razón de la invalidación (para auditoría)
     */
    record InvalidateCache(
        String username,
        String reason
    ) implements RepositoryCommand {
        
        public InvalidateCache(String username) {
            this(username, "Manual invalidation");
        }
    }
    
    /**
     * Comando para refrescar un repositorio específico.
     * 
     * Útil cuando:
     * - Usuario actualiza descripción de un repo
     * - Se detecta que un repo está desactualizado
     * - Testing de un repo específico
     * 
     * @param owner Propietario del repositorio
     * @param repoName Nombre del repositorio
     * @param principal Principal de autenticación
     * @param changes Mapa de cambios específicos a aplicar (opcional)
     */
    record RefreshSingleRepository(
        String owner,
        String repoName,
        Principal principal,
        Map<String, Object> changes
    ) implements RepositoryCommand {
        
        public RefreshSingleRepository(String owner, String repoName, Principal principal) {
            this(owner, repoName, principal, Map.of());
        }
        
        public String getFullName() {
            return owner + "/" + repoName;
        }
    }
}

