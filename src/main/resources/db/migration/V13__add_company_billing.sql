-- =============================================================================
-- V13__add_company_billing.sql
-- Comptes societes (companies) : tarif negocie + cycle de facturation, et
-- facture groupee (company_invoices) agregeant les factures individuelles
-- des sejours d'une societe sur une periode. bookings.company_id existait
-- deja (V12) mais n'etait relie a rien cote code.
-- =============================================================================

ALTER TABLE companies
    ADD COLUMN negotiated_rate_plan_id BIGINT NULL AFTER phone_number,
    ADD COLUMN billing_cycle VARCHAR(20) NOT NULL DEFAULT 'immediate' AFTER negotiated_rate_plan_id,
    ADD CONSTRAINT chk_companies_billing_cycle CHECK (billing_cycle IN ('immediate', 'monthly')),
    ADD CONSTRAINT fk_companies_negotiated_rate_plan FOREIGN KEY (negotiated_rate_plan_id) REFERENCES rate_plans(id);

CREATE TABLE company_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'unpaid',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_company_invoices_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT chk_company_invoices_status CHECK (status IN ('unpaid', 'paid'))
) ENGINE=InnoDB;

ALTER TABLE invoices
    ADD COLUMN company_invoice_id BIGINT NULL AFTER discount_amount,
    ADD CONSTRAINT fk_invoices_company_invoice FOREIGN KEY (company_invoice_id) REFERENCES company_invoices(id);
