-- =============================================================================
-- V20__add_group_bookings.sql
-- Groupes/allotements (Phase 3.1) : un bloc de chambres pour un evenement,
-- rattache aux reservations individuelles de chaque participant
-- (bookings.group_id). Les allotements sont indicatifs (suivi du pick-up),
-- jamais un blocage d'inventaire.
-- =============================================================================

CREATE TABLE booking_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    company_id BIGINT NULL,
    contact_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(255),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'tentative',
    notes VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_booking_groups_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_booking_groups_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT chk_booking_groups_status CHECK (status IN ('tentative', 'confirmed', 'cancelled', 'closed'))
) ENGINE=InnoDB;

CREATE TABLE group_room_allotments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    rate_plan_id BIGINT NULL,
    allotted_rooms INT NOT NULL,
    notes VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_group_room_allotments_group FOREIGN KEY (group_id) REFERENCES booking_groups(id),
    CONSTRAINT fk_group_room_allotments_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(id),
    CONSTRAINT fk_group_room_allotments_rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plans(id)
) ENGINE=InnoDB;

ALTER TABLE bookings ADD COLUMN group_id BIGINT NULL;
ALTER TABLE bookings ADD CONSTRAINT fk_bookings_group FOREIGN KEY (group_id) REFERENCES booking_groups(id);

CREATE TABLE group_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'unpaid',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_group_invoices_group FOREIGN KEY (group_id) REFERENCES booking_groups(id),
    CONSTRAINT chk_group_invoices_status CHECK (status IN ('unpaid', 'paid'))
) ENGINE=InnoDB;

ALTER TABLE invoices ADD COLUMN group_invoice_id BIGINT NULL;
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_group_invoice FOREIGN KEY (group_invoice_id) REFERENCES group_invoices(id);
