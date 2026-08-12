package com.pms.hotel.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resolves the caller's identity from the JWT issued by the external identity
 * service. This service owns no user table of its own: user records, roles and
 * permissions are managed elsewhere and simply asserted in the token claims.
 */
@Component
public class CurrentUser {

    public Long userId() {
        Jwt jwt = jwt();
        String subject = jwt.getSubject();
        try {
            return subject == null ? null : Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Le claim 'sub' du token n'est pas un identifiant utilisateur valide.");
        }
    }

    public String name() {
        return jwt().getClaimAsString("name");
    }

    private Jwt jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Aucun utilisateur authentifié dans le contexte courant.");
        }
        return jwt;
    }
}
