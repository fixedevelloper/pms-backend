package com.pms.hotel.property.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Accorde à un utilisateur (identifiant externe — voir CurrentUser, ce
 * service ne possède aucune table locale de comptes) l'accès à un
 * établissement donné. Non consultée pour un rôle "large" (admin/super-admin,
 * voir CurrentProperty) ni tant qu'un seul établissement actif existe (accès
 * implicite — amorçage mono-établissement).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_property_access")
public class UserPropertyAccess extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;
}
