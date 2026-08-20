package com.pms.hotel.billing.internal;

import com.pms.hotel.booking.BookingRoomLine;
import com.pms.hotel.pos.ExtraChargeLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BillingViews {

    private BillingViews() {
    }

    public record InvoiceView(
            Long id,
            Long bookingId,
            String invoiceNumber,
            BigDecimal totalRooms,
            BigDecimal totalExtras,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            BigDecimal taxAmount,
            BigDecimal cityTaxAmount,
            BigDecimal discountAmount,
            String status,
            List<InvoiceItemView> items,
            Instant createdAt) {
    }

    public record InvoiceItemView(String description, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
    }

    public record PaymentView(
            Long id,
            Long bookingId,
            Long invoiceId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String status,
            Instant paidAt,
            /** Non-null seulement si payé dans une devise différente de la devise de référence — voir Payment#tenderedAmount. */
            BigDecimal tenderedAmount,
            String tenderedCurrency,
            BigDecimal exchangeRateUsed) {
    }

    public record BookingBillingView(
            Long bookingId,
            String guestFullName,
            String guestEmail,
            List<BookingRoomLine> rooms,
            List<ExtraChargeLine> extraCharges,
            InvoiceView invoice,
            List<PaymentView> payments) {
    }

    /**
     * Une ligne de la main courante (guest ledger) — "room_charge"|"extra_charge"|
     * "tax"|"payment". Les paiements sont négatifs (réduisent le solde dû),
     * runningBalance est le solde cumulé après cette ligne, triée par date.
     */
    public record CompanyInvoiceView(
            Long id,
            Long companyId,
            String companyName,
            String invoiceNumber,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal totalAmount,
            String status,
            List<Long> bookingIds,
            Instant createdAt) {
    }

    public record GroupInvoiceView(
            Long id,
            Long groupId,
            String groupName,
            String invoiceNumber,
            BigDecimal totalAmount,
            String status,
            List<Long> bookingIds,
            Instant createdAt) {
    }

    public record BankStatementLineView(
            Long id,
            LocalDate transactionDate,
            String description,
            BigDecimal amount,
            String status,
            Long matchedPaymentId,
            Instant importedAt) {
    }

    public record BankReconciliationImportResult(int totalLines, int matchedCount, int unmatchedCount) {
    }

    public record LedgerEntry(
            String type,
            String description,
            BigDecimal amount,
            Instant date,
            String roomNumber,
            BigDecimal runningBalance) {
    }
}
