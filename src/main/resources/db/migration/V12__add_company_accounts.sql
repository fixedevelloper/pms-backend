-- Add companies table
CREATE TABLE companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    contact_email VARCHAR(255),
    phone_number VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

-- Add company_id to bookings
ALTER TABLE bookings ADD COLUMN company_id BIGINT;
ALTER TABLE bookings ADD CONSTRAINT fk_bookings_company FOREIGN KEY (company_id) REFERENCES companies(id);
