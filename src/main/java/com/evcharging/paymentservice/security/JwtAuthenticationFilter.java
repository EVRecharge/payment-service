package com.evcharging.paymentservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Reads the Bearer token (issued by user-service or provider-service, both signed with the same
 * shared secret) and, if valid, authenticates the request as the token's entityId/entityType. The
 * Razorpay webhook endpoint is exempt (see SecurityConfig) since Razorpay never sends a JWT --
 * that endpoint verifies the request's HMAC signature instead.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (Boolean.TRUE.equals(jwtTokenProvider.validateToken(token))) {
                Long entityId = jwtTokenProvider.getEntityIdFromToken(token);
                String entityType = jwtTokenProvider.getEntityTypeFromToken(token);
                String authority = "ROLE_" + (entityType != null ? entityType : "USER");
                var auth = new UsernamePasswordAuthenticationToken(
                        entityId, null, List.of(new SimpleGrantedAuthority(authority)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
