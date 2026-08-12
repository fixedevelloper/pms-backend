-- ==========================================================================
-- Spring Modulith event publication registry
-- (spring-modulith-starter-jpa persists in-flight/completed event publications here)
-- DDL confirmed by letting Hibernate (hibernate.ddl-auto=create) generate its
-- own schema once against a scratch MySQL 8 database for this exact entity
-- mapping, then copying what it produced: `id` is BINARY(16) (Hibernate's
-- default mapping for java.util.UUID on MySQL — no native UUID type there,
-- unlike H2/Postgres), and `status` is a nullable native ENUM, not the
-- VARCHAR+CHECK used for every other status-like column in this file.
-- ==========================================================================
CREATE TABLE event_publication (
    id                       BINARY(16)    NOT NULL PRIMARY KEY,
    listener_id               VARCHAR(512)  NOT NULL,
    event_type                VARCHAR(512)  NOT NULL,
    serialized_event          VARCHAR(4000) NOT NULL,
    publication_date          DATETIME(6)   NOT NULL,
    completion_date           DATETIME(6),
    last_resubmission_date    DATETIME(6),
    completion_attempts       INTEGER       NOT NULL DEFAULT 0,
    status                    ENUM('COMPLETED', 'FAILED', 'PROCESSING', 'PUBLISHED', 'RESUBMITTED')
) ENGINE = InnoDB;

-- ==========================================================================
-- ROOM module: catalog & physical inventory
-- ==========================================================================
-- Note: the Laravel source also defined `room_rates` and `hotel_services` /
-- `room_availabilities` tables, but no controller in the original app ever
-- read or wrote them (dead schema). They are intentionally left out here.
CREATE TABLE room_types (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255)   NOT NULL,
    description   TEXT,
    base_capacity INTEGER        NOT NULL,
    base_price    NUMERIC(10, 2) NOT NULL,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL
) ENGINE = InnoDB;

CREATE TABLE rooms (
    id                       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    room_type_id             BIGINT       NOT NULL,
    room_number              VARCHAR(50)  NOT NULL UNIQUE,
    floor                    INTEGER      NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'available',
    external_channel_room_id VARCHAR(255) UNIQUE,
    created_at               DATETIME(6)  NOT NULL,
    updated_at               DATETIME(6)  NOT NULL,
    CONSTRAINT chk_rooms_status CHECK (status IN ('available', 'occupied', 'dirty', 'maintenance')),
    CONSTRAINT fk_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE room_status_logs (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    room_id    BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    note       TEXT,
    updated_by BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT chk_room_status_logs_status CHECK (status IN ('available', 'occupied', 'dirty', 'maintenance')),
    CONSTRAINT fk_room_status_logs_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ==========================================================================
-- GUEST module
-- ==========================================================================
CREATE TABLE guests (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    first_name       VARCHAR(255) NOT NULL,
    last_name        VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    phone            VARCHAR(50),
    passport_number  VARCHAR(50),
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL
) ENGINE = InnoDB;

-- ==========================================================================
-- BOOKING module
-- ==========================================================================
CREATE TABLE bookings (
    id                  BIGINT         AUTO_INCREMENT PRIMARY KEY,
    guest_id            BIGINT         NOT NULL,
    checked_in_at       DATETIME(6),
    checked_out_at      DATETIME(6),
    status              VARCHAR(20)    NOT NULL DEFAULT 'pending',
    source              VARCHAR(50)    NOT NULL DEFAULT 'direct',
    external_reference  VARCHAR(255),
    tax_amount          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(10, 2) NOT NULL,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    CONSTRAINT chk_bookings_status CHECK (status IN ('pending', 'confirmed', 'checked_in', 'checked_out', 'cancelled')),
    CONSTRAINT fk_bookings_guest FOREIGN KEY (guest_id) REFERENCES guests (id) ON DELETE CASCADE
) ENGINE = InnoDB;
CREATE INDEX idx_bookings_stay_dates ON bookings (checked_in_at, checked_out_at);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_bookings_external_reference ON bookings (external_reference);

CREATE TABLE booking_room (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    booking_id       BIGINT         NOT NULL,
    room_id          BIGINT         NOT NULL,
    price_per_night  NUMERIC(10, 2) NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    CONSTRAINT uq_booking_room UNIQUE (booking_id, room_id),
    CONSTRAINT fk_booking_room_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_room_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Note: the Laravel source also had a `booking_guests` pivot (secondary
-- occupants) but no controller ever read or wrote it (dead schema) -
-- intentionally left out here, same as `room_rates` / `hotel_services`.

-- ==========================================================================
-- HOUSEKEEPING module
-- ==========================================================================
CREATE TABLE housekeeping_tasks (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    room_id     BIGINT      NOT NULL,
    task_type   VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'pending',
    assigned_to BIGINT,
    notes       TEXT,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT chk_housekeeping_tasks_task_type CHECK (task_type IN ('cleaning', 'laundry', 'maintenance', 'inspection')),
    CONSTRAINT chk_housekeeping_tasks_status CHECK (status IN ('pending', 'in_progress', 'completed')),
    CONSTRAINT fk_housekeeping_tasks_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ==========================================================================
-- POS module (restaurant / spa / bar charges billed to a room)
-- ==========================================================================
CREATE TABLE extra_charges (
    id                  BIGINT         AUTO_INCREMENT PRIMARY KEY,
    booking_id          BIGINT         NOT NULL,
    department          VARCHAR(50)    NOT NULL,
    item_name           VARCHAR(255)   NOT NULL,
    quantity            INTEGER        NOT NULL DEFAULT 1,
    unit_price          NUMERIC(10, 2) NOT NULL,
    total_price         NUMERIC(10, 2) NOT NULL,
    tax_amount          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    external_order_id   VARCHAR(255),
    payment_status      VARCHAR(20)    NOT NULL DEFAULT 'charged_to_room',
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    CONSTRAINT chk_extra_charges_payment_status CHECK (payment_status IN ('pending', 'paid_instantly', 'charged_to_room')),
    CONSTRAINT fk_extra_charges_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ==========================================================================
-- BILLING module
-- ==========================================================================
CREATE TABLE invoices (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT         NOT NULL UNIQUE,
    invoice_number  VARCHAR(50)    NOT NULL UNIQUE,
    total_rooms     NUMERIC(10, 2) NOT NULL,
    total_extras    NUMERIC(10, 2) NOT NULL,
    total_amount    NUMERIC(10, 2) NOT NULL,
    amount_paid     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(10, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)    NOT NULL DEFAULT 'unpaid',
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    CONSTRAINT chk_invoices_status CHECK (status IN ('unpaid', 'partially_paid', 'paid', 'refunded')),
    CONSTRAINT fk_invoices_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE invoice_items (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY,
    invoice_id   BIGINT         NOT NULL,
    description  VARCHAR(255)   NOT NULL,
    quantity     INTEGER        NOT NULL DEFAULT 1,
    unit_price   NUMERIC(15, 2) NOT NULL,
    total_price  NUMERIC(15, 2) NOT NULL,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE payments (
    id                     BIGINT         AUTO_INCREMENT PRIMARY KEY,
    booking_id             BIGINT         NOT NULL,
    invoice_id             BIGINT,
    amount                 NUMERIC(10, 2) NOT NULL,
    currency               VARCHAR(3)     NOT NULL DEFAULT 'XAF',
    payment_method         VARCHAR(20)    NOT NULL,
    status                 VARCHAR(20)    NOT NULL DEFAULT 'pending',
    gateway                VARCHAR(50),
    transaction_reference  VARCHAR(255) UNIQUE,
    paid_at                DATETIME(6),
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    CONSTRAINT chk_payments_payment_method CHECK (payment_method IN
        ('credit_card', 'cash', 'bank_transfer', 'mobile_money', 'stripe', 'paypal')),
    CONSTRAINT chk_payments_status CHECK (status IN ('pending', 'completed', 'failed', 'refunded')),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- ==========================================================================
-- SETTINGS module
-- ==========================================================================
CREATE TABLE settings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key    VARCHAR(255) NOT NULL UNIQUE,
    setting_value  TEXT
) ENGINE = InnoDB;
