package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.service.OAuth2UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para información del usuario autenticado.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    private final OAuth2UserService oAuth2UserService;

    /**
     * Obtiene la información del usuario autenticado.
     */
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<OAuth2UserService.UserInfo> getUserInfo(Authentication authentication) {
        return oAuth2UserService.getUserInfo(authentication)
            .doOnNext(userInfo -> log.debug("Información de usuario obtenida: {}", userInfo.getLogin()));
    }

    /**
     * Verifica el estado de autenticación del usuario.
     */
    @GetMapping(value = "/auth-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getAuthStatus(Principal principal) {
        Map<String, Object> status = new HashMap<>();
        
        if (principal != null) {
            status.put("authenticated", true);
            status.put("username", principal.getName());
            status.put("oauth2", oAuth2UserService.isOAuth2Authenticated(principal));
        } else {
            status.put("authenticated", false);
            status.put("oauth2", false);
        }
        
        return Mono.just(status);
    }

    /**
     * Obtiene el token de acceso del usuario (para debugging - no exponer en producción).
     */
    @GetMapping(value = "/token-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getTokenInfo(Principal principal) {
        Map<String, Object> tokenInfo = new HashMap<>();
        
        return oAuth2UserService.getUserAccessToken(principal)
            .map(token -> {
                tokenInfo.put("hasToken", true);
                tokenInfo.put("tokenLength", token.length());
                tokenInfo.put("tokenPrefix", token.substring(0, Math.min(10, token.length())) + "...");
                return tokenInfo;
            })
            .switchIfEmpty(Mono.fromCallable(() -> {
                tokenInfo.put("hasToken", false);
                return tokenInfo;
            }));
    }
} 