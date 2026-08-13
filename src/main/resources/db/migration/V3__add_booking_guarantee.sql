-- =============================================================================
-- V3__add_booking_guarantee.sql
-- A "confirmed" booking had no guarantee at all — no deposit, no cancellation
-- policy enforcement, no way to know what (if anything) was collected upfront.
-- Combined with V2's rate plan cancellation_policy, this lets cancelling a
-- booking actually compute what's owed instead of being a silent no-op.
-- =============================================================================

ALTER TABLE bookings
    -- none | credit_card | deposit | company — how (if at all) this booking is secured.
    ADD COLUMN guarantee_type VARCHAR(20) NOT NULL DEFAULT 'none' AFTER source,
    ADD COLUMN deposit_amount NUMERIC(10, 2) NOT NULL DEFAULT 0 AFTER guarantee_type,
    -- NULL tant que la réservation n'a jamais été annulée ; calculé une seule
    -- fois au moment de l'annulation (voir BookingService#computeCancellationFee).
    ADD COLUMN cancellation_fee_amount NUMERIC(10, 2) NULL AFTER deposit_amount;
