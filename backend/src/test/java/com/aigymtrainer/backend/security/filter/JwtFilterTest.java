package com.aigymtrainer.backend.security.filter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
        // Given
        String token = "valid.jwt.token";
        String email = "user@example.com";
        String role = "USER";
        
        Cookie accessTokenCookie = new Cookie("accessToken", token);
        
        given(request.getCookies()).willReturn(new Cookie[]{accessTokenCookie});
        given(jwtService.extractEmail(token)).willReturn(email);
        given(jwtService.extractRole(token)).willReturn(role);
        given(jwtService.isTokenExpired(token)).willReturn(false);
        
        // Mock SecurityContextHolder behavior
        SecurityContextHolder.setContext(securityContext);
        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(jwtService).isTokenExpired(token);
        verify(securityContext).setAuthentication(authCaptor.capture());
        verify(filterChain).doFilter(request, response);
        
        Authentication capturedAuth = authCaptor.getValue();
        assertThat(capturedAuth.getPrincipal()).isEqualTo(email);
        assertThat(capturedAuth.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_" + role);
    }

    @Test
    void doFilterInternal_InvalidToken_ShouldClearContextAndContinueChain() throws ServletException, IOException {
        // Given
        String token = "invalid.jwt.token";
        
        Cookie accessTokenCookie = new Cookie("accessToken", token);
        
        given(request.getCookies()).willReturn(new Cookie[]{accessTokenCookie});
        given(jwtService.extractEmail(token)).willThrow(new RuntimeException("Invalid token"));
        
        // Mock SecurityContextHolder behavior
        SecurityContextHolder.setContext(securityContext);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService).extractEmail(token);
        verify(filterChain).doFilter(request, response);
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_ExpiredToken_ShouldClearContextAndContinueChain() throws ServletException, IOException {
        // Given
        String token = "expired.jwt.token";
        String email = "user@example.com";
        String role = "USER";
        
        Cookie accessTokenCookie = new Cookie("accessToken", token);
        
        given(request.getCookies()).willReturn(new Cookie[]{accessTokenCookie});
        given(jwtService.extractEmail(token)).willReturn(email);
        given(jwtService.extractRole(token)).willReturn(role);
        given(jwtService.isTokenExpired(token)).willReturn(true); // Token expired
        
        // Mock SecurityContextHolder behavior
        SecurityContextHolder.setContext(securityContext);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(jwtService).isTokenExpired(token);
        verify(filterChain).doFilter(request, response);
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_NoToken_ShouldContinueChainWithoutAuthentication() throws ServletException, IOException {
        // Given
        given(request.getCookies()).willReturn(null); // No cookies

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, never()).extractEmail(anyString());
        verify(jwtService, never()).extractRole(anyString());
        verify(jwtService, never()).isTokenExpired(anyString());
        verify(filterChain).doFilter(request, response);
        // SecurityContext should not be modified
    }

    @Test
    void doFilterInternal_MultipleCookies_ShouldFindCorrectTokenCookie() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String email = "user@example.com";
        String role = "USER";
        
        Cookie accessTokenCookie = new Cookie("accessToken", token);
        Cookie otherCookie = new Cookie("sessionId", "some-session");
        
        given(request.getCookies()).willReturn(new Cookie[]{otherCookie, accessTokenCookie});
        given(jwtService.extractEmail(token)).willReturn(email);
        given(jwtService.extractRole(token)).willReturn(role);
        given(jwtService.isTokenExpired(token)).willReturn(false);
        
        // Mock SecurityContextHolder behavior
        SecurityContextHolder.setContext(securityContext);
        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(jwtService).isTokenExpired(token);
        verify(securityContext).setAuthentication(authCaptor.capture());
        verify(filterChain).doFilter(request, response);
        
        Authentication capturedAuth = authCaptor.getValue();
        assertThat(capturedAuth.getPrincipal()).isEqualTo(email);
        assertThat(capturedAuth.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_" + role);
    }

    @Test
    void doFilterInternal_ActuatorUri_ShouldSkipFilter() throws ServletException, IOException {
        // Given
        lenient().when(request.getRequestURI()).thenReturn("/actuator/health");
        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_EmptyTokenCookie_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        Cookie emptyTokenCookie = new Cookie("accessToken", "");
        
        given(request.getCookies()).willReturn(new Cookie[]{emptyTokenCookie});

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, never()).extractEmail(anyString());
        verify(jwtService, never()).extractRole(anyString());
        verify(jwtService, never()).isTokenExpired(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_BlankTokenCookie_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        Cookie blankTokenCookie = new Cookie("accessToken", "   ");
        
        given(request.getCookies()).willReturn(new Cookie[]{blankTokenCookie});

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, never()).extractEmail(anyString());
        verify(jwtService, never()).extractRole(anyString());
        verify(jwtService, never()).isTokenExpired(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_CookiesWithoutAccessToken_ShouldContinueWithoutAuthentication() throws ServletException, IOException {
        // Given
        Cookie otherCookie1 = new Cookie("sessionId", "some-session");
        Cookie otherCookie2 = new Cookie("userId", "123");
        
        given(request.getCookies()).willReturn(new Cookie[]{otherCookie1, otherCookie2});

        // When
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtService, never()).extractEmail(anyString());
        verify(jwtService, never()).extractRole(anyString());
        verify(jwtService, never()).isTokenExpired(anyString());
        verify(filterChain).doFilter(request, response);
    }
}