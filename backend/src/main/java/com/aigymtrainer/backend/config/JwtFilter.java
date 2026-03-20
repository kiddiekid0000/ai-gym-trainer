package com.aigymtrainer.backend.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
@Order(3)
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Value("${admin.email}")
    private String adminEmail;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {

    // Allow auth since user haven't have token
    if (request.getServletPath().startsWith("/auth")) {
        filterChain.doFilter(request, response);
        return;
    }

    // Get token from cookie
    String token = null;
    if (request.getCookies() != null) {
        for (var cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }
    }

    if (token == null || token.isEmpty()) {
        filterChain.doFilter(request, response);
        return;
    }

    String email = jwtService.extractEmail(token);

    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        // Determine role: admin if email matches, else user
        String role = email.equals(adminEmail) ? "ROLE_ADMIN" : "ROLE_USER";
        var authorities = List.of(new SimpleGrantedAuthority(role));

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    filterChain.doFilter(request, response);
}
}