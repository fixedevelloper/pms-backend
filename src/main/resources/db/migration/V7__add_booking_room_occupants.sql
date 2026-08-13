-- =============================================================================
-- V7__add_booking_room_occupants.sql
-- BookingCreateCommand ne portait qu'UN SEUL client (le titulaire) - impossible
-- d'enregistrer les accompagnants d'une chambre, pourtant requis legalement
-- dans beaucoup de pays pour la police/l'immigration a l'enregistrement.
-- V1 notait deja qu'un `booking_guests` existait cote Laravel mais etait mort
-- (jamais lu ni ecrit) - cette fois la table est reellement utilisee.
-- =============================================================================

CREATE TABLE booking_room_occupants (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    booking_room_id  BIGINT         NOT NULL,
    first_name       VARCHAR(255)   NOT NULL,
    last_name        VARCHAR(255)   NOT NULL,
    passport_number  VARCHAR(50)    NULL,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    CONSTRAINT fk_booking_room_occupants_room FOREIGN KEY (booking_room_id) REFERENCES booking_room (id) ON DELETE CASCADE
) ENGINE = InnoDB;
