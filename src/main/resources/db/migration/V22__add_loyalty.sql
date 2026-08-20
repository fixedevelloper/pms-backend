-- =============================================================================
-- V22__add_loyalty.sql
-- Programme de fidelite (Phase 4.1) : solde de points par client, journal des
-- mouvements (gain automatique a la sortie d'un sejour facture, ou ajustement
-- manuel). Le palier (Silver/Gold/Platinum) n'est jamais stocke : calcule a
-- la volee depuis le solde + les seuils configures dans settings, voir
-- LoyaltyService#toView.
-- =============================================================================

CREATE TABLE loyalty_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_id BIGINT NOT NULL UNIQUE,
    total_points BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_loyalty_accounts_guest FOREIGN KEY (guest_id) REFERENCES guests(id)
) ENGINE=InnoDB;

CREATE TABLE loyalty_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_id BIGINT NOT NULL,
    booking_id BIGINT NULL,
    points BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_loyalty_transactions_guest FOREIGN KEY (guest_id) REFERENCES guests(id),
    CONSTRAINT chk_loyalty_transactions_type CHECK (type IN ('earn', 'redeem', 'adjust'))
) ENGINE=InnoDB;
