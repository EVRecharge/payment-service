package com.evcharging.paymentservice.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiter (per client IP, per minute) for order creation, where a malicious or
 * buggy client hammering the endpoint could otherwise create unbounded Razorpay orders. In-memory
 * only -- fine for a single instance per service; would need a shared store (e.g. Redis) behind a
 * load balancer with multiple instances.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 15;
    private static final long WINDOW_MILLIS = 60_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().equals("/api/payments/orders")) {
            String ip = clientIp(request);
            Window window = windows.computeIfAbsent(ip, k -> new Window(System.currentTimeMillis()));
            synchronized (window) {
                long now = System.currentTimeMillis();
                if (now - window.windowStart > WINDOW_MILLIS) {
                    window.windowStart = now;
                    window.count.set(0);
                }
                if (window.count.incrementAndGet() > MAX_REQUESTS_PER_WINDOW) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Too many requests, please try again later.\"}");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Window {
        volatile long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long start) {
            this.windowStart = start;
        }
    }
}
