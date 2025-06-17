package com.drhdn.ghvis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador encargado de servir las vistas Thymeleaf.
 */
@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index"; // src/main/resources/templates/index.html
    }

    @GetMapping("/summary")
    public String summary() {
        return "summary"; // src/main/resources/templates/summary.html
    }
} 