package com.evcharging.paymentservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/** Shared helpers for reading the caller's identity/role and forwarding their own token onward. */
abstract class BaseAuthenticatedController {

    protected Long currentEntityId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
    }

    protected boolean hasRole(String entityType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + entityType));
    }

    protected String bearerToken(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    protected ResponseEntity<?> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", message));
    }
}
