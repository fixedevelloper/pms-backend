package com.pms.hotel.booking.internal.web;

import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.booking.internal.Booking;
import com.pms.hotel.booking.internal.BookingCreateCommand;
import com.pms.hotel.booking.internal.BookingRoomRepository;
import com.pms.hotel.booking.internal.BookingService;
import com.pms.hotel.booking.internal.web.PublicBookingRequests.CreatePublicBookingRequest;
import com.pms.hotel.payment.PaymentChargeResult;
import com.pms.hotel.payment.PaymentGateway;
import com.pms.hotel.property.PropertyApi;
import com.pms.hotel.rateplan.RatePlanApi;
import com.pms.hotel.rateplan.RatePlanSummary;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.settings.SettingsApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.security.RecaptchaVerifier;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Façade publique du Booking Engine (site vitrine de l'hôtel) : disponibilité,
 * tarifs et création de réservation, sans authentification (voir
 * SecurityConfig — protégé par PublicRateLimitFilter et RecaptchaVerifier à
 * la place). N'expose jamais de type {@code .internal} d'un autre module
 * (RoomApi/RatePlanApi uniquement) ; réutilise BookingService.create comme le
 * ferait la réception, juste avec une source forcée à "direct_online" et sans
 * garantie "company".
 */
@RestController
@RequestMapping("/api/v1/public/bookings")
@RequiredArgsConstructor
public class BookingPublicController {

    private final BookingService bookingService;
    private final RoomApi roomApi;
    private final RatePlanApi ratePlanApi;
    private final BookingRoomRepository bookingRoomRepository;
    private final RecaptchaVerifier recaptchaVerifier;
    private final Optional<PaymentGateway> paymentGateway;
    private final SettingsApi settingsApi;
    private final PropertyApi propertyApi;

    /** Établissements actifs proposés par le widget public — un sélecteur n'est nécessaire côté frontend que si plus d'un est retourné. */
    @GetMapping("/properties")
    public List<com.pms.hotel.property.PropertySummary> properties() {
        return propertyApi.findAllActive();
    }

    /** Chambres non bloquées et non déjà réservées sur [checkIn, checkOut), pour l'établissement demandé (ou le seul actif). */
    @GetMapping("/availability")
    public List<RoomDetails> checkAvailability(
            @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut, @RequestParam(required = false) Long propertyId) {
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessRuleException("La date de départ doit être postérieure à la date d'arrivée.");
        }
        Long resolvedPropertyId = resolvePublicPropertyId(propertyId);

        Instant checkInInstant = checkIn.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant checkOutInstant = checkOut.atStartOfDay(ZoneOffset.UTC).toInstant();

        return roomApi.findAll().stream()
                .filter(room -> room.propertyId().equals(resolvedPropertyId))
                .filter(room -> !bookingRoomRepository.existsOverlap(room.id(), checkInInstant, checkOutInstant))
                .filter(room -> !roomApi.isBlocked(room.id(), checkIn, checkOut))
                .collect(Collectors.toList());
    }

    /** Tarifs actifs d'un type de chambre — jamais un tarif désactivé (voir RatePlanApi#listActive). */
    @GetMapping("/rate-plans")
    public List<RatePlanSummary> getAvailableRatePlans(@RequestParam Long roomTypeId) {
        return ratePlanApi.listActive(roomTypeId);
    }

    /** Ce que le widget doit savoir avant d'afficher les options de garantie (acompte en ligne, CAPTCHA). */
    @GetMapping("/capabilities")
    public PublicBookingCapabilities capabilities() {
        return new PublicBookingCapabilities(paymentGateway.isPresent(), recaptchaVerifier.isEnabled());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingSummary book(@Valid @RequestBody CreatePublicBookingRequest request) {
        if (!recaptchaVerifier.verify(request.captchaToken())) {
            throw new BusinessRuleException("Vérification anti-robot invalide. Merci de réessayer.");
        }
        Long resolvedPropertyId = resolvePublicPropertyId(request.propertyId());

        String guaranteeType = request.guaranteeType() != null ? request.guaranteeType() : Booking.GUARANTEE_NONE;
        BigDecimal depositAmount = request.depositAmount() != null ? request.depositAmount() : BigDecimal.ZERO;

        if (Booking.GUARANTEE_DEPOSIT.equals(guaranteeType) && depositAmount.signum() > 0) {
            chargeDeposit(depositAmount, request.paymentMethodToken());
        }

        BookingCreateCommand command = new BookingCreateCommand(
                resolvedPropertyId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.passportNumber(),
                request.checkIn().atStartOfDay().toInstant(ZoneOffset.UTC),
                request.checkOut().atStartOfDay().toInstant(ZoneOffset.UTC),
                "direct_online",
                guaranteeType,
                null, // pas de société garante pour une réservation directe en ligne
                null, // pas de groupe pour une réservation directe en ligne
                depositAmount,
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

    /**
     * Capture réellement l'acompte avant de créer la réservation — jamais de
     * "succès" simulé : tant qu'aucun PaymentGateway n'est configuré, la
     * garantie "deposit" est simplement indisponible en ligne (voir
     * PaymentGateway, javadoc).
     */
    private void chargeDeposit(BigDecimal amount, String paymentMethodToken) {
        if (paymentGateway.isEmpty()) {
            throw new BusinessRuleException(
                    "Le paiement en ligne n'est pas encore configuré sur cet établissement. "
                            + "Choisissez une garantie par carte enregistrée ou contactez la réception.");
        }
        String currency = settingsApi.get("currency").filter(c -> !c.isBlank()).orElse("XAF");
        PaymentChargeResult result = paymentGateway.get().charge(amount, currency, paymentMethodToken, "Acompte réservation en ligne");
        if (!result.success()) {
            throw new BusinessRuleException("Le paiement de l'acompte a échoué : " + result.message());
        }
    }

    /**
     * Résout l'établissement d'une requête publique (jamais authentifiée —
     * CurrentProperty, qui suppose un utilisateur JWT, est inutilisable ici) :
     * l'id fourni s'il est bien actif, sinon le seul établissement actif s'il
     * n'y en a qu'un. Avec plusieurs établissements, l'appelant doit préciser
     * lequel (voir GET .../properties, à interroger avant .../availability).
     */
    private Long resolvePublicPropertyId(Long requestedPropertyId) {
        List<Long> active = propertyApi.findAllActivePropertyIds();
        if (requestedPropertyId != null) {
            if (!active.contains(requestedPropertyId)) {
                throw new BusinessRuleException("Établissement introuvable ou inactif.");
            }
            return requestedPropertyId;
        }
        if (active.size() == 1) {
            return active.get(0);
        }
        if (active.isEmpty()) {
            throw new BusinessRuleException("Aucun établissement n'est configuré.");
        }
        throw new BusinessRuleException("Plusieurs établissements sont proposés — précisez propertyId (voir GET .../properties).");
    }

    public record PublicBookingCapabilities(boolean onlineDepositEnabled, boolean captchaEnabled) {
    }
}
