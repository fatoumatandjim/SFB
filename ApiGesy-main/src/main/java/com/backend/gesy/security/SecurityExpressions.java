package com.backend.gesy.security;

/**
 * Expressions SpEL {@link org.springframework.security.access.prepost.PreAuthorize} réutilisables.
 * Aligné sur les autorités JWT {@code ROLE_*} du {@link com.backend.gesy.security.service.CustomUserDetailsService}.
 */
public final class SecurityExpressions {

    private SecurityExpressions() {
    }

    /** Rôle applicatif « admin » (autorité effective {@code ROLE_ADMIN}). */
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
}
