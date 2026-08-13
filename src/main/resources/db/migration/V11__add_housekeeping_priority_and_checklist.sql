-- =============================================================================
-- V11__add_housekeeping_priority_and_checklist.sql
-- HousekeepingTaskView n'avait aucun champ priorite (tri uniquement par date
-- de creation). Le frontend construisait deja une check-list qualite (linge,
-- salle de bain, minibar) mais ne l'envoyait jamais a l'API - purement un
-- garde-fou local sur le bouton "Marquer Propre", jetee apres usage.
-- =============================================================================

ALTER TABLE housekeeping_tasks
    ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'normal' AFTER task_type,
    ADD CONSTRAINT chk_housekeeping_tasks_priority CHECK (priority IN ('low', 'normal', 'high', 'urgent'));

ALTER TABLE room_status_logs
    ADD COLUMN linen_checked    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN bathroom_checked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN minibar_checked  BOOLEAN NOT NULL DEFAULT FALSE;
