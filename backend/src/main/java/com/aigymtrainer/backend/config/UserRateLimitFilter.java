package com.aigymtrainer.backend.config;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aigymtrainer.backend.user.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(4)
public class UserRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(UserRateLimitFilter.class);

    private final RateLimitService rateLimitService;

    public UserRateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            User user = (User) auth.getPrincipal();
            String userKey = "user:" + user.getId();
            Duration window = Duration.ofMinutes(1);
            int maxRequests = 20;

            boolean allowed = rateLimitService.isAllowed(userKey, maxRequests, window);
            if (!allowed) {
                logger.warn("Rate limit exceeded for user: {}", user.getEmail());
                response.setStatus(429);
                response.getWriter().write("Too many requests for this user");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}