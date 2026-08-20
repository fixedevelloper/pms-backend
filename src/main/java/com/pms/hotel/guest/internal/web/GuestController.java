package com.pms.hotel.guest.internal.web;

import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.guest.internal.Guest;
import com.pms.hotel.guest.internal.GuestDocument;
import com.pms.hotel.guest.internal.GuestDocumentRepository;
import com.pms.hotel.guest.internal.GuestRepository;
import com.pms.hotel.guest.internal.web.GuestRequests.CreateGuestRequest;
import com.pms.hotel.guest.internal.web.GuestRequests.UpdateGuestRequest;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import com.pms.hotel.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guests")
@RequiredArgsConstructor
class GuestController {

    private final GuestRepository guestRepository;
    private final GuestDocumentRepository guestDocumentRepository;

    @GetMapping
    public PageResponse<GuestSummary> index(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return PageResponse.of(guestRepository.search(search, pageable), Guest::toSummary);
    }

    @GetMapping("/search")
    public List<GuestSummary> quickSearch(@RequestParam("q") String query) {
        Pageable topTen = PageRequest.of(0, 10, Sort.by("lastName"));
        return guestRepository.search(query, topTen).map(Guest::toSummary).getContent();
    }

    @GetMapping("/{id}")
    public GuestSummary show(@PathVariable Long id) {
        return findEntity(id).toSummary();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuestSummary store(@Valid @RequestBody CreateGuestRequest request) {
        Guest guest = new Guest();
        guest.setFirstName(request.firstName());
        guest.setLastName(request.lastName());
        guest.setEmail(request.email());
        guest.setPhone(request.phone());
        guest.setPassportNumber(request.passportNumber());
        guest.setDateOfBirth(request.dateOfBirth());
        guest.setNationality(request.nationality());
        guest.setAddress(request.address());
        guest.setIdDocumentType(request.idDocumentType());
        guest.setIdDocumentNumber(request.idDocumentNumber());
        guest.setIdDocumentExpiry(request.idDocumentExpiry());
        guest.setPreferredFloor(request.preferredFloor());
        guest.setPreferredBedding(request.preferredBedding());
        guest.setAllergies(request.allergies());
        guest.setVip(request.vip());
        guest.setInternalNotes(request.internalNotes());
        guest.setMarketingConsent(request.marketingConsent());
        guest.setBlacklisted(request.blacklisted());
        guest.setBlacklistReason(request.blacklistReason());
        return guestRepository.save(guest).toSummary();
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuestSummary> update(@PathVariable Long id, @Valid @RequestBody UpdateGuestRequest request) {
        Guest guest = findEntity(id);
        if (request.firstName() != null) guest.setFirstName(request.firstName());
        if (request.lastName() != null) guest.setLastName(request.lastName());
        if (request.email() != null) guest.setEmail(request.email());
        if (request.phone() != null) guest.setPhone(request.phone());
        if (request.passportNumber() != null) guest.setPassportNumber(request.passportNumber());
        if (request.dateOfBirth() != null) guest.setDateOfBirth(request.dateOfBirth());
        if (request.nationality() != null) guest.setNationality(request.nationality());
        if (request.address() != null) guest.setAddress(request.address());
        if (request.idDocumentType() != null) guest.setIdDocumentType(request.idDocumentType());
        if (request.idDocumentNumber() != null) guest.setIdDocumentNumber(request.idDocumentNumber());
        if (request.idDocumentExpiry() != null) guest.setIdDocumentExpiry(request.idDocumentExpiry());
        if (request.preferredFloor() != null) guest.setPreferredFloor(request.preferredFloor());
        if (request.preferredBedding() != null) guest.setPreferredBedding(request.preferredBedding());
        if (request.allergies() != null) guest.setAllergies(request.allergies());
        if (request.vip() != null) guest.setVip(request.vip());
        if (request.internalNotes() != null) guest.setInternalNotes(request.internalNotes());
        if (request.marketingConsent() != null) guest.setMarketingConsent(request.marketingConsent());
        if (request.blacklisted() != null) guest.setBlacklisted(request.blacklisted());
        if (request.blacklistReason() != null) guest.setBlacklistReason(request.blacklistReason());
        return ResponseEntity.ok(guestRepository.save(guest).toSummary());
    }

    /** Pièces d'identité déposées par ce client (dépôt en ligne lors du pré-enregistrement — voir CheckinPublicController) — métadonnées seulement, pas les octets. */
    @GetMapping("/{id}/documents")
    public List<com.pms.hotel.guest.GuestDocumentInfo> documents(@PathVariable Long id) {
        return guestDocumentRepository.findByGuestIdOrderByCreatedAtDesc(id).stream().map(GuestDocument::toInfo).toList();
    }

    @GetMapping("/{id}/documents/{documentId}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, @PathVariable Long documentId) {
        GuestDocument document = guestDocumentRepository.findById(documentId)
                .filter(d -> d.getGuestId().equals(id))
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .body(document.getData());
    }

    private Guest findEntity(Long id) {
        return guestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Client", id));
    }
}
