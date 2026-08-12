package com.pms.hotel.billing.internal.web;

import com.pms.hotel.billing.internal.BillingService;
import com.pms.hotel.billing.internal.BillingViews.BookingBillingView;
import com.pms.hotel.billing.internal.BillingViews.InvoiceView;
import com.pms.hotel.billing.internal.BillingViews.PaymentView;
import com.pms.hotel.billing.internal.web.BillingRequests.RecordPaymentForBookingRequest;
import com.pms.hotel.billing.internal.web.BillingRequests.RecordPaymentRequest;
import com.pms.hotel.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
}
