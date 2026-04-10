package com.aigymtrainer.backend.config;

import java.io.IOException;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aigymtrainer.backend.auth.TokenService;
import com.aigymtrainer.backend.user.Role;
import com.aigymtrainer.backend.user.Status;
import com.aigymtrainer.backend.user.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
@Order(3)
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenService tokenService;

    public JwtFilter(JwtService jwtService, TokenService tokenService) {
        this.jwtService = jwtService;
        this.tokenService = tokenService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {

        // Allow auth endpoints since user haven't have token yet
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

        // Check if token is blacklisted before processing
        if (tokenService.isTokenBlacklisted(token)) {
            // Token has been revoked, reject immediately
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token has been revoked. Please login again.\"}");
            return;
        }

        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            if (email != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Check if user has a valid session in Redis (user hasn't logged out)
                // If no refresh token exists in Redis, the user has been logged out
                if (!tokenService.tokenExists(email)) {
                    // User has logged out, skip authentication
                    filterChain.doFilter(request, response);
                    return;
                }
                
                User user;
                if (role.equals(Role.ADMIN.name())) {
                    // Admin user - create dynamic user object
                    user = new User();
                    user.setEmail(email);
                    user.setRole(Role.ADMIN);
                    user.setStatus(Status.ACTIVE);
                } else {
                    // Regular user - create user object with role from JWT
                    user = new User();
                    user.setEmail(email);
                    user.setRole(Role.USER);
                    user.setStatus(Status.ACTIVE);
                }

                // Determine role from JWT claim
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token validation failed, skip authentication
        }

        filterChain.doFilter(request, response);
    }
}