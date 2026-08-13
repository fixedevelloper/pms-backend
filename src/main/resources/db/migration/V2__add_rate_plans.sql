-- =============================================================================
-- V2__add_rate_plans.sql
-- The Laravel source this app was ported from had a `room_rates` table that
-- nothing ever read or wrote (see the note in V1__baseline_schema.sql) — this
-- is that concept, rebuilt and actually wired into booking creation this time:
-- named rate plans per room type (e.g. "Flexible", "Non-remboursable",
-- "Petit-dejeuner inclus"), each with its own price and cancellation policy.
-- =============================================================================

CREATE TABLE rate_plans (
    id                       BIGINT         AUTO_INCREMENT PRIMARY KEY,
    room_type_id             BIGINT         NOT NULL,
    name                     VARCHAR(255)   NOT NULL,
    description              TEXT,
    price_per_night          NUMERIC(10, 2) NOT NULL,
    breakfast_included       BOOLEAN        NOT NULL DEFAULT FALSE,
    -- flexible | non_refundable | partial_refund
    cancellation_policy      VARCHAR(20)    NOT NULL DEFAULT 'flexible',
    -- Nombre de jours avant l'arrivee en dessous duquel l'annulation gratuite
    -- n'est plus possible (politique 'flexible').
    free_cancellation_days   INTEGER,
    -- Pourcentage du sejour facture en cas d'annulation (politique 'partial_refund').
    cancellation_fee_percent NUMERIC(5, 2),
    active                   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at               DATETIME(6)    NOT NULL,
    updated_at               DATETIME(6)    NOT NULL,
    CONSTRAINT fk_rate_plans_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Nullable : les reservations existantes et celles remontees par le channel
-- manager (upsertFromExternalChannel) n'ont pas de rate plan interne.
ALTER TABLE booking_room ADD COLUMN rate_plan_id BIGINT NULL AFTER room_id;
ALTER TABLE booking_room ADD CONSTRAINT fk_booking_room_rate_plan
    FOREIGN KEY (rate_plan_id) REFERENCES rate_plans (id);
