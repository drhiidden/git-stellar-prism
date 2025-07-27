package com.drhdn.ghvis.application.usecase;

import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.CommitRepository;
import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.domain.entity.Commit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.security.Principal;

/**
 * Caso de uso para obtener commits de un repositorio.
 * 
 * Implementa la lógica de aplicación para obtener commits,
 * incluyendo rate limiting, cache y manejo de errores.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryCommitsUseCase {
    
    private final CommitRepository commitRepository;
    private final CacheService cacheService;
    private final RateLimitService rateLimitService;
    
    /**
     * Ejecuta el caso de uso para obtener commits de un repositorio.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Flux de commits del repositorio
     */
    public Flux<Commit> execute(String owner, String repo, Principal principal) {
        String endpoint = buildEndpoint(owner, repo);
        String cacheKey = buildCacheKey(owner, repo, principal);
        
        log.debug("Ejecutando GetRepositoryCommitsUseCase para {}/{}", owner, repo);
        
        return rateLimitService.canMakeRequest(endpoint, principal)
            .flatMapMany(canMake -> {
                if (!canMake) {
                    log.warn("Rate limit excedido para {}/{}", owner, repo);
                    return Flux.error(new RateLimitService.RateLimitExceededException(
                        "Rate limit excedido para commits", 60));
                }
                
                return cacheService.getOrFetch(cacheKey, 
                    () -> commitRepository.findByRepository(owner, repo, principal).collectList())
                    .flatMapMany(commits -> Flux.fromIterable(commits));
            })
            .doOnSubscribe(subscription -> 
                log.debug("Obteniendo commits para repositorio: {}/{}", owner, repo))
            .doOnComplete(() -> 
                log.debug("Completado GetRepositoryCommitsUseCase para {}/{}", owner, repo))
            .doOnError(error -> 
                log.error("Error en GetRepositoryCommitsUseCase para {}/{}: {}", 
                    owner, repo, error.getMessage()));
    }
    
    /**
     * Construye el endpoint para rate limiting.
     */
    private String buildEndpoint(String owner, String repo) {
        return String.format("/repos/%s/%s/commits", owner, repo);
    }
    
    /**
     * Construye la clave del cache.
     */
    private String buildCacheKey(String owner, String repo, Principal principal) {
        return String.format("commits:%s:%s:%s", principal.getName(), owner, repo);
    }
} 