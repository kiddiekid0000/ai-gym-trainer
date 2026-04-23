package com.aigymtrainer.backend.security.filter;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.aigymtrainer.backend.security.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtFilter jwtFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ValidToken_ShouldSetAuthenticationAndContinueChain() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String email = "user@example.com";
        String role = "USER";
        
        Cookie accessTokenCookie = new Cookie("accessToken", token);
        
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(role);
        when(jwtService.isTokenExpired(token)).thenReturn(false);
        
        // Mock SecurityContextHolder behavior
        SecurityContextHolder.setContext(securityContext);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(jwtService).isTokenExpired(token);
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_InvalidToken_ShouldClearContextAndContinueChain() throws ServletException, IOException {
        // Arrange
        String token = "invalid.jwt.token";
        
        Cookie accessTokenCookie = new Cookie("accessToken", token);
        
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
        when(jwtService.extractEmail(token)).thenThrow(new RuntimeException("Invalid token"));
        
        // Mock SecurityContextHolder behavior
        SecurityContextHolder.setContext(securityContext);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService).extractEmail(token);
        verify(filterChain).doFilter(request, response);
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
    }

    @Test
    void doFilterInternal_ExpiredToken_ShouldClearContextAndContinueChain() throws ServletException, IOException {
        // Arrange
        String token = "expired.jwt.token";
        String email = "user@example.com";
        String role = "USER";
        
        Cookie accessTokenCookie = new Cookie("accessToken", token);
        
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(role);
        when(jwtService.isTokenExpired(token)).thenReturn(true); // Token expired
        
        // Mock SecurityContextHolder behavior
        SecurityContextHolder.setContext(securityContext);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(jwtService).isTokenExpired(token);
        verify(filterChain).doFilter(request, response);
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
    }

    @Test
    void doFilterInternal_NoToken_ShouldContinueChainWithoutAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getCookies()).thenReturn(null); // No cookies

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService, never()).extractEmail(anyString());
        verify(jwtService, never()).extractRole(anyString());
        verify(jwtService, never()).isTokenExpired(anyString());
        verify(filterChain).doFilter(request, response);
        // SecurityContext should not be modified
    }

    @Test
    void doFilterInternal_MultipleCookies_ShouldFindCorrectTokenCookie() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String email = "user@example.com";
        String role = "USER";
        
        Cookie accessTokenCookie = new Cookie("accessToken", token);
        Cookie otherCookie = new Cookie("sessionId", "some-session");
        
        when(request.getCookies()).thenReturn(new Cookie[]{otherCookie, accessTokenCookie});
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(role);
        when(jwtService.isTokenExpired(token)).thenReturn(false);
        
        // Mock SecurityContextHolder behavior
        SecurityContextHolder.setContext(securityContext);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(jwtService).isTokenExpired(token);
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }
}