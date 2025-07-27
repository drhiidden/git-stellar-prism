package com.drhdn.ghvis.infrastructure.adapter.outbound.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static org.springframework.http.HttpStatus.*;

/**
 * Manejador de errores reactivo para operaciones de GitHub API.
 * 
 * Proporciona manejo consistente de errores HTTP y excepciones específicas
 * del dominio para mejorar la experiencia del usuario y facilitar el debugging.
 * 
 * Este componente pertenece a la capa de infraestructura ya que maneja
 * errores específicos de frameworks externos (Spring WebFlux, HTTP).
 */
@Component
@Slf4j
public class ReactiveErrorHandler {

    /**
     * Maneja errores específicos de GitHub API y los convierte en excepciones del dominio.
     * 
     * @param <T> Tipo de retorno
     * @return Function que maneja errores y devuelve Mono con el tipo especificado
     */
    public <T> Function<Throwable, Mono<T>> handleGithubError() {
        return throwable -> {
            if (throwable instanceof WebClientResponseException) {
                WebClientResponseException ex = (WebClientResponseException) throwable;
                
                log.warn("Error de GitHub API - Status: {}, Message: {}", 
                    ex.getStatusCode(), ex.getMessage());
                
                switch (ex.getStatusCode()) {
                    case NOT_FOUND:
                        return Mono.error(new RepositoryNotFoundException(
                            "Repositorio no encontrado o no accesible"));
                        
                    case FORBIDDEN:
                        return Mono.error(new RateLimitExceededException(
                            "Rate limit excedido. Intenta más tarde."));
                        
                    case UNAUTHORIZED:
                        return Mono.error(new TokenExpiredException(
                            "Token de autenticación expirado. Por favor, vuelve a autenticarte."));
                        
                    case TOO_MANY_REQUESTS:
                        return handleRateLimit(ex);
                        
                    case INTERNAL_SERVER_ERROR:
                    case BAD_GATEWAY:
                    case SERVICE_UNAVAILABLE:
                        return Mono.error(new GitHubApiException(
                            "Error del servidor GitHub. Intenta más tarde."));
                        
                    default:
                        return Mono.error(new GitHubApiException(
                            "Error de GitHub API: " + ex.getStatusCode() + " - " + ex.getMessage()));
                }
            }
            
            // Errores de red o inesperados
            log.error("Error inesperado en operación de GitHub API", throwable);
            return Mono.error(new UnexpectedException("Error inesperado en la operación", throwable));
        };
    }

    /**
     * Maneja específicamente errores de rate limiting con información de retry.
     * 
     * @param ex Excepción de WebClientResponseException
     * @param <T> Tipo de retorno
     * @return Mono con error específico de rate limiting
     */
    private <T> Mono<T> handleRateLimit(WebClientResponseException ex) {
        String retryAfter = ex.getHeaders().getFirst("Retry-After");
        long waitSeconds = retryAfter != null ? Long.parseLong(retryAfter) : 60;
        
        log.warn("Rate limit excedido. Reintentar en {} segundos", waitSeconds);
        
        return Mono.error(new RateLimitExceededException(
            String.format("Rate limit excedido. Reintenta en %d segundos.", waitSeconds)));
    }

    /**
     * Maneja errores de validación de parámetros.
     * 
     * @param <T> Tipo de retorno
     * @return Function que maneja errores de validación
     */
    public <T> Function<Throwable, Mono<T>> handleValidationError() {
        return throwable -> {
            if (throwable instanceof IllegalArgumentException) {
                log.warn("Error de validación: {}", throwable.getMessage());
                return Mono.error(new ValidationException(throwable.getMessage()));
            }
            return Mono.error(throwable);
        };
    }

    /**
     * Maneja errores de autenticación OAuth2.
     * 
     * @param <T> Tipo de retorno
     * @return Function que maneja errores de autenticación
     */
    public <T> Function<Throwable, Mono<T>> handleOAuth2Error() {
        return throwable -> {
            log.warn("Error de autenticación OAuth2: {}", throwable.getMessage());
            
            if (throwable.getMessage().contains("access_denied")) {
                return Mono.error(new OAuth2AccessDeniedException("Acceso denegado por el usuario"));
            }
            
            if (throwable.getMessage().contains("invalid_scope")) {
                return Mono.error(new OAuth2ScopeException("Scopes de permisos inválidos"));
            }
            
            return Mono.error(new OAuth2Exception("Error de autenticación OAuth2: " + throwable.getMessage()));
        };
    }

    /**
     * Maneja errores de caché y operaciones de base de datos.
     * 
     * @param <T> Tipo de retorno
     * @return Function que maneja errores de caché
     */
    public <T> Function<Throwable, Mono<T>> handleCacheError() {
        return throwable -> {
            log.warn("Error de caché: {}", throwable.getMessage());
            
            // Para errores de caché, continuar sin caché en lugar de fallar
            if (throwable instanceof CacheException) {
                log.info("Continuando sin caché debido a error: {}", throwable.getMessage());
                return Mono.empty();
            }
            
            return Mono.error(new CacheException("Error en operación de caché", throwable));
        };
    }

    /**
     * Combina múltiples manejadores de errores en uno solo.
     * 
     * @param handlers Manejadores de errores a combinar
     * @param <T> Tipo de retorno
     * @return Function que combina todos los manejadores
     */
    @SafeVarargs
    public static <T> Function<Throwable, Mono<T>> combineHandlers(
            Function<Throwable, Mono<T>>... handlers) {
        return throwable -> {
            for (Function<Throwable, Mono<T>> handler : handlers) {
                try {
                    Mono<T> result = handler.apply(throwable);
                    if (result != null) {
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("Error en manejador de errores", e);
                }
            }
            return Mono.error(throwable);
        };
    }
}

// Excepciones específicas de infraestructura

class RepositoryNotFoundException extends RuntimeException {
    public RepositoryNotFoundException(String message) {
        super(message);
    }
}

class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}

class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}

class GitHubApiException extends RuntimeException {
    public GitHubApiException(String message) {
        super(message);
    }
}

class UnexpectedException extends RuntimeException {
    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

class OAuth2Exception extends RuntimeException {
    public OAuth2Exception(String message) {
        super(message);
    }
}

class OAuth2AccessDeniedException extends OAuth2Exception {
    public OAuth2AccessDeniedException(String message) {
        super(message);
    }
}

class OAuth2ScopeException extends OAuth2Exception {
    public OAuth2ScopeException(String message) {
        super(message);
    }
}

class CacheException extends RuntimeException {
    public CacheException(String message) {
        super(message);
    }
    
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
} 