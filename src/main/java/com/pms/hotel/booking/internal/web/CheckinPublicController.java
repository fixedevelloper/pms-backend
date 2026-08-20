package com.pms.hotel.booking.internal.web;

import com.pms.hotel.booking.internal.Booking;
import com.pms.hotel.booking.internal.BookingRoom;
import com.pms.hotel.booking.internal.BookingService;
import com.pms.hotel.booking.internal.web.CheckinPublicRequests.CompleteCheckinRequest;
import com.pms.hotel.booking.internal.web.CheckinPublicRequests.PublicCheckinView;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestProfileUpdate;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Pré-enregistrement en ligne (Phase 3.2) : le client accède à sa réservation
 * via le lien envoyé par e-mail (confirmation/relance pré-arrivée), sans
 * authentification — le jeton opaque {@code Booking#checkinToken} est le seul
 * contrôle d'accès (voir BookingService#findEntityForCheckin). Comme
 * BookingPublicController, protégé par PublicRateLimitFilter plutôt que par
 * l'authentification (voir SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/public/checkin")
@RequiredArgsConstructor
public class CheckinPublicController {

    private final BookingService bookingService;
    private final GuestApi guestApi;
    private final RoomApi roomApi;

    @GetMapping("/{bookingId}")
    public PublicCheckinView show(@PathVariable Long bookingId, @RequestParam String token) {
        Booking booking = bookingService.findEntityForCheckin(bookingId, token);
        GuestSummary guest = guestApi.getById(booking.getGuestId());

        return new PublicCheckinView(
                booking.getId(), booking.getCheckedInAt(), booking.getCheckedOutAt(),
                booking.getRooms().stream().map(BookingRoom::getRoomId).map(id -> roomApi.getById(id).roomNumber()).toList(),
                guest.firstName(), guest.lastName(), guest.email(), guest.phone(),
                guest.dateOfBirth(), guest.nationality(), guest.address(),
                guest.idDocumentType(), guest.idDocumentNumber(), guest.idDocumentExpiry(),
                booking.getOnlineCheckinCompletedAt() != null);
    }

    @PutMapping("/{bookingId}")
    public PublicCheckinView complete(
            @PathVariable Long bookingId, @RequestParam String token, @Valid @RequestBody CompleteCheckinRequest request) {
        Booking booking = bookingService.findEntityForCheckin(bookingId, token);

        guestApi.updateProfile(booking.getGuestId(), new GuestProfileUpdate(
                request.dateOfBirth(), request.nationality(), request.address(),
                request.idDocumentType(), request.idDocumentNumber(), request.idDocumentExpiry()));
        bookingService.completeOnlineCheckin(bookingId, token);

        return show(bookingId, token);
    }

    /** Photo/scan de la pièce d'identité — consultable ensuite par la réception (voir GuestController#downloadDocument). */
    @PostMapping("/{bookingId}/document")
    public void uploadDocument(@PathVariable Long bookingId, @RequestParam String token, @RequestParam("file") MultipartFile file) {
        Booking booking = bookingService.findEntityForCheckin(bookingId, token);
        if (file.isEmpty()) {
            throw new BusinessRuleException("Aucun fichier reçu.");
        }
        try {
            guestApi.attachDocument(booking.getGuestId(), file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new BusinessRuleException("Impossible de lire le fichier envoyé.");
        }
    }
}
