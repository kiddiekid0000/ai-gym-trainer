package com.aigymtrainer.backend.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final Environment environment;

    public SecurityHeadersFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Prevent clickjacking
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");

        // CSP for SPA (React/Vite) - blocks most XSS without breaking app
        boolean isDev = environment.matchesProfiles("dev") || environment.matchesProfiles("default");
        String connectSrc = isDev ? "'self' http://localhost:8080;" : "'self' https://yourdomain.com;";  // Adjust prod domain

        response.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +  // Allow HTTPS images for CDN
            "font-src 'self' data:; " +
            "connect-src " + connectSrc + " " +
            "object-src 'none'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self';"
        );

        // HSTS only on HTTPS
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        filterChain.doFilter(request, response);
    }
}