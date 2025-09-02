package com.drhdn.ghvis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;


import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Configuración para manejo centralizado de errores OAuth2 y llamadas a APIs.
 * 
 * Proporciona filtros y manejadores de error profesionales para:
 * - Errores de autenticación OAuth2
 * - Rate limiting de GitHub API
 * - Errores de red y timeouts
 * - Logging estructurado de errores
 */
@Configuration
@Slf4j
public class OAuth2ErrorConfig {

    /**
     * Filtro de manejo de errores para llamadas a GitHub API.
     * Maneja rate limiting, errores de autenticación y otros errores comunes.
     */
    @Bean
    public ExchangeFilterFunction githubErrorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(this::handleGitHubApiResponse);
    }

    /**
     * Filtro de logging para requests OAuth2.
     * Registra información útil para debugging sin exponer datos sensibles.
     */
    @Bean
    public ExchangeFilterFunction oauth2LoggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("OAuth2 Request: {} {}", 
                clientRequest.method(), 
                clientRequest.url().getPath());
            return Mono.just(clientRequest);
        });
    }

    /**
     * Maneja respuestas de la API de GitHub con diferentes códigos de error.
     */
    private Mono<ClientResponse> handleGitHubApiResponse(ClientResponse response) {
        if (response.statusCode().isError()) {
            return handleErrorResponse(response);
        }
        return Mono.just(response);
    }

    /**
     * Procesa respuestas de error de manera centralizada.
     */
    private Mono<ClientResponse> handleErrorResponse(ClientResponse response) {
        HttpStatus status = (HttpStatus) response.statusCode();
        
        return response.bodyToMono(String.class)
            .defaultIfEmpty("Error sin cuerpo de respuesta")
            .flatMap(errorBody -> {
                switch (status) {
                    case UNAUTHORIZED:
                        log.error("Error de autenticación OAuth2: {}", errorBody);
                        return createOAuth2Exception("unauthorized", "Token OAuth2 inválido o expirado");
                    
                    case FORBIDDEN:
                        if (errorBody.contains("rate limit")) {
                            log.warn("Rate limit alcanzado en GitHub API");
                            return createOAuth2Exception("rate_limit", "Rate limit de GitHub API alcanzado");
                        } else if (errorBody.contains("access")) {
                            log.error("Acceso denegado al recurso: {}", errorBody);
                            return createOAuth2Exception("access_denied", "Acceso denegado al recurso solicitado");
                        }
                        break;
                    
                    case NOT_FOUND:
                        log.warn("Recurso no encontrado: {}", response.request().getURI());
                        return createOAuth2Exception("not_found", "Recurso no encontrado");
                    
                    case UNPROCESSABLE_ENTITY:
                        log.error("Error de validación en GitHub API: {}", errorBody);
                        return createOAuth2Exception("validation_error", "Error de validación en la solicitud");
                    
                    case TOO_MANY_REQUESTS:
                        log.warn("Demasiadas solicitudes a GitHub API");
                        return createOAuth2Exception("too_many_requests", "Demasiadas solicitudes");
                    
                    default:
                        log.error("Error inesperado en GitHub API [{}]: {}", status, errorBody);
                        return createOAuth2Exception("api_error", "Error inesperado en la API");
                }
                
                // Para errores no específicos, crear excepción genérica
                log.error("Error HTTP {} en GitHub API: {}", status, errorBody);
                return createOAuth2Exception("http_error", "Error HTTP: " + status);
            });
    }

    /**
     * Crea una excepción OAuth2 personalizada.
     */
    private Mono<ClientResponse> createOAuth2Exception(String errorCode, String description) {
        // OAuth2Error error = new OAuth2Error(errorCode, description, null); // Reserved for future use
        // OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error); // Reserved for future use
        
        return Mono.error(new WebClientResponseException(
            description, 
            HttpStatus.UNAUTHORIZED.value(), 
            HttpStatus.UNAUTHORIZED.getReasonPhrase(), 
            null, 
            null, 
            null
        ));
    }

    /**
     * Maneja timeout específicamente para llamadas OAuth2.
     */
    @Bean
    public ExchangeFilterFunction oauth2TimeoutFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            // Log para monitoring de performance
            String requestStartHeader = response.request().getHeaders().getFirst("X-Request-Start");
            long currentTime = System.currentTimeMillis();
            long duration = 0;
            
            if (requestStartHeader != null) {
                try {
                    long requestStart = Long.parseLong(requestStartHeader);
                    duration = currentTime - requestStart;
                } catch (NumberFormatException e) {
                    log.debug("No se pudo parsear X-Request-Start header: {}", requestStartHeader);
                }
            }
            
            if (duration > 10000) { // 10 segundos
                log.warn("Llamada OAuth2 lenta detectada: {}ms para {}", 
                    duration, response.request().getURI());
            }
            
            return Mono.just(response);
        });
    }
    
    /**
     * Filtro para agregar timestamp de inicio de request.
     */
    @Bean
    public ExchangeFilterFunction requestTimingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            return Mono.just(
                ClientRequest.from(clientRequest)
                    .header("X-Request-Start", String.valueOf(System.currentTimeMillis()))
                    .build()
            );
        });
    }
} 