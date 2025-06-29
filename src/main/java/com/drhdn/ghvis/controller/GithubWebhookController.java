package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.model.Event;
import com.drhdn.ghvis.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint receptor de Webhooks de GitHub con validación de seguridad.
 */
@RestController
@ConditionalOnProperty(name = "app.realtime.enabled", havingValue = "true")
@RequestMapping("/webhook")
@Slf4j
@RequiredArgsConstructor
public class GithubWebhookController {

    private final EventPublisherService eventPublisherService;
    
    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payloadString,
            @RequestBody Map<String, Object> payload) {
        
        try {
            log.info("Webhook recibido - Tipo: {}, Tamaño: {} bytes", eventType, payloadString.length());
            
            // Validar firma si está configurada
            if (!webhookSecret.isEmpty() && !isValidSignature(payloadString, signature)) {
                log.warn("Webhook rechazado: firma inválida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Firma inválida");
            }
            
            // Filtrar solo eventos importantes para reducir ruido
            if (!isRelevantEvent(eventType)) {
                log.debug("Evento {} ignorado (no relevante para visualización)", eventType);
                return ResponseEntity.ok("Evento ignorado");
            }
            
            // Crear evento interno
            Event event = Event.builder()
                    .id(UUID.randomUUID().toString())
                    .type(eventType)
                    .timestamp(Instant.now())
                    .repositoryFullName(extractRepositoryName(payload))
                    .payload(payload)
                    .build();

            // Publicar evento para SSE
            eventPublisherService.publish(event);
            
            log.debug("Evento {} procesado exitosamente para repo: {}", 
                eventType, event.getRepositoryFullName());
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Webhook procesado");
            
        } catch (Exception e) {
            log.error("Error procesando webhook de tipo {}: {}", eventType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno del servidor");
        }
    }
    
    /**
     * Valida la firma del webhook usando HMAC-SHA256.
     */
    private boolean isValidSignature(String payload, String signature) {
        if (signature == null || webhookSecret.isEmpty()) {
            // Si no hay secret configurado, aceptar (modo desarrollo)
            return webhookSecret.isEmpty();
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + bytesToHex(digest);
            
            return expectedSignature.equals(signature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validando firma del webhook: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Convierte bytes a hexadecimal.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Determina si un evento es relevante para la visualización.
     */
    private boolean isRelevantEvent(String eventType) {
        return switch (eventType) {
            case "push", "pull_request", "issues", "create", "delete", 
                 "release", "fork", "star", "watch" -> true;
            default -> false;
        };
    }
    
    /**
     * Extrae el nombre completo del repositorio del payload.
     */
    private String extractRepositoryName(Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> repository = (Map<String, Object>) payload.get("repository");
            if (repository != null) {
                return (String) repository.get("full_name");
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer nombre del repositorio del payload");
        }
        return "unknown/unknown";
    }
} 