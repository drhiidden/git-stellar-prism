package com.drhdn.ghvis.application.command;

import java.security.Principal;

/**
 * Comandos relacionados con generación y exportación de CVs.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public sealed interface CVCommand extends Command 
    permits CVCommand.GenerateCV,
            CVCommand.RegenerateCVForAllUsers {
    
    /**
     * Comando para generar un CV técnico para un usuario.
     * 
     * Este comando:
     * - Obtiene repositorios del usuario
     * - Analiza metadata técnica
     * - Genera CV estructurado
     * - Cachea el resultado
     * 
     * @param username Nombre de usuario
     * @param principal Principal de autenticación
     * @param useCache Si true, usa CV cacheado si existe y es válido
     */
    record GenerateCV(
        String username,
        Principal principal,
        boolean useCache
    ) implements CVCommand {
        
        public GenerateCV(String username, Principal principal) {
            this(username, principal, true);
        }
    }
    
    /**
     * Comando administrativo para regenerar CVs de todos los usuarios activos.
     * 
     * Útil para:
     * - Actualización masiva de formato de CV
     * - Migración de versiones
     * - Mantenimiento programado
     * 
     * @param reason Razón de la regeneración
     * @param batchSize Número de usuarios a procesar por lote
     */
    record RegenerateCVForAllUsers(
        String reason,
        int batchSize
    ) implements CVCommand {
        
        public RegenerateCVForAllUsers(String reason) {
            this(reason, 10);
        }
    }
}

