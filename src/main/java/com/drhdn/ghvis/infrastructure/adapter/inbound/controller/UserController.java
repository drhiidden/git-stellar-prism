package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import com.drhdn.ghvis.application.handler.GetUserRepositoriesQueryHandler;
import com.drhdn.ghvis.application.query.GetUserInfoQuery;
import com.drhdn.ghvis.application.handler.GetUserInfoQueryHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para información del usuario autenticado.
 * Implementa CQRS usando queries y handlers.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    private final GetUserInfoQueryHandler getUserInfoQueryHandler;
    private final GetUserRepositoriesQueryHandler getUserRepositoriesQueryHandler;

    /**
     * Obtiene la información básica del usuario autenticado.
     */
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getUserInfo(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
            return Mono.just(ResponseEntity.ok(Map.of("error", "Usuario no autenticado")));
        }
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String username = oauth2User.getAttribute("login");
        
        log.info("🔍 Obteniendo información del usuario: {}", username);
        
        GetUserInfoQuery query = GetUserInfoQuery.create(username, authentication);
        
        return getUserInfoQueryHandler.handle(query)
            .map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("login", user.getLogin());
                userInfo.put("name", user.getName());
                userInfo.put("email", user.getEmail());
                userInfo.put("avatar_url", user.getAvatarUrl());
                userInfo.put("bio", user.getBio());
                userInfo.put("location", user.getLocation());
                userInfo.put("public_repos", user.getPublicRepos());
                userInfo.put("total_private_repos", user.getTotalPrivateRepos());
                userInfo.put("followers", user.getFollowers());
                userInfo.put("following", user.getFollowing());
                userInfo.put("html_url", user.getHtmlUrl());
                userInfo.put("type", user.getType());
                userInfo.put("verified", user.getVerified());
                userInfo.put("bot", user.getBot());
                userInfo.put("site_admin", user.getSiteAdmin());
                
                return ResponseEntity.ok(userInfo);
            })
            .doOnSuccess(response -> log.info("✅ Información del usuario obtenida exitosamente: {}", username))
            .doOnError(error -> log.error("❌ Error obteniendo información del usuario {}: {}", username, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError()
                .body(Map.of("error", "Error obteniendo información del usuario", "message", error.getMessage()))));
    }

    /**
     * Verifica el estado de autenticación del usuario.
     */
    @GetMapping(value = "/auth-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getAuthStatus(Principal principal, Authentication authentication) {
        Map<String, Object> status = new HashMap<>();
        
        if (principal != null && authentication != null) {
            status.put("authenticated", true);
            status.put("username", principal.getName());
            status.put("oauth2", authentication.getPrincipal() instanceof OAuth2User);
            
            if (authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                status.put("login", oauth2User.getAttribute("login"));
                status.put("avatar_url", oauth2User.getAttribute("avatar_url"));
            }
        } else {
            status.put("authenticated", false);
            status.put("oauth2", false);
        }
        
        return Mono.just(ResponseEntity.ok(status))
            .doOnSuccess(response -> log.debug("🔍 Estado de autenticación verificado"))
            .doOnError(error -> log.error("❌ Error verificando estado de autenticación: {}", error.getMessage()));
    }

    /**
     * Obtiene los repositorios del usuario autenticado (con caché).
     */
    @GetMapping(value = "/repositories", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getUserRepositories(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.ok(Map.of("error", "Usuario no autenticado")));
        }
        
        String username = principal.getName();
        log.info("🔍 Obteniendo repositorios del usuario: {}", username);
        
        return getUserRepositoriesQueryHandler.handleAllQuery(username, principal)
            .collectList()
            .map(repositories -> ResponseEntity.ok((Object) repositories))
            .doOnSuccess(response -> log.info("✅ Repositorios obtenidos exitosamente para usuario: {}", username))
            .doOnError(error -> log.error("❌ Error obteniendo repositorios del usuario {}: {}", username, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError()
                .body(Map.of("error", "Error obteniendo repositorios", "message", error.getMessage()))));
    }

    /**
     * Obtiene los repositorios del usuario con información detallada de tecnologías (con caché).
     */
    @GetMapping(value = "/repositories/detailed", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getUserRepositoriesDetailed(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.ok(Map.of("error", "Usuario no autenticado")));
        }
        
        String username = principal.getName();
        log.info("🔍 Obteniendo repositorios detallados del usuario: {}", username);
        
        return getUserRepositoriesQueryHandler.handleDetailedQuery(username, principal)
            .collectList()
            .map(repositories -> ResponseEntity.ok((Object) repositories))
            .doOnSuccess(response -> log.info("✅ Repositorios detallados obtenidos exitosamente para usuario: {}", username))
            .doOnError(error -> log.error("❌ Error obteniendo repositorios detallados del usuario {}: {}", username, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError()
                .body(Map.of("error", "Error obteniendo repositorios detallados", "message", error.getMessage()))));
    }

    /**
     * Obtiene repositorios sin caché - para forzar actualización.
     */
    @GetMapping(value = "/repositories/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> refreshUserRepositories(Principal principal) {
        if (principal == null) {
            return Mono.just(ResponseEntity.ok(Map.of("error", "Usuario no autenticado")));
        }
        
        String username = principal.getName();
        log.info("🔄 Refrescando repositorios del usuario: {}", username);
        
        // Limpiar cache del usuario primero
        getUserRepositoriesQueryHandler.clearCache(username);
        
        return getUserRepositoriesQueryHandler.handleDetailedQuery(username, principal)
            .collectList()
            .map(repositories -> ResponseEntity.ok((Object) repositories))
            .doOnSuccess(response -> log.info("✅ Repositorios refrescados exitosamente para usuario: {}", username))
            .doOnError(error -> log.error("❌ Error refrescando repositorios del usuario {}: {}", username, error.getMessage()))
            .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError()
                .body(Map.of("error", "Error refrescando repositorios", "message", error.getMessage()))));
    }
} 