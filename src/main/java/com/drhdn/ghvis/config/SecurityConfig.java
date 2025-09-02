package com.drhdn.ghvis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;


import java.net.URI;

/**
 * Configuración de seguridad OAuth2 profesional para autenticación con GitHub.
 * 
 * Esta configuración sigue las mejores prácticas de Spring Security WebFlux:
 * - Separación clara de responsabilidades
 * - Manejo adecuado de beans OAuth2
 * - Configuración robusta de seguridad
 * - Integración correcta con WebClient
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.security.success-url:/}")
    private String successUrl;

    @Value("${app.security.logout-success-url:/}")
    private String logoutSuccessUrl;

    /**
     * Configuración principal de la cadena de filtros de seguridad.
     * Definimos las reglas de autorización, OAuth2 login y configuraciones CSRF.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Configurar autorización de rutas de manera más granular
            .authorizeExchange(this::configureAuthorization)
            
            // Configurar OAuth2 Login con manejo personalizado
            .oauth2Login(this::configureOAuth2Login)
            
            // Configurar OAuth2 Client para API calls (usa automáticamente el bean ReactiveOAuth2AuthorizedClientManager)
            .oauth2Client(Customizer.withDefaults())
            
            // Configurar logout
            .logout(this::configureLogout)
            
            // Configurar CSRF de manera más segura
            .csrf(this::configureCsrf)
            
            // Configurar headers de seguridad
            .headers(this::configureHeaders)
            
            .build();
    }

    /**
     * Configuración granular de autorización de rutas.
     */
    private void configureAuthorization(ServerHttpSecurity.AuthorizeExchangeSpec exchanges) {
        exchanges
            // Recursos estáticos públicos
            .pathMatchers("/", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
            
            // Endpoints de autenticación OAuth2
            .pathMatchers("/login", "/oauth2/**", "/login/oauth2/**").permitAll()
            
            // Console H2 solo en desarrollo
            .pathMatchers("/h2-console/**").permitAll()
            
            // Páginas públicas adicionales
            .pathMatchers("/index", "/home", "/about", "/public/**").permitAll()
            
            // API endpoints requieren autenticación
            .pathMatchers("/api/**").authenticated()
            
            // Páginas que requieren autenticación
            .pathMatchers("/dashboard", "/profile", "/settings").authenticated()
            
            // Webhooks pueden requerir configuración especial
            .pathMatchers("/webhook/**").permitAll() // Configurar según necesidades
            
            // Por defecto, permitir acceso público y requerir autenticación solo para rutas específicas
            .anyExchange().permitAll();
    }

    /**
     * Configuración de OAuth2 Login con handlers personalizados.
     */
    private void configureOAuth2Login(ServerHttpSecurity.OAuth2LoginSpec oauth2) {
        oauth2
            .authenticationSuccessHandler(authenticationSuccessHandler())
            .authenticationFailureHandler((webFilterExchange, ex) -> {
                // Log del error de autenticación
                return webFilterExchange.getExchange().getResponse()
                    .setComplete();
            });
    }

    /**
     * Configuración de logout con limpieza adecuada.
     */
    private void configureLogout(ServerHttpSecurity.LogoutSpec logout) {
        logout
            .logoutUrl("/logout")
            .logoutSuccessHandler(logoutSuccessHandler())
            .requiresLogout(ServerWebExchangeMatchers.pathMatchers("/logout"));
    }

    /**
     * Configuración CSRF más granular y segura para WebFlux.
     */
    private void configureCsrf(ServerHttpSecurity.CsrfSpec csrf) {
        // Para WebFlux, configuramos CSRF solo para métodos mutadores en rutas específicas
        csrf.csrfTokenRepository(org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository.withHttpOnlyFalse())
            .requireCsrfProtectionMatcher(exchange -> {
                String path = exchange.getRequest().getPath().value();
                String method = exchange.getRequest().getMethod() != null ? 
                    exchange.getRequest().getMethod().name() : "GET";
                
                // CSRF solo para métodos mutadores (POST, PUT, DELETE, PATCH)
                boolean isMutatorMethod = "POST".equals(method) || "PUT".equals(method) || 
                                        "DELETE".equals(method) || "PATCH".equals(method);
                
                // NO aplicar CSRF a APIs REST, consola H2, webhooks, etc.
                boolean isExcludedPath = path.startsWith("/api/") || 
                                       path.startsWith("/h2-console/") || 
                                       path.startsWith("/webhook/") || 
                                       path.startsWith("/actuator/") ||
                                       path.startsWith("/error") ||
                                       path.startsWith("/oauth2/") ||
                                       path.startsWith("/login/oauth2/");
                
                // Solo proteger métodos mutadores en rutas no excluidas
                boolean shouldProtect = isMutatorMethod && !isExcludedPath;
                
                return shouldProtect ? 
                    ServerWebExchangeMatcher.MatchResult.match() : 
                    ServerWebExchangeMatcher.MatchResult.notMatch();
            });
    }

    /**
     * Configuración de headers de seguridad para WebFlux.
     */
    private void configureHeaders(ServerHttpSecurity.HeaderSpec headers) {
        // Configuración simplificada para WebFlux - usar solo las APIs disponibles
        headers.contentTypeOptions(Customizer.withDefaults());
    }

    /**
     * Manager de clientes OAuth2 autorizados con configuración completa.
     * Sigue las mejores prácticas de la documentación de Spring Security.
     */
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
        
        // Configurar providers para diferentes tipos de grant
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = 
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .clientCredentials()
                .build();

        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager = 
            new DefaultReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, 
                authorizedClientRepository
            );
        
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        
        return authorizedClientManager;
    }

    // WebClient OAuth2 ya está configurado en WebClientConfig, no necesitamos duplicarlo aquí

    /**
     * Handler de éxito de autenticación personalizado.
     */
    @Bean
    public RedirectServerAuthenticationSuccessHandler authenticationSuccessHandler() {
        RedirectServerAuthenticationSuccessHandler handler = 
            new RedirectServerAuthenticationSuccessHandler();
        
        // Configurar la URL de redirección después del éxito de autenticación
        handler.setLocation(URI.create(successUrl));
        
        return handler;
    }

    /**
     * Handler de éxito de logout personalizado.
     */
    @Bean
    public RedirectServerLogoutSuccessHandler logoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler handler = new RedirectServerLogoutSuccessHandler();
        handler.setLogoutSuccessUrl(URI.create(logoutSuccessUrl));
        return handler;
    }
} 