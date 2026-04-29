package com.aigymtrainer.backend.security.filter;

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

        // Prevent basic attacks
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");

        // Detect environment (dev vs prod)
        boolean isDev = environment.matchesProfiles("dev") || environment.matchesProfiles("default");

        // connect-src for header response, allow localhost in dev for API calls, strict in prod
        String connectSrc = isDev
                ? "'self' http://localhost:8080"
                : "'self'";    //change to real domain when deploy

        // Dev allows inline for convenience, Prod is strict
        String styleSrc = isDev
                ? "'self' 'unsafe-inline'"
                : "'self'";

        // CSP Header
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src " + styleSrc + "; " +
                "img-src 'self' data: https:; " +    //THIS 'img-src....https' ALLOW ALL HTTPS WEB AND CAN BE INJECTED WITH <img> tag, ---> user a direct url to store image. Save url in postgress, save real image in Firebase. When backend try to load img, from url https:...firebase --> img-src allow, else not allow to prevent attacker
                "font-src 'self' data:; " +
                "connect-src " + connectSrc + "; " +
                "object-src 'none'; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self';"
        );

        // HSTS only when HTTPS
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getRequestURI().startsWith("/actuator");
    }
}
