package com.pms.hotel.property;

import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ForbiddenActionException;
import com.pms.hotel.shared.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Résout l'établissement dans lequel une requête authentifiée opère.
 * <ul>
 *   <li>Un rôle "large" (admin/super-admin) voit tous les établissements actifs.</li>
 *   <li>Tant qu'un seul établissement actif existe, tout utilisateur y a un
 *       accès implicite — amorçage mono-établissement : aucun déploiement
 *       existant n'a besoin de configurer d'accès avant de fonctionner.</li>
 *   <li>Sinon, seuls les établissements explicitement accordés via
 *       {@code UserPropertyAccess} sont accessibles.</li>
 * </ul>
 * L'établissement demandé se lit dans l'en-tête {@code X-Property-Id} — absent
 * quand un seul établissement est accessible (résolu automatiquement), requis
 * dès que plusieurs le sont. Inutilisable pour une requête non authentifiée
 * (voir BookingPublicController, qui résout son propre propertyId via un
 * paramètre de requête).
 */
@Component
@RequiredArgsConstructor
public class CurrentProperty {

    private static final String HEADER = "X-Property-Id";
    private static final Set<String> PROPERTY_WIDE_ROLES = Set.of("ROLE_ADMIN", "ROLE_SUPER-ADMIN");

    private final PropertyApi propertyApi;
    private final CurrentUser currentUser;

    /** L'id de l'établissement courant — lève une exception si ambigu ou non autorisé. */
    public Long resolve() {
        List<Long> accessible = accessiblePropertyIds();

        String header = currentRequest().getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            Long requested;
            try {
                requested = Long.valueOf(header.trim());
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("En-tête X-Property-Id invalide.");
            }
            if (!accessible.contains(requested)) {
                throw new ForbiddenActionException("Vous n'avez pas accès à cet établissement.");
            }
            return requested;
        }

        if (accessible.size() == 1) {
            return accessible.get(0);
        }
        if (accessible.isEmpty()) {
            throw new ForbiddenActionException("Aucun établissement accessible pour cet utilisateur.");
        }
        throw new BusinessRuleException("Plusieurs établissements sont accessibles — précisez l'en-tête X-Property-Id.");
    }

    public List<PropertySummary> accessibleProperties() {
        return accessiblePropertyIds().stream().map(propertyApi::getById).toList();
    }

    private List<Long> accessiblePropertyIds() {
        List<Long> allActive = propertyApi.findAllActivePropertyIds();
        if (isPropertyWideRole() || allActive.size() <= 1) {
            return allActive;
        }
        return propertyApi.findGrantedPropertyIds(currentUser.userId());
    }

    private boolean isPropertyWideRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> PROPERTY_WIDE_ROLES.contains(authority.getAuthority()));
    }

    private HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
