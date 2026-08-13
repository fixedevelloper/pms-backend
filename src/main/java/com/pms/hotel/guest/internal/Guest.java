package com.pms.hotel.guest.internal;

import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guests")
public class Guest extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(name = "passport_number")
    private String passportNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String nationality;

    private String address;

    /** "passport" | "national_id" | "driving_license" | "other" — voir GuestRequests pour la validation. */
    @Column(name = "id_document_type")
    private String idDocumentType;

    @Column(name = "id_document_number")
    private String idDocumentNumber;

    @Column(name = "id_document_expiry")
    private LocalDate idDocumentExpiry;

    @Column(name = "preferred_floor")
    private String preferredFloor;

    @Column(name = "preferred_bedding")
    private String preferredBedding;

    private String allergies;

    @Column(nullable = false)
    private boolean vip = false;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent = false;

    @Column(nullable = false)
    private boolean blacklisted = false;

    @Column(name = "blacklist_reason")
    private String blacklistReason;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public GuestSummary toSummary() {
        return new GuestSummary(
                getId(), firstName, lastName, getFullName(), email, phone, passportNumber,
                dateOfBirth, nationality, address,
                idDocumentType, idDocumentNumber, idDocumentExpiry,
                preferredFloor, preferredBedding, allergies,
                vip, internalNotes, marketingConsent, blacklisted, blacklistReason);
    }
}
