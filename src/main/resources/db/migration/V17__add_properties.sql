-- =============================================================================
-- V17__add_properties.sql
-- Fondation multi-propriete : table properties + un etablissement par defaut
-- ("main") pour que toute installation existante continue de fonctionner
-- sans configuration prealable, et table d'acces explicite par utilisateur
-- (necessaire uniquement des qu'un 2e etablissement actif existe, voir
-- CurrentProperty). room_types/bookings sont rattaches a cet etablissement
-- par defaut dans V18/V19.
-- =============================================================================

CREATE TABLE properties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

INSERT INTO properties (name, code, address, active, created_at, updated_at)
VALUES ('Établissement principal', 'main', NULL, TRUE, NOW(6), NOW(6));

CREATE TABLE user_property_access (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_user_property_access_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT uq_user_property_access UNIQUE (user_id, property_id)
) ENGINE=InnoDB;
