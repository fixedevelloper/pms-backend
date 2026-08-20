package com.pms.hotel.billing.internal;

import com.pms.hotel.billing.internal.BillingViews.BookingBillingView;
import com.pms.hotel.billing.internal.BillingViews.InvoiceItemView;
import com.pms.hotel.billing.internal.BillingViews.InvoiceView;
import com.pms.hotel.billing.internal.BillingViews.LedgerEntry;
import com.pms.hotel.billing.internal.BillingViews.PaymentView;
import com.pms.hotel.billing.internal.BillingViews.BankReconciliationImportResult;
import com.pms.hotel.billing.internal.BillingViews.BankStatementLineView;
import com.pms.hotel.billing.internal.BillingViews.CompanyInvoiceView;
import com.pms.hotel.billing.internal.BillingViews.GroupInvoiceView;
import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingDepositCollectedEvent;
import com.pms.hotel.booking.BookingRoomLine;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.company.CompanyApi;
import com.pms.hotel.company.CompanySummary;
import com.pms.hotel.currency.ExchangeRateApi;
import com.pms.hotel.groupbooking.GroupBookingApi;
import com.pms.hotel.groupbooking.GroupSummary;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.pos.ExtraChargeAddedEvent;
import com.pms.hotel.pos.ExtraChargeLine;
import com.pms.hotel.pos.PosApi;
import com.pms.hotel.settings.SettingsApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final CompanyInvoiceRepository companyInvoiceRepository;
    private final GroupInvoiceRepository groupInvoiceRepository;
    private final BankStatementLineRepository bankStatementLineRepository;
    private final BookingApi bookingApi;
    private final GuestApi guestApi;
    private final PosApi posApi;
    private final SettingsApi settingsApi;
    private final CompanyApi companyApi;
    private final GroupBookingApi groupBookingApi;
    private final ExchangeRateApi exchangeRateApi;

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

    /**
     * Paiement dans une devise différente de la devise de référence de
     * l'établissement : converti au taux en vigueur à cet instant (voir
     * ExchangeRateApi), jamais recalculé ensuite même si le taux configuré
     * change — {@code amount}/{@code currency} du paiement enregistré
     * restent l'équivalent en devise de référence, ce qui compte pour le
     * solde de la facture (comportement de refreshStatus inchangé).
     */
    public PaymentView recordForeignCurrencyPaymentForBooking(
            Long bookingId, BigDecimal tenderedAmount, String tenderedCurrency, String paymentMethod) {
        String referenceCurrency = resolveCurrency();
        if (tenderedCurrency.equalsIgnoreCase(referenceCurrency)) {
            return recordPaymentForBooking(bookingId, tenderedAmount, paymentMethod);
        }

        BigDecimal rate = exchangeRateApi.getRate(tenderedCurrency, referenceCurrency);
        BigDecimal referenceAmount = exchangeRateApi.convert(tenderedAmount, tenderedCurrency, referenceCurrency);

        var invoice = invoiceRepository.findByBookingId(bookingId);
        Payment payment = createCompletedPayment(bookingId, invoice.map(Invoice::getId).orElse(null), referenceAmount, referenceCurrency, paymentMethod);
        payment.setTenderedAmount(tenderedAmount);
        payment.setTenderedCurrency(tenderedCurrency.toUpperCase());
        payment.setExchangeRateUsed(rate);
        payment = paymentRepository.save(payment);

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
     * Agrège en une facture unique les factures individuelles (une par
     * réservation) des séjours d'une société garante sortis sur la période —
     * pour les sociétés en cycle "monthly" (voir Company#billingCycle). Une
     * réservation déjà couverte par une facture société précédente n'est
     * jamais reprise (idempotent d'une exécution à l'autre).
     */
    public CompanyInvoiceView generateCompanyInvoice(Long companyId, LocalDate periodStart, LocalDate periodEnd) {
        CompanySummary company = companyApi.getById(companyId);

        List<Invoice> invoicesToBill = new ArrayList<>();
        for (BookingSummary booking : bookingApi.findByCompanyCheckedOutBetween(companyId, periodStart, periodEnd)) {
            generateInvoice(booking.id()); // idempotent : ne crée que si absente
            Invoice invoice = invoiceRepository.findByBookingId(booking.id())
                    .orElseThrow(() -> ResourceNotFoundException.of("Facture", booking.id()));
            if (invoice.getCompanyInvoice() == null) {
                invoicesToBill.add(invoice);
            }
        }

        if (invoicesToBill.isEmpty()) {
            throw new BusinessRuleException(
                    "Aucune réservation non facturée pour " + company.name() + " sur la période indiquée.");
        }

        BigDecimal total = invoicesToBill.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        CompanyInvoice companyInvoice = new CompanyInvoice();
        companyInvoice.setCompanyId(companyId);
        companyInvoice.setInvoiceNumber(
                "FAC-SOC-" + Year.now() + "-" + String.format("%05d", companyId) + "-" + periodStart.toString().replace("-", ""));
        companyInvoice.setPeriodStart(periodStart);
        companyInvoice.setPeriodEnd(periodEnd);
        companyInvoice.setTotalAmount(total);
        companyInvoice = companyInvoiceRepository.save(companyInvoice);

        for (Invoice invoice : invoicesToBill) {
            invoice.setCompanyInvoice(companyInvoice);
        }
        companyInvoice.getInvoices().addAll(invoicesToBill);

        return toView(companyInvoice, company.name());
    }

    @Transactional(readOnly = true)
    public List<CompanyInvoiceView> listCompanyInvoices(Long companyId) {
        CompanySummary company = companyApi.getById(companyId);
        return companyInvoiceRepository.findByCompanyIdOrderByPeriodStartDesc(companyId).stream()
                .map(ci -> toView(ci, company.name()))
                .toList();
    }

    /**
     * Agrège en une facture unique les factures individuelles de tous les
     * participants d'un groupe — appelle {@link #generateInvoice} pour chacun
     * (idempotent, et qui les marque checked_out au passage, comme pour une
     * facture société — voir generateCompanyInvoice). Une réservation déjà
     * couverte par une facture de groupe précédente n'est jamais reprise.
     */
    public GroupInvoiceView generateGroupInvoice(Long groupId) {
        GroupSummary group = groupBookingApi.getById(groupId);

        List<Invoice> invoicesToBill = new ArrayList<>();
        for (BookingSummary booking : bookingApi.findByGroupId(groupId)) {
            generateInvoice(booking.id()); // idempotent : ne crée que si absente
            Invoice invoice = invoiceRepository.findByBookingId(booking.id())
                    .orElseThrow(() -> ResourceNotFoundException.of("Facture", booking.id()));
            if (invoice.getGroupInvoice() == null) {
                invoicesToBill.add(invoice);
            }
        }

        if (invoicesToBill.isEmpty()) {
            throw new BusinessRuleException("Aucune réservation non facturée pour le groupe " + group.name() + ".");
        }

        BigDecimal total = invoicesToBill.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Le numéro inclut un compteur (pas seulement l'année+id, contrairement à la facture société
        // qui a le periodStart pour se différencier) : un groupe n'a pas de période, rien d'autre
        // ne distinguerait deux factures générées pour le même groupe à des moments différents.
        int sequence = groupInvoiceRepository.findByGroupIdOrderByCreatedAtDesc(groupId).size() + 1;
        GroupInvoice groupInvoice = new GroupInvoice();
        groupInvoice.setGroupId(groupId);
        groupInvoice.setInvoiceNumber("FAC-GRP-" + Year.now() + "-" + String.format("%05d", groupId) + "-" + sequence);
        groupInvoice.setTotalAmount(total);
        groupInvoice = groupInvoiceRepository.save(groupInvoice);

        for (Invoice invoice : invoicesToBill) {
            invoice.setGroupInvoice(groupInvoice);
        }
        groupInvoice.getInvoices().addAll(invoicesToBill);

        return toView(groupInvoice, group.name());
    }

    @Transactional(readOnly = true)
    public List<GroupInvoiceView> listGroupInvoices(Long groupId) {
        GroupSummary group = groupBookingApi.getById(groupId);
        return groupInvoiceRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(gi -> toView(gi, group.name()))
                .toList();
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

    /**
     * Importe un relevé bancaire au format CSV brut (en-tête
     * "date,description,montant", une ligne par transaction) et tente de
     * rapprocher chaque ligne avec un paiement complété, de même montant,
     * daté à ±3 jours et pas déjà rapproché. Les lignes sans correspondance
     * restent {@code unmatched} pour un rapprochement manuel.
     */
    public BankReconciliationImportResult importBankStatement(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new BusinessRuleException("Le relevé importé est vide.");
        }

        int matched = 0;
        int total = 0;
        String[] rows = csv.strip().split("\\r?\\n");
        for (String rawRow : rows) {
            String row = rawRow.strip();
            if (row.isEmpty()) continue;

            String[] cols = row.split(",", 3);
            if (cols.length < 3) continue;

            LocalDate transactionDate;
            try {
                transactionDate = LocalDate.parse(cols[0].strip());
            } catch (java.time.format.DateTimeParseException e) {
                continue; // ligne d'en-tête ou ligne mal formée : ignorée
            }

            BankStatementLine line = new BankStatementLine();
            line.setTransactionDate(transactionDate);
            line.setDescription(cols[1].strip());
            line.setAmount(new BigDecimal(cols[2].strip()));

            List<Payment> candidates = paymentRepository.findUnreconciledCompletedMatchCandidates(
                    line.getAmount(), transactionDate.minusDays(3), transactionDate.plusDays(3));
            if (!candidates.isEmpty()) {
                line.setStatus(BankStatementLine.MATCHED);
                line.setMatchedPaymentId(candidates.get(0).getId());
                matched++;
            }

            bankStatementLineRepository.save(line);
            total++;
        }

        return new BankReconciliationImportResult(total, matched, total - matched);
    }

    @Transactional(readOnly = true)
    public List<BankStatementLineView> listBankStatementLines(String status) {
        List<BankStatementLine> lines = status != null
                ? bankStatementLineRepository.findByStatusOrderByTransactionDateDesc(status)
                : bankStatementLineRepository.findAllByOrderByTransactionDateDesc();
        return lines.stream().map(this::toView).toList();
    }

    /** Rapprochement manuel, pour les lignes que l'appariement automatique n'a pas pu résoudre. */
    public BankStatementLineView matchBankStatementLine(Long lineId, Long paymentId) {
        BankStatementLine line = bankStatementLineRepository.findById(lineId)
                .orElseThrow(() -> ResourceNotFoundException.of("Ligne de relevé", lineId));
        if (!paymentRepository.existsById(paymentId)) {
            throw ResourceNotFoundException.of("Paiement", paymentId);
        }
        line.setMatchedPaymentId(paymentId);
        line.setStatus(BankStatementLine.MATCHED);
        return toView(bankStatementLineRepository.save(line));
    }

    private BankStatementLineView toView(BankStatementLine line) {
        return new BankStatementLineView(
                line.getId(), line.getTransactionDate(), line.getDescription(), line.getAmount(),
                line.getStatus(), line.getMatchedPaymentId(), line.getCreatedAt());
    }

    private CompanyInvoiceView toView(CompanyInvoice companyInvoice, String companyName) {
        List<Long> bookingIds = companyInvoice.getInvoices().stream().map(Invoice::getBookingId).toList();
        return new CompanyInvoiceView(
                companyInvoice.getId(), companyInvoice.getCompanyId(), companyName, companyInvoice.getInvoiceNumber(),
                companyInvoice.getPeriodStart(), companyInvoice.getPeriodEnd(), companyInvoice.getTotalAmount(),
                companyInvoice.getStatus(), bookingIds, companyInvoice.getCreatedAt());
    }

    private GroupInvoiceView toView(GroupInvoice groupInvoice, String groupName) {
        List<Long> bookingIds = groupInvoice.getInvoices().stream().map(Invoice::getBookingId).toList();
        return new GroupInvoiceView(
                groupInvoice.getId(), groupInvoice.getGroupId(), groupName, groupInvoice.getInvoiceNumber(),
                groupInvoice.getTotalAmount(), groupInvoice.getStatus(), bookingIds, groupInvoice.getCreatedAt());
    }

    private PaymentView toView(Payment payment) {
        return new PaymentView(
                payment.getId(), payment.getBookingId(), payment.getInvoiceId(), payment.getAmount(),
                payment.getCurrency(), payment.getPaymentMethod(), payment.getStatus(), payment.getPaidAt(),
                payment.getTenderedAmount(), payment.getTenderedCurrency(), payment.getExchangeRateUsed());
    }
}
