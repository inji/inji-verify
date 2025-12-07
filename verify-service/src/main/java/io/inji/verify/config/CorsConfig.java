package io.inji.verify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow all origins - you can restrict this to specific domains if needed
        config.addAllowedOrigin("*");
        // Or more specifically:
        // config.addAllowedOrigin("http://localhost:3000");

        // Allow all headers
        config.addAllowedHeader("*");

        // Allow all HTTP methods
        config.addAllowedMethod("*");

        // Allow credentials (if needed)
        // config.setAllowCredentials(true); // Note: Cannot use with allowedOrigin("*")

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}