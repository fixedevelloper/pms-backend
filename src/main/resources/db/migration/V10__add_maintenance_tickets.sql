-- =============================================================================
-- V10__add_maintenance_tickets.sql
-- Un statut de chambre "maintenance" existait deja, mais pas de ticket avec
-- description du probleme, priorite, technicien assigne, historique, cout de
-- reparation.
-- =============================================================================

CREATE TABLE maintenance_tickets (
    id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
    room_id      BIGINT        NOT NULL,
    title        VARCHAR(255)  NOT NULL,
    description  VARCHAR(2000) NULL,
    priority     VARCHAR(20)   NOT NULL DEFAULT 'medium',
    status       VARCHAR(20)   NOT NULL DEFAULT 'open',
    assigned_to  BIGINT        NULL,
    reported_by  BIGINT        NULL,
    cost         NUMERIC(10, 2) NULL,
    resolved_at  DATETIME(6)   NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    CONSTRAINT chk_maintenance_tickets_priority CHECK (priority IN ('low', 'medium', 'high', 'urgent')),
    CONSTRAINT chk_maintenance_tickets_status CHECK (status IN ('open', 'in_progress', 'resolved', 'cancelled')),
    CONSTRAINT fk_maintenance_tickets_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;
