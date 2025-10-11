package com.drhdn.ghvis.application.command;

/**
 * Interfaz marcadora para todos los comandos del sistema.
 * 
 * Los comandos representan intenciones de CAMBIAR el estado del sistema:
 * - Crear algo nuevo
 * - Modificar algo existente
 * - Eliminar algo
 * - Invalidar cachés
 * 
 * Características de los Commands:
 * - Son inmutables (records en Java)
 * - Contienen solo los datos necesarios para la operación
 * - NO retornan datos de negocio (solo confirmación/error)
 * - Pueden ser auditados y logueados
 * 
 * Patrón CQRS (Command Query Responsibility Segregation)
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface Command {
    
    /**
     * Obtiene el nombre del comando para logging y auditoría.
     * Por defecto usa el nombre de la clase.
     */
    default String getCommandName() {
        return this.getClass().getSimpleName();
    }
}

