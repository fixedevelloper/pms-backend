-- =============================================================================
-- V23__add_foreign_currency_payments.sql
-- Multi-devises (Phase 4.2) : trace le montant reellement remis et le taux
-- applique au moment du paiement quand un client paie dans une devise
-- differente de la devise de reference de l'etablissement. payments.amount/
-- currency restent l'equivalent en devise de reference (comportement
-- inchange pour le calcul du solde des factures).
-- =============================================================================

ALTER TABLE payments
    ADD COLUMN tendered_amount DECIMAL(12, 2) NULL,
    ADD COLUMN tendered_currency VARCHAR(3) NULL,
    ADD COLUMN exchange_rate_used DECIMAL(18, 8) NULL;
