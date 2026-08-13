package com.pms.hotel.billing.internal;

import com.pms.hotel.billing.internal.BillingViews.BookingBillingView;
import com.pms.hotel.billing.internal.BillingViews.InvoiceItemView;
import com.pms.hotel.billing.internal.BillingViews.InvoiceView;
import com.pms.hotel.billing.internal.BillingViews.LedgerEntry;
import com.pms.hotel.billing.internal.BillingViews.PaymentView;
import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingDepositCollectedEvent;
import com.pms.hotel.booking.BookingRoomLine;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.pos.ExtraChargeAddedEvent;
import com.pms.hotel.pos.ExtraChargeLine;
import com.pms.hotel.pos.PosApi;
import com.pms.hotel.settings.SettingsApi;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final SettingsApi settingsApi;

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

        BigDecimal vatPercent = settingsApi.get("tva_default").map(BigDecimal::new).orElse(BigDecimal.ZERO);
        BigDecimal taxAmount = totalRooms.add(totalExtras)
                .multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal cityTaxAmount = computeCityTax(booking);

        BigDecimal totalAmount = totalRooms.add(totalExtras).add(taxAmount).add(cityTaxAmount);
        BigDecimal amountPaid = paymentsCompletedTotal(bookingId);

        Invoice invoice = new Invoice();
        invoice.setBookingId(bookingId);
        invoice.setInvoiceNumber("FAC-" + Year.now() + "-" + String.format("%05d", bookingId));
        invoice.setTotalRooms(totalRooms);
        invoice.setTotalExtras(totalExtras);
        invoice.setTaxAmount(taxAmount);
        invoice.setCityTaxAmount(cityTaxAmount);
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
        Payment payment = createCompletedPayment(bookingId, invoice.map(Invoice::getId).orElse(null), amount, resolveCurrency(), paymentMethod);
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

    /**
     * Main courante d'une réservation : chaque mouvement financier daté et
     * trié chronologiquement (nuitées, extras, taxes, paiements), avec un
     * solde cumulé. Les paiements sont négatifs (ils réduisent le solde dû).
     */
    @Transactional(readOnly = true)
    public List<LedgerEntry> guestLedger(Long bookingId) {
        BookingSummary booking = bookingApi.getById(bookingId);
        long nights = Math.max(1, ChronoUnit.DAYS.between(booking.checkedInAt(), booking.checkedOutAt()));

        List<LedgerEntry> entries = new ArrayList<>();
        for (BookingRoomLine room : booking.rooms()) {
            BigDecimal amount = room.pricePerNight().multiply(BigDecimal.valueOf(nights));
            entries.add(new LedgerEntry(
                    "room_charge", "Hébergement — Chambre " + room.roomNumber() + " (" + nights + " nuit(s))",
                    amount, booking.checkedInAt(), room.roomNumber(), null));
        }

        for (ExtraChargeLine extra : posApi.getChargesForBooking(bookingId)) {
            entries.add(new LedgerEntry(
                    "extra_charge", extra.department() + " — " + extra.itemName(),
                    extra.totalPrice(), extra.createdAt(), null, null));
        }

        invoiceRepository.findByBookingId(bookingId).ifPresent(invoice -> {
            if (invoice.getTaxAmount().signum() > 0) {
                entries.add(new LedgerEntry("tax", "TVA", invoice.getTaxAmount(), invoice.getCreatedAt(), null, null));
            }
            if (invoice.getCityTaxAmount().signum() > 0) {
                entries.add(new LedgerEntry("tax", "Taxe de séjour", invoice.getCityTaxAmount(), invoice.getCreatedAt(), null, null));
            }
        });

        for (Payment payment : paymentRepository.findByBookingId(bookingId)) {
            if (Payment.COMPLETED.equals(payment.getStatus())) {
                entries.add(new LedgerEntry(
                        "payment", "Paiement (" + payment.getPaymentMethod() + ")",
                        payment.getAmount().negate(), payment.getPaidAt(), null, null));
            }
        }

        entries.sort(Comparator.comparing(LedgerEntry::date));

        List<LedgerEntry> withBalance = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            balance = balance.add(entry.amount());
            withBalance.add(new LedgerEntry(entry.type(), entry.description(), entry.amount(), entry.date(), entry.roomNumber(), balance));
        }
        return withBalance;
    }

    @ApplicationModuleListener
    void onExtraChargeAdded(ExtraChargeAddedEvent event) {
        invoiceRepository.findByBookingId(event.bookingId()).ifPresent(invoice -> {
            invoice.setTotalExtras(invoice.getTotalExtras().add(event.amount()));
            invoice.setTotalAmount(invoice.getTotalAmount().add(event.amount()));
        });
    }

    /**
     * Un dépôt saisi à la création d'une réservation est un vrai paiement,
     * pas juste un chiffre stocké sur Booking — enregistré ici (pas
     * directement depuis booking.BookingService, qui créerait une dépendance
     * cyclique billing<->booking) pour apparaître dans BookingBillingView#payments
     * comme n'importe quel autre encaissement.
     */
    @ApplicationModuleListener
    void onDepositCollected(BookingDepositCollectedEvent event) {
        createCompletedPayment(event.bookingId(), null, event.amount(), resolveCurrency(), "deposit");
    }

    /** Devise configurée pour l'hôtel (réglage "currency") — "XAF" si jamais configurée. */
    private String resolveCurrency() {
        return settingsApi.get("currency").filter(c -> !c.isBlank()).orElse("XAF");
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

    /**
     * Taxe de séjour : tarif × nuits × adultes, par chambre, enfants exonérés.
     * Réglage {@code city_tax_per_night_per_adult}, absent par défaut (0 partout).
     */
    private BigDecimal computeCityTax(BookingSummary booking) {
        BigDecimal ratePerNightPerAdult = settingsApi.get("city_tax_per_night_per_adult")
                .map(BigDecimal::new).orElse(BigDecimal.ZERO);
        if (ratePerNightPerAdult.signum() == 0) {
            return BigDecimal.ZERO;
        }

        long nights = Math.max(1, ChronoUnit.DAYS.between(booking.checkedInAt(), booking.checkedOutAt()));
        int totalAdults = booking.rooms().stream()
                .mapToInt(room -> room.adultsCount() != null ? room.adultsCount() : 1)
                .sum();

        return ratePerNightPerAdult.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(totalAdults));
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
                invoice.getTaxAmount(), invoice.getCityTaxAmount(), invoice.getDiscountAmount(), invoice.getStatus(), items, invoice.getCreatedAt());
    }

    private PaymentView toView(Payment payment) {
        return new PaymentView(
                payment.getId(), payment.getBookingId(), payment.getInvoiceId(), payment.getAmount(),
                payment.getCurrency(), payment.getPaymentMethod(), payment.getStatus(), payment.getPaidAt());
    }
}
