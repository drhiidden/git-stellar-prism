package com.drhdn.ghvis.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuración profesional para WebClient con integración OAuth2.
 * 
 * Esta configuración proporciona:
 * - WebClient básico para uso general
 * - WebClient con OAuth2 para APIs autenticadas
 * - WebClient específico para GitHub API
 * - Configuraciones de timeout y performance optimizadas
 */
@Configuration
public class WebClientConfig {

    @Value("${app.webclient.timeout.connection:10000}")
    private int connectionTimeout;

    @Value("${app.webclient.timeout.read:30000}")
    private int readTimeout;

    @Value("${app.webclient.timeout.write:30000}")
    private int writeTimeout;

    @Value("${app.webclient.max-memory-size:2097152}") // 2MB por defecto
    private int maxInMemorySize;

    /**
     * WebClient básico para uso general sin autenticación.
     * Configurado con timeouts optimizados y manejo de memoria.
     */
    @Bean
    @Primary
    public WebClient webClient() {
        return WebClient.builder()
            .clientConnector(createClientConnector())
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(maxInMemorySize);
                configurer.defaultCodecs().enableLoggingRequestDetails(true);
            })
            .build();
    }

    /**
     * WebClient configurado específicamente para GitHub API.
     * Incluye headers específicos y configuraciones optimizadas para GitHub.
     */
    @Bean("githubWebClient")
    public WebClient githubWebClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter = 
            new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        
        // Configurar el filtro para usar automáticamente el cliente 'github'
        oauth2Filter.setDefaultClientRegistrationId("github");
        
        // Crear HttpClient optimizado con DNS resolver explícito para GitHub
        HttpClient httpClient = HttpClient.create() 
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
            .doOnConnected(connection -> {
                connection
                    .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS));
            })
            .responseTimeout(Duration.ofMillis(readTimeout));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl("https://api.github.com")
            .defaultHeaders(headers -> {
                headers.add("Accept", "application/vnd.github.v3+json");
                headers.add("User-Agent", "GitStellarPrism/1.0");
                // Usar la versión actual oficial de GitHub API (verificada en 2025)
                headers.add("X-GitHub-Api-Version", "2022-11-28");
            })
            .filter(oauth2Filter)
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(maxInMemorySize);
                configurer.defaultCodecs().enableLoggingRequestDetails(true);
            })
            .build();
    }

    /**
     * WebClient genérico con OAuth2 para otras APIs que requieran autenticación.
     */
    @Bean("oAuth2WebClient")
    public WebClient oAuth2WebClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter = 
            new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        return WebClient.builder()
            .clientConnector(createClientConnector())
            .filter(oauth2Filter)
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(maxInMemorySize);
                configurer.defaultCodecs().enableLoggingRequestDetails(true);
            })
            .build();
    }

    /**
     * Crea un conector HTTP con configuraciones de timeout y performance optimizadas.
     */
    private ReactorClientHttpConnector createClientConnector() {
        return new ReactorClientHttpConnector(createOptimizedHttpClient());
    }
    
    /**
     * Crea un HttpClient optimizado con configuración de DNS y timeouts.
     */
    private HttpClient createOptimizedHttpClient() {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
            .doOnConnected(connection -> {
                connection
                    .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS));
            })
            .responseTimeout(Duration.ofMillis(readTimeout));
    }
} 