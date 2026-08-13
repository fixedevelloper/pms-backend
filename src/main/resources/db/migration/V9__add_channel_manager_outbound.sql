-- =============================================================================
-- V9__add_channel_manager_outbound.sql
-- ChannelManagerController ne fait que RECEVOIR les reservations OTA (webhook
-- entrant) - rien ne pousse en retour la disponibilite/les tarifs vers les
-- canaux, donc pas de garantie de parite tarifaire, risque reel de surbooking
-- si une chambre se vend simultanement sur deux canaux.
--
-- rooms.external_channel_room_id (V1) reste le mapping legacy utilise par le
-- webhook entrant existant (channel-agnostique). channel_room_mappings est un
-- nouveau mapping PAR CANAL : une meme chambre peut avoir un id externe
-- different sur Booking.com et sur Expedia.
-- =============================================================================

CREATE TABLE channels (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL UNIQUE,
    webhook_url    VARCHAR(500) NOT NULL,
    webhook_secret VARCHAR(255) NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL
) ENGINE = InnoDB;

CREATE TABLE channel_room_mappings (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    channel_id        BIGINT       NOT NULL,
    room_id           BIGINT       NOT NULL,
    external_room_id  VARCHAR(255) NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    CONSTRAINT uq_channel_room_mappings UNIQUE (channel_id, room_id),
    CONSTRAINT fk_channel_room_mappings_channel FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_channel_room_mappings_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;
