package com.drhdn.ghvis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.features")
public class FeatureFlagsConfig {
    
    private Cache cache = new Cache();
    private Resilience resilience = new Resilience();
    private Metrics metrics = new Metrics();
    
    @Data
    public static class Cache {
        private boolean enabled = true;
    }
    
    @Data
    public static class Resilience {
        private boolean enabled = true;
    }
    
    @Data
    public static class Metrics {
        private boolean enabled = true;
    }
    
    /**
     * Verifica si una caracteristica esta habilitada.
     * 
     * @param feature Nombre de la caracteristica
     * @return true si esta habilitada
     */
    public boolean isEnabled(String feature) {
        return switch (feature.toLowerCase()) {
            case "cache" -> cache.isEnabled();
            case "resilience" -> resilience.isEnabled();
            case "metrics" -> metrics.isEnabled();
            default -> false;
        };
    }
} 