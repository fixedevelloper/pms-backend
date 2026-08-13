-- =============================================================================
-- V6__add_guest_profile_fields.sql
-- Guest ne portait que 5 champs (nom, prenom, email, telephone, passeport).
-- Rien sur l'identite reglementaire (obligatoire a l'enregistrement dans la
-- plupart des pays), les preferences, le statut VIP, les notes internes, le
-- consentement marketing (RGPD) ou la liste noire.
-- =============================================================================

ALTER TABLE guests
    ADD COLUMN date_of_birth       DATE          NULL AFTER passport_number,
    ADD COLUMN nationality         VARCHAR(100)  NULL AFTER date_of_birth,
    ADD COLUMN address             VARCHAR(500)  NULL AFTER nationality,
    ADD COLUMN id_document_type    VARCHAR(30)   NULL AFTER address,
    ADD COLUMN id_document_number  VARCHAR(100)  NULL AFTER id_document_type,
    ADD COLUMN id_document_expiry  DATE          NULL AFTER id_document_number,
    ADD COLUMN preferred_floor     VARCHAR(50)   NULL AFTER id_document_expiry,
    ADD COLUMN preferred_bedding   VARCHAR(100)  NULL AFTER preferred_floor,
    ADD COLUMN allergies           VARCHAR(500)  NULL AFTER preferred_bedding,
    ADD COLUMN vip                 BOOLEAN       NOT NULL DEFAULT FALSE AFTER allergies,
    ADD COLUMN internal_notes      TEXT          NULL AFTER vip,
    ADD COLUMN marketing_consent   BOOLEAN       NOT NULL DEFAULT FALSE AFTER internal_notes,
    ADD COLUMN blacklisted         BOOLEAN       NOT NULL DEFAULT FALSE AFTER marketing_consent,
    ADD COLUMN blacklist_reason    VARCHAR(500)  NULL AFTER blacklisted;
