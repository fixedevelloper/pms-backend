-- =============================================================================
-- V18__add_property_scoping.sql
-- Rattache les types de chambre et les reservations a un etablissement.
-- rooms/room_blocks/housekeeping_tasks/maintenance_tickets restent sans
-- colonne dediee : leur etablissement se deduit de room_types.property_id
-- (via room_id) au moment de la lecture, voir RoomApi#findRoomIdsByProperty.
-- =============================================================================

ALTER TABLE room_types ADD COLUMN property_id BIGINT NULL AFTER id;
UPDATE room_types SET property_id = (SELECT id FROM properties WHERE code = 'main');
ALTER TABLE room_types MODIFY COLUMN property_id BIGINT NOT NULL;
ALTER TABLE room_types ADD CONSTRAINT fk_room_types_property FOREIGN KEY (property_id) REFERENCES properties(id);

ALTER TABLE bookings ADD COLUMN property_id BIGINT NULL AFTER id;
UPDATE bookings SET property_id = (SELECT id FROM properties WHERE code = 'main');
ALTER TABLE bookings MODIFY COLUMN property_id BIGINT NOT NULL;
ALTER TABLE bookings ADD CONSTRAINT fk_bookings_property FOREIGN KEY (property_id) REFERENCES properties(id);
