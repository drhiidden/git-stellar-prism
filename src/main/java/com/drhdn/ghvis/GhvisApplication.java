package com.drhdn.ghvis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Para habilitar @Scheduled en UserRepositoryCacheService
public class GhvisApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GhvisApplication.class);
        app.run(args);
        System.out.println("\n🚀 GitStellarPrism iniciado correctamente!");
        System.out.println("📱 Interfaz web: http://localhost:8080");
        System.out.println("🔐 OAuth2 GitHub configurado");
        System.out.println("📊 H2 Console: http://localhost:8080/h2-console");
        System.out.println("✨ ¡Listo para visualizar repositorios!\n");
    }

}
