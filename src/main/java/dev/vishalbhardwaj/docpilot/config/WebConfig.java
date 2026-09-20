package dev.vishalbhardwaj.docpilot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The embeddable widget calls the chat API from third-party origins, so CORS must
 * allow it. This is intentionally permissive for the demo — lock it down to explicit
 * origins before any production use.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/chat/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST");
        registry.addMapping("/api/v1/tickets")
                .allowedOrigins("*")
                .allowedMethods("POST");
    }
}
