package com.aigymtrainer.backend.security.jwt;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Extract JWT token from cookies
        String token = TokenExtractor.extractTokenFromCookies(request);
        
        if (token != null) {
            try {
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);
                
                if (!jwtService.isTokenExpired(token)) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("User authenticated successfully");
                } else {
                    logger.warn("Token expired");
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception e) {
                // Token validation failed
                logger.warn("JWT validation failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

