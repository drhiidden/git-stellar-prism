package com.drhdn.ghvis.infrastructure.adapter.inbound.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Principal;


/**
 * Servicio para manejar usuarios OAuth2 y sus tokens de acceso.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2UserService {

    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    /**
     * Obtiene el token de acceso del usuario autenticado.
     * 
     * @param principal Principal del usuario autenticado
     * @return Mono con el token de acceso
     */
    public Mono<String> getUserAccessToken(Principal principal) {
        if (!(principal instanceof OAuth2AuthenticationToken)) {
            return Mono.empty();
        }
        
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) principal;
        String registrationId = authToken.getAuthorizedClientRegistrationId();
        String principalName = authToken.getName();
        
        return authorizedClientService
            .loadAuthorizedClient(registrationId, principalName)
            .map(OAuth2AuthorizedClient::getAccessToken)
            .map(accessToken -> accessToken.getTokenValue())
            .doOnNext(token -> log.debug("Token obtenido para usuario: {}", principalName))
            .doOnError(error -> log.error("Error al obtener token para usuario {}: {}", 
                principalName, error.getMessage()));
    }

    /**
     * Obtiene la información del usuario autenticado.
     * 
     * @param authentication Autenticación del usuario
     * @return Mono con la información del usuario
     */
    public Mono<UserInfo> getUserInfo(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return Mono.empty();
        }
        
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = authToken.getPrincipal();
        
        UserInfo userInfo = UserInfo.builder()
            .id(oauth2User.getAttribute("id"))
            .login(oauth2User.getAttribute("login"))
            .name(oauth2User.getAttribute("name"))
            .email(oauth2User.getAttribute("email"))
            .avatarUrl(oauth2User.getAttribute("avatar_url"))
            .htmlUrl(oauth2User.getAttribute("html_url"))
            .publicRepos(oauth2User.getAttribute("public_repos"))
            .followers(oauth2User.getAttribute("followers"))
            .following(oauth2User.getAttribute("following"))
            .build();
        
        return Mono.just(userInfo);
    }

    /**
     * Verifica si el usuario está autenticado vía OAuth2.
     * 
     * @param principal Principal del usuario
     * @return true si está autenticado vía OAuth2
     */
    public boolean isOAuth2Authenticated(Principal principal) {
        return principal instanceof OAuth2AuthenticationToken;
    }

    /**
     * Información del usuario de GitHub.
     */
    @lombok.Builder
    @lombok.Data
    public static class UserInfo {
        private Integer id;
        private String login;
        private String name;
        private String email;
        private String avatarUrl;
        private String htmlUrl;
        private Integer publicRepos;
        private Integer followers;
        private Integer following;
    }
} 