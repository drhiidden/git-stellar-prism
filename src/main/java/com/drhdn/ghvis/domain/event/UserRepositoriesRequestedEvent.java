package com.drhdn.ghvis.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Evento que se dispara cuando se solicita obtener repositorios de un usuario.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRepositoriesRequestedEvent {
    
    /**
     * ID único de la solicitud
     */
    private String requestId;
    
    /**
     * Nombre de usuario del cual se solicitan los repositorios
     */
    private String username;
    
    /**
     * Usuario que realiza la solicitud
     */
    private String requestingUser;
    
    /**
     * Timestamp de la solicitud
     */
    private Instant timestamp;
    
    /**
     * Si se incluyen detalles completos
     */
    private boolean includeDetails;
    
    /**
     * Tipo de solicitud (all, public, private, detailed)
     */
    private String requestType;
    
    /**
     * Obtiene el mensaje de log para este evento
     */
    public String getLogMessage() {
        return String.format("Solicitud de repositorios para usuario '%s' (tipo: %s, detalles: %s)", 
            username, requestType, includeDetails);
    }
    
    /**
     * Obtiene la clave de cache para esta solicitud
     */
    public String getCacheKey() {
        return String.format("user:%s:repositories:%s", username, requestType);
    }
} 