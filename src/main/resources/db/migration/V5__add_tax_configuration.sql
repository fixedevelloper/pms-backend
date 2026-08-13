-- =============================================================================
-- V5__add_tax_configuration.sql
-- Invoice.taxAmount (TVA) n'a jamais été calculé nulle part — toujours 0 —
-- malgré un réglage tva_default déjà présent côté Paramètres Hôtel. Pas de
-- taxe de séjour (city tax) du tout, laquelle se calcule par nuit et par
-- adulte (enfants exonérés) — d'où le comptage d'occupants sur booking_room.
-- =============================================================================

ALTER TABLE booking_room
    ADD COLUMN adults_count INT NOT NULL DEFAULT 1 AFTER price_per_night,
    ADD COLUMN children_count INT NOT NULL DEFAULT 0 AFTER adults_count;

ALTER TABLE invoices
    ADD COLUMN city_tax_amount NUMERIC(10, 2) NOT NULL DEFAULT 0 AFTER tax_amount;
