-- =============================================================================
-- V4__add_night_audit.sql
-- night_audit_auto existe déjà comme réglage cliquable côté frontend
-- (settingService.ts) mais aucun code backend ne le lit ni n'exécute quoi
-- que ce soit chaque nuit — pas de date métier distincte de la date système,
-- pas de traitement des no-shows, pas de rapport de clôture.
-- =============================================================================

-- Singleton (une seule ligne en pratique) : la date métier de l'hôtel, avancée
-- uniquement par un run de night audit — pas par l'horloge système.
CREATE TABLE night_audit_state (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    business_date DATE        NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL
) ENGINE = InnoDB;

-- Historique des clôtures effectuées — une ligne par date métier auditée.
CREATE TABLE night_audit_runs (
    id                  BIGINT         AUTO_INCREMENT PRIMARY KEY,
    business_date       DATE           NOT NULL UNIQUE,
    ran_at              DATETIME(6)    NOT NULL,
    occupied_rooms      INT            NOT NULL,
    total_revenue       NUMERIC(12, 2) NOT NULL,
    no_shows_processed  INT            NOT NULL,
    no_show_fees_total  NUMERIC(10, 2) NOT NULL,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL
) ENGINE = InnoDB;
