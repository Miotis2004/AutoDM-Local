package com.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cross-Origin Resource Sharing (CORS) configuration for the local development
 * workflow.
 *
 * <p>The Angular front-end runs on its own dev server (by default
 * <code>http://localhost:5300</code>) which is a different origin from the
 * back-end API served on <code>http://localhost:5150</code>. Requests made from
 * the browser are therefore cross-origin and require the back-end to advertise
 * permissive CORS headers for the preflight <code>OPTIONS</code> requests and
 * the subsequent API calls.</p>
 *
 * <p>The configuration is registered globally so every endpoint is covered. The
 * configured origins cover the local dev server (with its usual port swaps for
 * <code>4200</code>/<code>3000</code>) so the front-end can talk to the API
 * directly, without relying on the dev-server proxy.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Origins that are allowed to call the back-end from a browser. The list
     * covers the local Angular dev server on its standard and commonly used
     * alternate ports.
     */
    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:5300",
            "http://localhost:4200",
            "http://localhost:3000",
            "http://127.0.0.1:5300",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:3000"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
