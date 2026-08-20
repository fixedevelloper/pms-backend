package com.pms.hotel.property.internal;

import com.pms.hotel.property.PropertySummary;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un établissement (hôtel) du parc — l'unité de cloisonnement du multi-propriété. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "properties")
public class Property extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** Slug court, unique, stable (jamais affiché aux clients) — utilisé pour identifier la propriété "main" créée par la migration. */
    @Column(nullable = false, unique = true)
    private String code;

    private String address;

    @Column(nullable = false)
    private boolean active = true;

    public PropertySummary toSummary() {
        return new PropertySummary(getId(), name, code, address, active);
    }
}
