package com.pms.hotel.guest.internal;

import com.pms.hotel.guest.GuestDocumentInfo;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pièce d'identité déposée par le client — typiquement lors du pré-enregistrement en ligne, stockée en base (pas de service de stockage externe à ce jour). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guest_documents")
public class GuestDocument extends BaseEntity {

    @Column(name = "guest_id", nullable = false)
    private Long guestId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    /** columnDefinition explicite : sans lui, Hibernate valide un byte[] @Lob contre TINYBLOB (trop petit pour une photo de pièce d'identité) alors que la migration crée bien un LONGBLOB. */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    public GuestDocumentInfo toInfo() {
        return new GuestDocumentInfo(getId(), guestId, fileName, contentType, getCreatedAt());
    }
}
