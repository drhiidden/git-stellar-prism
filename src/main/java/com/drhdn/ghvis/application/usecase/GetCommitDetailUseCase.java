package com.drhdn.ghvis.application.usecase;

import com.drhdn.ghvis.domain.port.CacheService;
import com.drhdn.ghvis.domain.port.CommitRepository;
import com.drhdn.ghvis.domain.port.RateLimitService;
import com.drhdn.ghvis.domain.entity.Commit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Principal;

/**
 * Caso de uso para obtener detalles de un commit específico.
 * 
 * Implementa la lógica de aplicación para obtener detalles de commits,
 * incluyendo rate limiting, cache y manejo de errores.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetCommitDetailUseCase {
    
    private final CommitRepository commitRepository;
    private final CacheService cacheService;
    private final RateLimitService rateLimitService;
    
    /**
     * Ejecuta el caso de uso para obtener detalles de un commit.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param sha SHA del commit
     * @param principal Usuario autenticado
     * @return Mono con los detalles del commit
     */
    public Mono<Commit> execute(String owner, String repo, String sha, Principal principal) {
        String endpoint = buildEndpoint(owner, repo, sha);
        String cacheKey = buildCacheKey(owner, repo, sha, principal);
        
        log.debug("Ejecutando GetCommitDetailUseCase para {}/{} commit {}", owner, repo, sha);
        
        return rateLimitService.canMakeRequest(endpoint, principal)
            .flatMap(canMake -> {
                if (!canMake) {
                    log.warn("Rate limit excedido para commit {}/{}:{}", owner, repo, sha);
                    return Mono.error(new RateLimitService.RateLimitExceededException(
                        "Rate limit excedido para commit details", 60));
                }
                
                return cacheService.getOrFetch(cacheKey, 
                    () -> commitRepository.findBySha(owner, repo, sha, principal));
            })
            .doOnSuccess(commit -> 
                log.debug("Detalles de commit obtenidos para {}/{}:{}", owner, repo, sha))
            .doOnError(error -> 
                log.error("Error en GetCommitDetailUseCase para {}/{}:{}: {}", 
                    owner, repo, sha, error.getMessage()));
    }
    
    /**
     * Construye el endpoint para rate limiting.
     */
    private String buildEndpoint(String owner, String repo, String sha) {
        return String.format("/repos/%s/%s/commits/%s", owner, repo, sha);
    }
    
    /**
     * Construye la clave del cache.
     */
    private String buildCacheKey(String owner, String repo, String sha, Principal principal) {
        return String.format("commit:%s:%s:%s:%s", principal.getName(), owner, repo, sha);
    }
} 