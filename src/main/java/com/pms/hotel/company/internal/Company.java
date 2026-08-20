package com.pms.hotel.company.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    public static final String IMMEDIATE = "immediate";
    public static final String MONTHLY = "monthly";

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column
    private String contactEmail;

    @Column
    private String phoneNumber;

    @Column(nullable = false)
    private boolean active = true;

    /** Tarif appliqué par défaut aux réservations garanties par cette société — null si aucun tarif négocié. */
    @Column(name = "negotiated_rate_plan_id")
    private Long negotiatedRatePlanId;

    /** "immediate" (facturée comme toute autre réservation) ou "monthly" (agrégée dans une facture société périodique). */
    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle = IMMEDIATE;

    public com.pms.hotel.company.CompanySummary toSummary() {
        return new com.pms.hotel.company.CompanySummary(
                getId(), name, address, contactEmail, phoneNumber, active, negotiatedRatePlanId, billingCycle);
    }
}
