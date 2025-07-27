package com.drhdn.ghvis.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento que se dispara cuando falla la obtención de repositorios de un usuario.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRepositoriesRetrievalFailedEvent {
    
    /**
     * ID único de la solicitud original
     */
    private String requestId;
    
    /**
     * Nombre de usuario del cual se intentaron obtener los repositorios
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
     * Mensaje de error
     */
    private String errorMessage;
    
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
        return String.format("Error obteniendo repositorios para usuario '%s' en %dms: %s", 
            username, durationMs, errorMessage);
    }
    
    /**
     * Verifica si la operación falló
     */
    public boolean isFailed() {
        return "error".equals(status) || "failed".equals(status);
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
    
    /**
     * Obtiene el tipo de error basado en el mensaje
     */
    public String getErrorType() {
        if (errorMessage == null) return "UNKNOWN";
        
        String message = errorMessage.toLowerCase();
        if (message.contains("not found") || message.contains("404")) {
            return "USER_NOT_FOUND";
        } else if (message.contains("rate limit") || message.contains("403")) {
            return "RATE_LIMIT_EXCEEDED";
        } else if (message.contains("unauthorized") || message.contains("401")) {
            return "UNAUTHORIZED";
        } else if (message.contains("timeout")) {
            return "TIMEOUT";
        } else if (message.contains("network") || message.contains("connection")) {
            return "NETWORK_ERROR";
        } else {
            return "GENERAL_ERROR";
        }
    }
} 