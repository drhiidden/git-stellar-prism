package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.service.GithubService;
import com.drhdn.ghvis.service.OAuth2UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    private final OAuth2UserService oAuth2UserService;
    private final GithubService githubService;

    /**
     * Obtiene la información básica del usuario autenticado.
     */
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getUserInfo(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
            return Mono.just(Map.of("error", "Usuario no autenticado"));
        }
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> userInfo = new HashMap<>();
        
        // Información básica del usuario
        userInfo.put("id", oauth2User.getAttribute("id"));
        userInfo.put("login", oauth2User.getAttribute("login"));
        userInfo.put("name", oauth2User.getAttribute("name"));
        userInfo.put("email", oauth2User.getAttribute("email"));
        userInfo.put("avatar_url", oauth2User.getAttribute("avatar_url"));
        userInfo.put("bio", oauth2User.getAttribute("bio"));
        userInfo.put("location", oauth2User.getAttribute("location"));
        userInfo.put("public_repos", oauth2User.getAttribute("public_repos"));
        userInfo.put("total_private_repos", oauth2User.getAttribute("total_private_repos"));
        userInfo.put("followers", oauth2User.getAttribute("followers"));
        userInfo.put("following", oauth2User.getAttribute("following"));
        
        return Mono.just(userInfo)
            .doOnNext(info -> log.debug("Información de usuario obtenida: {}", 
                (String) oauth2User.getAttribute("login")));
    }

    /**
     * Verifica el estado de autenticación del usuario.
     */
    @GetMapping(value = "/auth-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getAuthStatus(Principal principal, Authentication authentication) {
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
        
        return Mono.just(status);
    }

    /**
     * Obtiene los repositorios del usuario autenticado.
     */
    @GetMapping(value = "/repositories", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Object> getUserRepositories(Principal principal) {
        if (principal == null) {
            return Mono.just(Map.of("error", "Usuario no autenticado"));
        }
        
        return githubService.getUserRepositories(principal)
            .collectList()
            .cast(Object.class)
            .doOnNext(repos -> log.debug("Obtenidos repositorios para el usuario"))
            .doOnError(error -> log.error("Error obteniendo repositorios del usuario: {}", error.getMessage()))
            .onErrorReturn(Map.of("error", "Error obteniendo repositorios"));
    }
} 