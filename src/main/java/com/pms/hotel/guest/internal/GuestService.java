package com.pms.hotel.guest.internal;

import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestDocumentInfo;
import com.pms.hotel.guest.GuestProfileUpdate;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.guest.GuestUpsertRequest;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
class GuestService implements GuestApi {

    private final GuestRepository guestRepository;
    private final GuestDocumentRepository guestDocumentRepository;

    @Override
    @Transactional(readOnly = true)
    public GuestSummary getById(Long id) {
        return findEntity(id).toSummary();
    }

    @Override
    public GuestSummary findOrCreateByPhone(GuestUpsertRequest request) {
        Guest guest = guestRepository.findByPhone(request.phone()).orElseGet(Guest::new);
        applyUpsert(guest, request);
        guest.setPhone(request.phone());
        return guestRepository.save(guest).toSummary();
    }

    @Override
    public GuestSummary findOrCreateByEmail(GuestUpsertRequest request) {
        Guest guest = guestRepository.findByEmail(request.email()).orElseGet(Guest::new);
        applyUpsert(guest, request);
        guest.setEmail(request.email());
        return guestRepository.save(guest).toSummary();
    }

    @Override
    public GuestSummary updateProfile(Long guestId, GuestProfileUpdate update) {
        Guest guest = findEntity(guestId);
        if (update.dateOfBirth() != null) guest.setDateOfBirth(update.dateOfBirth());
        if (update.nationality() != null) guest.setNationality(update.nationality());
        if (update.address() != null) guest.setAddress(update.address());
        if (update.idDocumentType() != null) guest.setIdDocumentType(update.idDocumentType());
        if (update.idDocumentNumber() != null) guest.setIdDocumentNumber(update.idDocumentNumber());
        if (update.idDocumentExpiry() != null) guest.setIdDocumentExpiry(update.idDocumentExpiry());
        return guestRepository.save(guest).toSummary();
    }

    @Override
    public GuestDocumentInfo attachDocument(Long guestId, String fileName, String contentType, byte[] data) {
        findEntity(guestId); // 404 si le client n'existe pas
        GuestDocument document = new GuestDocument();
        document.setGuestId(guestId);
        document.setFileName(fileName);
        document.setContentType(contentType);
        document.setData(data);
        return guestDocumentRepository.save(document).toInfo();
    }

    private void applyUpsert(Guest guest, GuestUpsertRequest request) {
        guest.setFirstName(request.firstName());
        guest.setLastName(request.lastName());
        if (request.email() != null) {
            guest.setEmail(request.email());
        }
        if (request.phone() != null) {
            guest.setPhone(request.phone());
        }
        if (request.passportNumber() != null) {
            guest.setPassportNumber(request.passportNumber());
        }
    }

    Guest findEntity(Long id) {
        return guestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Client", id));
    }
}
