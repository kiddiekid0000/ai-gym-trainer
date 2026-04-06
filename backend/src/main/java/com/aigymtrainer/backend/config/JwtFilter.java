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

import com.aigymtrainer.backend.auth.TokenService;
import com.aigymtrainer.backend.user.Status;
import com.aigymtrainer.backend.user.User;
import com.aigymtrainer.backend.user.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
@Order(3)
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Value("${admin.email}")
    private String adminEmail;

    public JwtFilter(JwtService jwtService, TokenService tokenService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
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

        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Check if user has a valid session in Redis (user hasn't logged out)
                // If no refresh token exists in Redis, the user has been logged out
                if (!tokenService.tokenExists(email)) {
                    // User has logged out, skip authentication
                    filterChain.doFilter(request, response);
                    return;
                }
                
                User user = null;
                if (email.equals(adminEmail)) {
                    // Admin user
                    user = new User();
                    user.setEmail(adminEmail);
                    user.setRole(com.aigymtrainer.backend.user.Role.ADMIN);
                    user.setStatus(Status.ACTIVE);
                } else {
                    // Regular user
                    user = userRepository.findByEmail(email).orElse(null);
                    if (user == null || user.getStatus() == Status.SUSPENDED) {
                        // User not found or suspended
                        filterChain.doFilter(request, response);
                        return;
                    }
                }

                // Determine role
                String role = "ROLE_" + user.getRole().name();
                var authorities = List.of(new SimpleGrantedAuthority(role));

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