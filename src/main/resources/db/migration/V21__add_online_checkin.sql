-- =============================================================================
-- V21__add_online_checkin.sql
-- Pre-enregistrement en ligne (Phase 3.2) : jeton opaque par reservation
-- (envoye au client dans ses e-mails de confirmation/relance) et pieces
-- d'identite deposees, stockees en base (pas de service de stockage externe
-- a ce jour).
-- =============================================================================

ALTER TABLE bookings ADD COLUMN checkin_token VARCHAR(64) NULL UNIQUE;
ALTER TABLE bookings ADD COLUMN online_checkin_completed_at DATETIME(6) NULL;

CREATE TABLE guest_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    data LONGBLOB NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_guest_documents_guest FOREIGN KEY (guest_id) REFERENCES guests(id)
) ENGINE=InnoDB;
