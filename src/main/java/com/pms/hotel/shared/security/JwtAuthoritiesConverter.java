package com.pms.hotel.shared.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps the "roles" and "permissions" claims asserted by the external identity
 * service onto Spring Security authorities:
 * <ul>
 *   <li>roles: {@code ["admin"]} -> {@code ROLE_ADMIN}, usable with {@code hasRole("ADMIN")}</li>
 *   <li>permissions: {@code ["bookings.create"]} -> {@code bookings.create} as-is,
 *       usable with {@code hasAuthority("bookings.create")}</li>
 * </ul>
 */
public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT))));
        }

        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions != null) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }

        return authorities;
    }
}
