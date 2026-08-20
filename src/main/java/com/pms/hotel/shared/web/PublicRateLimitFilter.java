package com.pms.hotel.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

/**
 * Limite le débit des endpoints publics ({@code /api/v1/public/**}), seuls
 * exposés sans authentification (voir SecurityConfig) — donc les seuls
 * atteignables par n'importe qui sur Internet. Enregistré explicitement
 * (voir PublicRateLimitFilterConfig), pas en @Component : évite tout risque
 * de double-enregistrement par le scan automatique des beans Filter de
 * Spring Boot.
 */
@RequiredArgsConstructor
public class PublicRateLimitFilter implements jakarta.servlet.Filter {

    private static final int GET_LIMIT_PER_MINUTE = 30;
    private static final int WRITE_LIMIT_PER_MINUTE = 5;

    private final RateLimiter rateLimiter;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        boolean isRead = "GET".equalsIgnoreCase(request.getMethod());
        int limit = isRead ? GET_LIMIT_PER_MINUTE : WRITE_LIMIT_PER_MINUTE;
        String key = (isRead ? "public-get:" : "public-write:") + clientIp(request);

        if (!rateLimiter.tryConsume(key, limit, Duration.ofMinutes(1))) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Trop de requêtes. Merci de réessayer dans une minute.\"}");
            return;
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
