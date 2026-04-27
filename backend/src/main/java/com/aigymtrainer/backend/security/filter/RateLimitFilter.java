package com.aigymtrainer.backend.security.filter;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aigymtrainer.backend.security.service.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final Set<String> trustedProxies = Set.of("127.0.0.1", "172.17.0.1");
    
    // Only use Regex from IP 172.16.0.0 to 172.31.255.255
    private static final Pattern DOCKER_PRIVATE_RANGE = 
        Pattern.compile("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        if (request.getRequestURI().startsWith("/auth")) {
            String clientIp = getClientIp(request);
            
            if (rateLimitService.isIpRateLimited(clientIp)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        
        if (isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
                String[] ips = xForwardedFor.split(",");
                // FIXED: take the 1st ip (most left) as the client IP
                for (int i = 0; i < ips.length; i++) {
                    String ip = ips[i].trim();
                    if (!ip.isEmpty()) {
                        return ip;
                    }
                }
            }
        }
        
        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        if (trustedProxies.contains(ip)) {
            return true;
        }
        // Chỉ trust dải private 172.16.0.0/12
        return DOCKER_PRIVATE_RANGE.matcher(ip).matches();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
