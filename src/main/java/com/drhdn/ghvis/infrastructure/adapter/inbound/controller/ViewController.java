package com.drhdn.ghvis.infrastructure.adapter.inbound.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;

/**
 * Controlador encargado de servir las vistas Thymeleaf con fragmentos reutilizables.
 */
@Controller
public class ViewController {

    /**
     * Página principal - redirige al dashboard si está autenticado, sino muestra landing
     */
    @GetMapping("/")
    public Mono<String> index(Principal principal, Model model, @RequestParam(value = "repo", required = false) String repo) {
        // Variables de autenticación seguras
        model.addAttribute("isAuthenticated", principal != null);
        if (principal != null) {
            model.addAttribute("userPrincipal", principal);
            
            // Si hay un parámetro repo, ir directamente al análisis
            if (repo != null && !repo.trim().isEmpty()) {
                model.addAttribute("initialRepo", repo);
                // Variables para fragmentos - página de análisis con repo directo
                model.addAttribute("showBreadcrumb", true);
                model.addAttribute("breadcrumbTitle", "Análisis");
                model.addAttribute("breadcrumbIcon", "fas fa-search");
                model.addAttribute("showRealtimeStatus", true);
                return Mono.just("analysis");
            }
            // Si está autenticado sin parámetro repo, ir al dashboard
            return Mono.just("redirect:/dashboard");
        }
        
        // No autenticado, mostrar página de landing/login
        model.addAttribute("showBreadcrumb", false);
        model.addAttribute("showRealtimeStatus", false);
        return Mono.just("index");
    }

    /**
     * Dashboard para usuarios autenticados
     */
    @GetMapping("/dashboard")
    public Mono<String> dashboard(Principal principal, Model model) {
        if (principal == null) {
            return Mono.just("redirect:/");
        }
        
        // Variables para fragmentos - dashboard no necesita breadcrumb ni estado tiempo real
        model.addAttribute("showBreadcrumb", false);
        model.addAttribute("showRealtimeStatus", false);
        
        // Variables de autenticación seguras
        model.addAttribute("isAuthenticated", principal != null);
        if (principal != null) {
            model.addAttribute("userPrincipal", principal);
        }
        
        return Mono.just("dashboard");
    }

    /**
     * Página de análisis de repositorio
     */
    @GetMapping("/analysis")
    public Mono<String> analysis(Principal principal, Model model, 
                                @RequestParam(value = "repo", required = false) String repo) {
        if (repo != null && !repo.trim().isEmpty()) {
            model.addAttribute("initialRepo", repo);
        }
        
        // Variables para fragmentos - análisis necesita breadcrumb y estado tiempo real
        model.addAttribute("showBreadcrumb", true);
        model.addAttribute("breadcrumbTitle", "Análisis");
        model.addAttribute("breadcrumbIcon", "fas fa-search");
        model.addAttribute("showRealtimeStatus", true);
        
        // Variables de autenticación seguras
        model.addAttribute("isAuthenticated", principal != null);
        if (principal != null) {
            model.addAttribute("userPrincipal", principal);
        }
        
        return Mono.just("analysis");
    }

    /**
     * Página de resumen técnico
     */
    @GetMapping("/summary")
    public String summary(Model model) {
        // Variables para fragmentos - resumen necesita breadcrumb pero no estado tiempo real
        model.addAttribute("showBreadcrumb", true);
        model.addAttribute("breadcrumbTitle", "Resumen");
        model.addAttribute("breadcrumbIcon", "fas fa-file-alt");
        model.addAttribute("showRealtimeStatus", false);
        
        return "summary"; // src/main/resources/templates/summary.html
    }
} 