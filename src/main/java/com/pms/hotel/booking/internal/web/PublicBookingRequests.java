package com.pms.hotel.booking.internal.web;

import com.pms.hotel.booking.internal.web.BookingRequests.CreateBookingRequest.RoomAllocation;
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

/**
 * DTO propre au Booking Engine public — distinct de {@link BookingRequests.CreateBookingRequest}
 * (utilisé par la réception) : pas de {@code source} (imposé côté serveur à
 * "direct_online", jamais choisi par l'appelant), pas de garantie "company"
 * (réservée au personnel), et un jeton anti-bot en plus.
 */
public final class PublicBookingRequests {

    private PublicBookingRequests() {
    }

    public record CreatePublicBookingRequest(
            /** Requis dès que plusieurs établissements sont actifs (voir GET .../properties) — sinon résolu automatiquement s'il n'y en a qu'un. */
            Long propertyId,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotBlank String phone,
            String passportNumber,
            @NotNull @FutureOrPresent LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Pattern(regexp = "none|credit_card|deposit") String guaranteeType,
            @DecimalMin("0") BigDecimal depositAmount,
            /** Jeton opaque transmis à PaymentGateway#charge — ignoré tant qu'aucune passerelle n'est configurée. */
            String paymentMethodToken,
            @NotEmpty @Valid List<RoomAllocation> rooms,
            @NotNull @DecimalMin("0") BigDecimal totalAmount,
            /** Jeton reCAPTCHA v3 — ignoré si aucune clé secrète n'est configurée côté serveur (voir RecaptchaVerifier). */
            String captchaToken) {
    }
}
