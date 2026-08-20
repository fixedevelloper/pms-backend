-- =============================================================================
-- V15__add_rate_change_audit_log.sql
-- Journal d'audit des modifications de tarifs (le pendant room_status_logs,
-- deja existant, couvrait deja les changements de statut de chambre).
-- =============================================================================

CREATE TABLE rate_change_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rate_plan_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    new_price DECIMAL(10, 2) NOT NULL,
    changed_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;
