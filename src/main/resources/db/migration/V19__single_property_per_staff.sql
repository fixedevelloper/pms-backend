-- =============================================================================
-- V19__single_property_per_staff.sql
-- Un membre du personnel ne peut etre affecte qu'a UN SEUL etablissement a la
-- fois (evite d'avoir a choisir un etablissement lors d'une reservation) :
-- remplace la contrainte unique (user_id, property_id) par une contrainte
-- unique sur user_id seul. Garde-fou : supprime d'abord les doublons
-- eventuels (fonctionnalite neuve, peu/pas de donnees reelles a ce jour) en
-- ne gardant que l'affectation la plus recente.
-- =============================================================================

DELETE upa1 FROM user_property_access upa1
INNER JOIN user_property_access upa2
    ON upa1.user_id = upa2.user_id AND upa1.id < upa2.id;

ALTER TABLE user_property_access DROP INDEX uq_user_property_access;
ALTER TABLE user_property_access ADD CONSTRAINT uq_user_property_access_user UNIQUE (user_id);
