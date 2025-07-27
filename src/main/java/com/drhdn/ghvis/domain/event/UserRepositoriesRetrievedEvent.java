package com.drhdn.ghvis.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento que se dispara cuando se obtienen exitosamente los repositorios de un usuario.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRepositoriesRetrievedEvent {
    
    /**
     * ID único de la solicitud original
     */
    private String requestId;
    
    /**
     * Nombre de usuario del cual se obtuvieron los repositorios
     */
    private String username;
    
    /**
     * Usuario que realizó la solicitud
     */
    private String requestingUser;
    
    /**
     * Duración de la operación en milisegundos
     */
    private long durationMs;
    
    /**
     * Tipo de solicitud (all, public, private, detailed)
     */
    private String requestType;
    
    /**
     * Si se incluyeron detalles completos
     */
    private boolean includeDetails;
    
    /**
     * Estado de la operación
     */
    private String status;
    
    /**
     * Obtiene el mensaje de log para este evento
     */
    public String getLogMessage() {
        return String.format("Repositorios obtenidos exitosamente para usuario '%s' en %dms (tipo: %s)", 
            username, durationMs, requestType);
    }
    
    /**
     * Verifica si la operación fue exitosa
     */
    public boolean isSuccessful() {
        return "success".equals(status);
    }
    
    /**
     * Obtiene la duración formateada
     */
    public String getFormattedDuration() {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else {
            return String.format("%.2fs", durationMs / 1000.0);
        }
    }
} 