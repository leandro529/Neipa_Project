package com.neipa.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para que el frontend React (SPA aparte, servido por Vite en
 * desarrollo o como build estatico en produccion) pueda consumir la API REST
 * de este backend, que corren en origenes/puertos distintos.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${neipa.frontend.origenes-permitidos:http://localhost:5173,http://localhost:3000}")
    private String[] origenesPermitidos;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // Para que la SPA pueda leer el nombre de los XML/ZIP de reconfiguracion.
                .exposedHeaders("Content-Disposition");
    }
}
