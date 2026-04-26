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

import com.aigymtrainer.backend.security.service.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
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
    void doFilterInternal_AuthUri_WithXForwardedForHeader_ShouldUseXForwardedForIp() throws ServletException, IOException {
        // Given
        String xForwardedFor = "10.0.0.1, 10.0.0.2";
        String clientIp = "10.0.0.1";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getHeader("X-Forwarded-For")).willReturn(xForwardedFor);
        given(rateLimitService.isIpRateLimited(clientIp)).willReturn(false);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).isIpRateLimited(clientIp);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthUri_WithXRealIpHeader_ShouldUseXRealIp() throws ServletException, IOException {
        // Given
        String xRealIp = "10.0.0.3";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(xRealIp);
        given(rateLimitService.isIpRateLimited(xRealIp)).willReturn(false);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(rateLimitService).isIpRateLimited(xRealIp);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AuthUri_NoHeaders_ShouldUseRemoteAddr() throws ServletException, IOException {
        // Given
        String remoteAddr = "192.168.1.100";
        given(request.getRequestURI()).willReturn("/auth/login");
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
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
}
