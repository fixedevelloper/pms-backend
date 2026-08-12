package com.pms.hotel.booking.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class BookingRequests {

    private BookingRequests() {
    }

    public record CreateBookingRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotBlank String phone,
            String passportNumber,
            @NotNull @FutureOrPresent LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @NotBlank String source,
            @NotEmpty List<Long> roomIds,
            @NotNull @DecimalMin("0") BigDecimal totalAmount) {
    }

    public record UpdateReservationStatusRequest(
            @NotBlank @Pattern(regexp = "pending|confirmed|checked_in|checked_out|cancelled") String status) {
    }
}
