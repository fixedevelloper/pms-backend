-- =============================================================================
-- V8__add_room_blocks.sql
-- Pas de concept de chambre bloquee hors-vente (maintenance longue duree,
-- renovation, usage interne), distinct du statut operationnel "maintenance"
-- (rooms.status). Sans ca, une chambre en travaux pour 3 semaines reste
-- comptee dans l'inventaire vendable.
-- =============================================================================

CREATE TABLE room_blocks (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    room_id     BIGINT       NOT NULL,
    -- Demi-ouvert [start_date, end_date) — meme convention que checkIn/checkOut
    -- des reservations : la nuit de end_date n'est pas bloquee.
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    reason      VARCHAR(30)  NOT NULL,
    notes       VARCHAR(500) NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT chk_room_blocks_reason CHECK (reason IN ('maintenance', 'renovation', 'internal_use', 'other')),
    CONSTRAINT chk_room_blocks_dates CHECK (end_date > start_date),
    CONSTRAINT fk_room_blocks_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;
