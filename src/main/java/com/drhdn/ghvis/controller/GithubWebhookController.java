package com.drhdn.ghvis.controller;

import com.drhdn.ghvis.model.Event;
import com.drhdn.ghvis.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint receptor de Webhooks de GitHub.
 */
@RestController
@ConditionalOnProperty(name = "app.realtime.enabled", havingValue = "true")
@RequestMapping("/webhook")
@Slf4j
@RequiredArgsConstructor
public class GithubWebhookController {

    private final EventPublisherService eventPublisherService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestHeader("X-GitHub-Event") String eventType,
                                              @RequestBody Map<String, Object> payload) {
        log.info("Webhook recibido: {}", eventType);

        // Convertimos a evento interno (versión simplificada)
        Event event = Event.builder()
                .id(UUID.randomUUID().toString())
                .type(eventType)
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        eventPublisherService.publish(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
} 