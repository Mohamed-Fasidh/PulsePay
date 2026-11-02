package com.pulsepay.config;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig {
    @Bean
    CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("GET"); config.addAllowedMethod("POST"); config.addAllowedMethod("PATCH"); config.addAllowedMethod("PUT"); config.addAllowedMethod("DELETE");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

    @Bean
    Filter correlationIdFilter() {
        return new Filter() {
            @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                String correlationId = httpRequest.getHeader("X-Request-Id");
                if (correlationId == null || correlationId.length() > 128) correlationId = UUID.randomUUID().toString();
                MDC.put("request_id", correlationId);
                httpResponse.setHeader("X-Request-Id", correlationId);
                try { chain.doFilter(request, response); } finally { MDC.remove("request_id"); }
            }
        };
    }
}
