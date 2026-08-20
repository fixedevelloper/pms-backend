package com.pms.hotel.billing.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class BillingRequests {

    private BillingRequests() {
    }

    public record RecordPaymentRequest(
            @NotNull Long bookingId,
            @NotNull Long invoiceId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Size(max = 3) String currency,
            @NotBlank @Pattern(regexp = "credit_card|cash|bank_transfer|mobile_money|stripe|paypal") String paymentMethod) {
    }

    public record RecordPaymentForBookingRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "credit_card|cash|bank_transfer|mobile_money|stripe|paypal") String paymentMethod) {
    }

    public record RecordForeignCurrencyPaymentRequest(
            @NotNull @DecimalMin("0.01") BigDecimal tenderedAmount,
            @NotBlank @Size(min = 3, max = 3) String tenderedCurrency,
            @NotBlank @Pattern(regexp = "credit_card|cash|bank_transfer|mobile_money|stripe|paypal") String paymentMethod) {
    }

    /** csv : en-tête "date,description,montant" (une transaction par ligne, date ISO). */
    public record ImportBankStatementRequest(@NotBlank String csv) {
    }
}
