package com.pms.hotel.billing.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une ligne d'un relevé bancaire importé, rapprochée automatiquement (ou manuellement) avec un {@link Payment} enregistré. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bank_statement_lines")
public class BankStatementLine extends BaseEntity {

    public static final String UNMATCHED = "unmatched";
    public static final String MATCHED = "matched";

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status = UNMATCHED;

    @Column(name = "matched_payment_id")
    private Long matchedPaymentId;
}
