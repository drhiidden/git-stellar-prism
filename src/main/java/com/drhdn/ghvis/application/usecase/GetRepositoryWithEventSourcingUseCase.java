package com.drhdn.ghvis.application.usecase;

import com.drhdn.ghvis.application.service.EventSourcingService;
import com.drhdn.ghvis.domain.entity.Repository;
import com.drhdn.ghvis.domain.event.DomainEvent;
import com.drhdn.ghvis.domain.port.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

/**
 * Caso de uso para obtener un repositorio usando Event Sourcing.
 * 
 * @author GitStellarPrism Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetRepositoryWithEventSourcingUseCase {
    
    private final RepositoryRepository repositoryRepository;
    private final EventSourcingService eventSourcingService;
    
    /**
     * Obtiene un repositorio usando Event Sourcing.
     * 
     * @param owner Propietario del repositorio
     * @param repo Nombre del repositorio
     * @param principal Usuario autenticado
     * @return Mono con el repositorio
     */
    public Mono<Repository> execute(String owner, String repo, Principal principal) {
        String aggregateId = owner + "/" + repo;
        log.info("🔄 Obteniendo repositorio con Event Sourcing: {}", aggregateId);
        
        // Intentar cargar el repositorio desde el Event Store
        return eventSourcingService.loadAggregate(aggregateId, "repository", id -> new Repository())
            .cast(Repository.class)
            .switchIfEmpty(
                // Si no existe en el Event Store, obtenerlo de GitHub y guardarlo
                repositoryRepository.findByOwnerAndName(owner, repo, principal)
                    .flatMap(repository -> {
                        // Obtener eventos pendientes del repositorio
                        List<DomainEvent> events = repository.getPendingEvents();
                        
                        // Guardar eventos en el Event Store
                        return eventSourcingService.saveEvents(repository, events)
                            .thenReturn(repository);
                    })
            )
            .doOnSuccess(repository -> log.info("✅ Repositorio obtenido con Event Sourcing: {}", aggregateId))
            .doOnError(error -> log.error("❌ Error obteniendo repositorio con Event Sourcing: {} - {}", 
                                        aggregateId, error.getMessage()));
    }
    
    /**
     * Actualiza un repositorio usando Event Sourcing.
     * 
     * @param repository Repositorio a actualizar
     * @return Mono con el repositorio actualizado
     */
    public Mono<Repository> update(Repository repository) {
        String aggregateId = repository.getOwner() + "/" + repository.getName();
        log.info("🔄 Actualizando repositorio con Event Sourcing: {}", aggregateId);
        
        // Obtener eventos pendientes del repositorio
        List<DomainEvent> events = repository.getPendingEvents();
        
        if (events.isEmpty()) {
            log.info("ℹ️ No hay eventos para guardar para el repositorio: {}", aggregateId);
            return Mono.just(repository);
        }
        
        // Guardar eventos en el Event Store
        return eventSourcingService.saveEvents(repository, events)
            .thenReturn(repository)
            .doOnSuccess(repo -> log.info("✅ Repositorio actualizado con Event Sourcing: {}", aggregateId))
            .doOnError(error -> log.error("❌ Error actualizando repositorio con Event Sourcing: {} - {}", 
                                        aggregateId, error.getMessage()));
    }
} 