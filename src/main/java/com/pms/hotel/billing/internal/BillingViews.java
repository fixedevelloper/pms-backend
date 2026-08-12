package com.pms.hotel.billing.internal;

import com.pms.hotel.booking.BookingRoomLine;
import com.pms.hotel.pos.ExtraChargeLine;
import java.math.BigDecimal;
import java.time.Instant;
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
            Instant paidAt) {
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
}
