package com.pms.hotel.booking.internal.web;

import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.booking.internal.Booking;
import com.pms.hotel.booking.internal.BookingCreateCommand;
import com.pms.hotel.booking.internal.BookingRepository;
import com.pms.hotel.booking.internal.DailyFlux;
import com.pms.hotel.booking.internal.BookingService;
import com.pms.hotel.booking.internal.web.BookingRequests.CreateBookingRequest;
import com.pms.hotel.booking.internal.web.BookingRequests.UpdateReservationStatusRequest;
import com.pms.hotel.property.CurrentProperty;
import com.pms.hotel.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
class BookingController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final CurrentProperty currentProperty;

    @GetMapping("/bookings")
    public PageResponse<BookingSummary> index(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long guestId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return PageResponse.of(bookingRepository.search(currentProperty.resolve(), status, guestId, groupId, pageable), bookingService::toSummary);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingSummary store(@Valid @RequestBody CreateBookingRequest request) {
        BookingCreateCommand command = new BookingCreateCommand(
                currentProperty.resolve(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.passportNumber(),
                request.checkIn().atStartOfDay().toInstant(ZoneOffset.UTC),
                request.checkOut().atStartOfDay().toInstant(ZoneOffset.UTC),
                request.source(),
                request.guaranteeType() != null ? request.guaranteeType() : Booking.GUARANTEE_NONE,
                request.companyId(),
                request.groupId(),
                request.depositAmount() != null ? request.depositAmount() : BigDecimal.ZERO,
                request.rooms().stream()
                        .map(r -> new BookingCreateCommand.RoomAllocation(
                                r.roomId(), r.ratePlanId(),
                                r.adultsCount() != null ? r.adultsCount() : 1,
                                r.childrenCount() != null ? r.childrenCount() : 0,
                                r.occupants() != null
                                        ? r.occupants().stream()
                                                .map(o -> new BookingCreateCommand.OccupantInput(o.firstName(), o.lastName(), o.passportNumber()))
                                                .toList()
                                        : List.of()))
                        .toList(),
                request.totalAmount());
        return bookingService.create(command);
    }

    @GetMapping("/bookings/{id}")
    public BookingSummary show(@PathVariable Long id) {
        return bookingService.toSummary(bookingService.findEntity(id));
    }

    @PatchMapping("/reception/bookings/{id}/status")
    public BookingSummary updateReservationStatus(@PathVariable Long id, @Valid @RequestBody UpdateReservationStatusRequest request) {
        return bookingService.updateReservationStatus(id, request.status());
    }

    @GetMapping("/reception/daily-flux")
    public DailyFlux dailyFlux(@RequestParam(required = false) LocalDate date) {
        return bookingService.dailyFlux(date != null ? date : LocalDate.now());
    }
}
