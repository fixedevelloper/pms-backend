package com.pms.hotel.billing.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Facture groupée d'un groupe/allotement : agrège en un seul document les
 * factures individuelles (une par réservation, {@link Invoice}) de tous les
 * participants — pas de notion de période contrairement à {@link CompanyInvoice},
 * un groupe est un événement ponctuel, pas un compte facturé récurrent.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "group_invoices")
public class GroupInvoice extends BaseEntity {

    public static final String UNPAID = "unpaid";
    public static final String PAID = "paid";

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = UNPAID;

    @OneToMany(mappedBy = "groupInvoice", cascade = CascadeType.ALL)
    private List<Invoice> invoices = new ArrayList<>();
}
