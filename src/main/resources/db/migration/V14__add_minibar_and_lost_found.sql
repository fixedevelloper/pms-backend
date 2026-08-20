-- =============================================================================
-- V14__add_minibar_and_lost_found.sql
-- Inventaire minibar par article (remplace la case a cocher globale
-- room_status_logs.minibar_checked, conservee pour l'historique existant) et
-- registre des objets trouves.
-- =============================================================================

CREATE TABLE minibar_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE minibar_consumptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    minibar_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    billed BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_by BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_minibar_consumptions_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_minibar_consumptions_item FOREIGN KEY (minibar_item_id) REFERENCES minibar_items(id)
) ENGINE=InnoDB;

CREATE TABLE lost_found_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT,
    description VARCHAR(500) NOT NULL,
    found_location VARCHAR(255),
    found_by BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'stored',
    claimant_name VARCHAR(255),
    notes VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_lost_found_items_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT chk_lost_found_items_status CHECK (status IN ('stored', 'claimed', 'disposed'))
) ENGINE=InnoDB;
