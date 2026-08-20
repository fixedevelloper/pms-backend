package com.pms.hotel.billing.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Facture groupée d'une société garante : agrège les factures individuelles
 * (une par réservation, {@link Invoice}) sorties sur une période donnée en un
 * seul document, pour les entreprises facturées en cycle "monthly" plutôt
 * qu'immédiatement à chaque départ (voir Company#billingCycle).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "company_invoices")
public class CompanyInvoice extends BaseEntity {

    public static final String UNPAID = "unpaid";
    public static final String PAID = "paid";

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = UNPAID;

    @OneToMany(mappedBy = "companyInvoice", cascade = CascadeType.ALL)
    private List<Invoice> invoices = new ArrayList<>();
}
