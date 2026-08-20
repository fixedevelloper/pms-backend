-- =============================================================================
-- V16__add_bank_statement_lines.sql
-- Rapprochement bancaire : lignes de releve importees, rapprochees
-- automatiquement (montant + date a +/-3 jours) ou manuellement avec un
-- paiement existant (payments).
-- =============================================================================

CREATE TABLE bank_statement_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'unmatched',
    matched_payment_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_bank_statement_lines_payment FOREIGN KEY (matched_payment_id) REFERENCES payments(id),
    CONSTRAINT chk_bank_statement_lines_status CHECK (status IN ('unmatched', 'matched'))
) ENGINE=InnoDB;
