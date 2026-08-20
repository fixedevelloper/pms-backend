package com.pms.hotel.booking.internal.web;

import jakarta.validation.Valid;
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
            @Pattern(regexp = "none|credit_card|deposit|company") String guaranteeType,
            Long companyId,
            Long groupId,
            @DecimalMin("0") BigDecimal depositAmount,
            @NotEmpty @Valid List<RoomAllocation> rooms,
            @NotNull @DecimalMin("0") BigDecimal totalAmount) {

        public record RoomAllocation(
                @NotNull Long roomId,
                @NotNull Long ratePlanId,
                @jakarta.validation.constraints.Min(1) Integer adultsCount,
                @jakarta.validation.constraints.Min(0) Integer childrenCount,
                @Valid List<OccupantRequest> occupants) {
        }

        public record OccupantRequest(
                @NotBlank String firstName,
                @NotBlank String lastName,
                String passportNumber) {
        }
    }

    public record UpdateReservationStatusRequest(
            @NotBlank @Pattern(regexp = "pending|confirmed|checked_in|checked_out|cancelled") String status) {
    }
}
