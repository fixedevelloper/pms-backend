package com.pms.hotel.billing.internal.web;

import com.pms.hotel.billing.internal.BillingService;
import com.pms.hotel.billing.internal.BillingViews.BankReconciliationImportResult;
import com.pms.hotel.billing.internal.BillingViews.BankStatementLineView;
import com.pms.hotel.billing.internal.BillingViews.BookingBillingView;
import com.pms.hotel.billing.internal.BillingViews.CompanyInvoiceView;
import com.pms.hotel.billing.internal.BillingViews.GroupInvoiceView;
import com.pms.hotel.billing.internal.BillingViews.InvoiceView;
import com.pms.hotel.billing.internal.BillingViews.LedgerEntry;
import com.pms.hotel.billing.internal.BillingViews.PaymentView;
import com.pms.hotel.billing.internal.web.BillingRequests.ImportBankStatementRequest;
import com.pms.hotel.billing.internal.web.BillingRequests.RecordForeignCurrencyPaymentRequest;
import com.pms.hotel.billing.internal.web.BillingRequests.RecordPaymentForBookingRequest;
import com.pms.hotel.billing.internal.web.BillingRequests.RecordPaymentRequest;
import com.pms.hotel.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class BillingController {

    private final BillingService billingService;

    @GetMapping("/invoices")
    public PageResponse<InvoiceView> index(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(billingService.listInvoices(Pageable.ofSize(size).withPage(page)));
    }

    @GetMapping("/invoices/{id}")
    public InvoiceView show(@PathVariable Long id) {
        return billingService.getInvoice(id);
    }

    @PostMapping("/bookings/{bookingId}/invoice/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceView generate(@PathVariable Long bookingId) {
        return billingService.generateInvoice(bookingId);
    }

    @GetMapping("/bookings/{bookingId}/billing")
    public BookingBillingView billing(@PathVariable Long bookingId) {
        return billingService.getBookingBillingView(bookingId);
    }

    @GetMapping("/bookings/{bookingId}/ledger")
    public List<LedgerEntry> ledger(@PathVariable Long bookingId) {
        return billingService.guestLedger(bookingId);
    }

    @PostMapping("/companies/{companyId}/invoices/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyInvoiceView generateCompanyInvoice(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return billingService.generateCompanyInvoice(companyId, periodStart, periodEnd);
    }

    @GetMapping("/companies/{companyId}/invoices")
    public List<CompanyInvoiceView> companyInvoices(@PathVariable Long companyId) {
        return billingService.listCompanyInvoices(companyId);
    }

    @PostMapping("/groups/{groupId}/invoice/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupInvoiceView generateGroupInvoice(@PathVariable Long groupId) {
        return billingService.generateGroupInvoice(groupId);
    }

    @GetMapping("/groups/{groupId}/invoices")
    public List<GroupInvoiceView> groupInvoices(@PathVariable Long groupId) {
        return billingService.listGroupInvoices(groupId);
    }

    @PostMapping("/bank-reconciliation/import")
    @ResponseStatus(HttpStatus.CREATED)
    public BankReconciliationImportResult importBankStatement(@Valid @RequestBody ImportBankStatementRequest request) {
        return billingService.importBankStatement(request.csv());
    }

    @GetMapping("/bank-reconciliation")
    public List<BankStatementLineView> bankStatementLines(@RequestParam(required = false) String status) {
        return billingService.listBankStatementLines(status);
    }

    @PostMapping("/bank-reconciliation/{lineId}/match/{paymentId}")
    public BankStatementLineView matchBankStatementLine(@PathVariable Long lineId, @PathVariable Long paymentId) {
        return billingService.matchBankStatementLine(lineId, paymentId);
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentView store(@Valid @RequestBody RecordPaymentRequest request) {
        return billingService.recordPaymentForInvoice(
                request.bookingId(), request.invoiceId(), request.amount(), request.currency(), request.paymentMethod());
    }

    @PostMapping("/bookings/{bookingId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentView storeByBookingId(@PathVariable Long bookingId, @Valid @RequestBody RecordPaymentForBookingRequest request) {
        return billingService.recordPaymentForBooking(bookingId, request.amount(), request.paymentMethod());
    }

    @PostMapping("/bookings/{bookingId}/payments/foreign-currency")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentView storeForeignCurrencyPayment(
            @PathVariable Long bookingId, @Valid @RequestBody RecordForeignCurrencyPaymentRequest request) {
        return billingService.recordForeignCurrencyPaymentForBooking(
                bookingId, request.tenderedAmount(), request.tenderedCurrency(), request.paymentMethod());
    }
}
