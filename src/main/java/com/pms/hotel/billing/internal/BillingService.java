package com.pms.hotel.billing.internal;

import com.pms.hotel.billing.internal.BillingViews.BookingBillingView;
import com.pms.hotel.billing.internal.BillingViews.InvoiceItemView;
import com.pms.hotel.billing.internal.BillingViews.InvoiceView;
import com.pms.hotel.billing.internal.BillingViews.PaymentView;
import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.pos.ExtraChargeAddedEvent;
import com.pms.hotel.pos.PosApi;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final BookingApi bookingApi;
    private final GuestApi guestApi;
    private final PosApi posApi;

    /** Mirrors {@code InvoiceController::generate}: idempotent, computes totals from the booking + outstanding extras. */
    public InvoiceView generateInvoice(Long bookingId) {
        var existing = invoiceRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            return toView(existing.get());
        }

        BookingSummary booking = bookingApi.getById(bookingId);

        BigDecimal totalRooms = booking.rooms().stream()
                .map(com.pms.hotel.booking.BookingRoomLine::pricePerNight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExtras = posApi.getOutstandingTotalForBooking(bookingId);
        BigDecimal totalAmount = totalRooms.add(totalExtras);
        BigDecimal amountPaid = paymentsCompletedTotal(bookingId);

        Invoice invoice = new Invoice();
        invoice.setBookingId(bookingId);
        invoice.setInvoiceNumber("FAC-" + Year.now() + "-" + String.format("%05d", bookingId));
        invoice.setTotalRooms(totalRooms);
        invoice.setTotalExtras(totalExtras);
        invoice.setTotalAmount(totalAmount);
        invoice.refreshStatus(amountPaid);

        for (var room : booking.rooms()) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setDescription("Nuitée: " + room.roomNumber());
            item.setQuantity(1);
            item.setUnitPrice(room.pricePerNight());
            item.setTotalPrice(room.pricePerNight());
            invoice.getItems().add(item);
        }

        invoice = invoiceRepository.save(invoice);
        bookingApi.markCheckedOut(bookingId);

        return toView(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceView> listInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable).map(this::toView);
    }

    @Transactional(readOnly = true)
    public InvoiceView getInvoice(Long id) {
        return toView(findInvoice(id));
    }

    /** Mirrors {@code PaymentController::store}: a payment always references an existing invoice. */
    public PaymentView recordPaymentForInvoice(Long bookingId, Long invoiceId, BigDecimal amount, String currency, String paymentMethod) {
        Invoice invoice = findInvoice(invoiceId);
        Payment payment = createCompletedPayment(bookingId, invoiceId, amount, currency, paymentMethod);
        invoice.refreshStatus(paymentsCompletedTotalForInvoice(invoiceId));
        return toView(payment);
    }

    /** Mirrors {@code PaymentController::storeBYID}: payment recorded against a booking, invoice refreshed if it exists. */
    public PaymentView recordPaymentForBooking(Long bookingId, BigDecimal amount, String paymentMethod) {
        var invoice = invoiceRepository.findByBookingId(bookingId);
        Payment payment = createCompletedPayment(bookingId, invoice.map(Invoice::getId).orElse(null), amount, "XAF", paymentMethod);
        invoice.ifPresent(inv -> inv.refreshStatus(paymentsCompletedTotalForInvoice(inv.getId())));
        return toView(payment);
    }

    @Transactional(readOnly = true)
    public BookingBillingView getBookingBillingView(Long bookingId) {
        BookingSummary booking = bookingApi.getById(bookingId);
        GuestSummary guest = guestApi.getById(booking.guestId());
        InvoiceView invoice = invoiceRepository.findByBookingId(bookingId).map(this::toView).orElse(null);
        var payments = paymentRepository.findByBookingId(bookingId).stream().map(this::toView).toList();

        return new BookingBillingView(
                bookingId, guest.fullName(), guest.email(), booking.rooms(),
                posApi.getChargesForBooking(bookingId), invoice, payments);
    }

    @ApplicationModuleListener
    void onExtraChargeAdded(ExtraChargeAddedEvent event) {
        invoiceRepository.findByBookingId(event.bookingId()).ifPresent(invoice -> {
            invoice.setTotalExtras(invoice.getTotalExtras().add(event.amount()));
            invoice.setTotalAmount(invoice.getTotalAmount().add(event.amount()));
        });
    }

    private Payment createCompletedPayment(Long bookingId, Long invoiceId, BigDecimal amount, String currency, String paymentMethod) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setInvoiceId(invoiceId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(Payment.COMPLETED);
        payment.setPaidAt(java.time.Instant.now());
        return paymentRepository.save(payment);
    }

    private BigDecimal paymentsCompletedTotal(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream()
                .filter(p -> Payment.COMPLETED.equals(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal paymentsCompletedTotalForInvoice(Long invoiceId) {
        return paymentRepository.sumCompletedForInvoice(invoiceId);
    }

    private Invoice findInvoice(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Facture", id));
    }

    private InvoiceView toView(Invoice invoice) {
        var items = invoice.getItems().stream()
                .map(i -> new InvoiceItemView(i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getTotalPrice()))
                .toList();
        return new InvoiceView(
                invoice.getId(), invoice.getBookingId(), invoice.getInvoiceNumber(),
                invoice.getTotalRooms(), invoice.getTotalExtras(), invoice.getTotalAmount(), invoice.getAmountPaid(),
                invoice.getTaxAmount(), invoice.getDiscountAmount(), invoice.getStatus(), items, invoice.getCreatedAt());
    }

    private PaymentView toView(Payment payment) {
        return new PaymentView(
                payment.getId(), payment.getBookingId(), payment.getInvoiceId(), payment.getAmount(),
                payment.getCurrency(), payment.getPaymentMethod(), payment.getStatus(), payment.getPaidAt());
    }
}
