package com.aigymtrainer.backend.security.filter;

import java.io.IOException;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.aigymtrainer.backend.security.service.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter writer;

    @Test
    void doFilterInternal_NonAuthUri_ShouldContinueChainWithoutRateLimitCheck() throws ServletException, IOException {
        // Given
        given(request.getRequestURI()).willReturn("/api/user");

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService, never()).isIpRateLimited(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthUri_NotRateLimited_ShouldContinueChain() throws ServletException, IOException {
        // Given
        String clientIp = "192.168.1.1";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getRemoteAddr()).willReturn(clientIp);
        given(rateLimitService.isIpRateLimited(clientIp)).willReturn(false);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).isIpRateLimited(clientIp);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthUri_RateLimited_ShouldReturn429() throws ServletException, IOException {
        // Given
        String clientIp = "192.168.1.1";
        given(request.getRequestURI()).willReturn("/auth/register");
        given(request.getRemoteAddr()).willReturn(clientIp);
        given(rateLimitService.isIpRateLimited(clientIp)).willReturn(true);
        given(response.getWriter()).willReturn(writer);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).isIpRateLimited(clientIp);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        verify(writer).write("{\"error\": \"Too many requests. Please try again later.\"}");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthUri_WithXForwardedForHeader_FromTrustedProxy_ShouldUseLeftmostIp() throws ServletException, IOException {
        // Given
        String xForwardedFor = "10.0.0.1, 10.0.0.2";
        String expectedClientIp = "10.0.0.1";
        String trustedProxyIp = "172.17.0.1";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getRemoteAddr()).willReturn(trustedProxyIp);
        given(request.getHeader("X-Forwarded-For")).willReturn(xForwardedFor);
        given(rateLimitService.isIpRateLimited(expectedClientIp)).willReturn(false);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).isIpRateLimited(expectedClientIp);
        verify(filterChain).doFilter(request, response);
    }

    

    @Test
    void doFilterInternal_AuthUri_FromUntrustedProxy_ShouldUseRemoteAddr() throws ServletException, IOException {
        // Given
        String untrustedProxyIp = "203.0.113.1";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getRemoteAddr()).willReturn(untrustedProxyIp);
        given(request.getHeader("X-Forwarded-For")).willReturn("10.0.0.1"); // Should ignore
        given(rateLimitService.isIpRateLimited(untrustedProxyIp)).willReturn(false);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).isIpRateLimited(untrustedProxyIp);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthUri_NoHeaders_ShouldUseRemoteAddr() throws ServletException, IOException {
        // Given
        String remoteAddr = "192.168.1.100";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getRemoteAddr()).willReturn(remoteAddr);
        given(rateLimitService.isIpRateLimited(remoteAddr)).willReturn(false);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).isIpRateLimited(remoteAddr);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ActuatorUri_ShouldSkipFilter() throws ServletException, IOException {
        // Given
        given(request.getRequestURI()).willReturn("/actuator/health");

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verifyNoInteractions(rateLimitService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotFilter_ActuatorUri_ShouldReturnTrue() throws ServletException {
        // Given
        given(request.getRequestURI()).willReturn("/actuator/health");

        // When
        boolean result = rateLimitFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFilter_NonActuatorUri_ShouldReturnFalse() throws ServletException {
        // Given
        given(request.getRequestURI()).willReturn("/auth/login");

        // When
        boolean result = rateLimitFilter.shouldNotFilter(request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void doFilterInternal_EmptyXForwardedFor_ShouldFallbackToRemoteAddr() throws ServletException, IOException {
        String trustedProxy = "172.17.0.1";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getRemoteAddr()).willReturn(trustedProxy);
        given(request.getHeader("X-Forwarded-For")).willReturn("");
        given(rateLimitService.isIpRateLimited(trustedProxy)).willReturn(false);
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(rateLimitService).isIpRateLimited(trustedProxy);
    }

    @Test
    void doFilterInternal_XForwardedForNull_ShouldFallbackToRemoteAddr() throws ServletException, IOException {
        String trustedProxy = "172.17.0.1";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getRemoteAddr()).willReturn(trustedProxy);
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(rateLimitService.isIpRateLimited(trustedProxy)).willReturn(false);
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(rateLimitService).isIpRateLimited(trustedProxy);
    }

    @Test
    void doFilterInternal_FromTrustedProxyWith10x_ShouldUseLeftmostIp() throws ServletException, IOException {
        String xForwardedFor = "1.2.3.4, 5.6.7.8";
        String expectedClientIp = "1.2.3.4";
        String trustedProxyIp = "172.17.0.1";  // Fixed to trusted IP
        
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getRemoteAddr()).willReturn(trustedProxyIp);
        given(request.getHeader("X-Forwarded-For")).willReturn(xForwardedFor);
        given(rateLimitService.isIpRateLimited(expectedClientIp)).willReturn(false);
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(rateLimitService).isIpRateLimited(expectedClientIp);
    }

    @Test
    void doFilterInternal_XForwardedForWithSpaces_ShouldTrimCorrectly() throws ServletException, IOException {
        String xForwardedFor = "  192.168.1.1  ,   10.0.0.2  ";
        String expectedClientIp = "192.168.1.1";
        String trustedProxyIp = "172.17.0.1";
        
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getRemoteAddr()).willReturn(trustedProxyIp);
        given(request.getHeader("X-Forwarded-For")).willReturn(xForwardedFor);
        given(rateLimitService.isIpRateLimited(expectedClientIp)).willReturn(false);
        
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        
        verify(rateLimitService).isIpRateLimited(expectedClientIp);
    }
}
