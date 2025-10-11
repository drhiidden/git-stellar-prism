package com.drhdn.ghvis.application.command;

import reactor.core.publisher.Mono;

/**
 * Interfaz genérica para manejadores de comandos.
 * 
 * Un CommandHandler:
 * - Recibe un comando
 * - Valida los datos
 * - Ejecuta la lógica de negocio
 * - Retorna resultado/confirmación
 * - Publica eventos si es necesario
 * 
 * @param <C> Tipo del comando
 * @param <R> Tipo del resultado
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
public interface CommandHandler<C extends Command, R> {
    
    /**
     * Maneja la ejecución del comando.
     * 
     * @param command Comando a ejecutar
     * @return Resultado de la ejecución
     */
    Mono<R> handle(C command);
    
    /**
     * Valida el comando antes de ejecutarlo.
     * Por defecto no hace validación.
     * 
     * @param command Comando a validar
     * @return Mono vacío si es válido, error si no lo es
     */
    default Mono<Void> validate(C command) {
        return Mono.empty();
    }
}

