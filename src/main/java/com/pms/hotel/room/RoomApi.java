package com.pms.hotel.room;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Public entry point into the room module for the rest of the application.
 * Other modules must depend only on this interface (and the records it
 * exposes), never on {@code com.pms.hotel.room.internal} types.
 */
public interface RoomApi {

    /** All rooms, ordered by room number. */
    List<RoomDetails> findAll();

    RoomDetails getById(Long roomId);

    RoomDetails getByRoomNumber(String roomNumber);

    /** Looks up the physical room mapped to a channel-manager (OTA) room id. */
    Optional<RoomDetails> findByExternalChannelRoomId(String externalChannelRoomId);

    /**
     * Updates a room's housekeeping/occupancy status and records the change
     * in the room's status history, mirroring the automatic audit trail the
     * original Laravel {@code Room} model kept on every status update.
     */
    RoomDetails updateStatus(Long roomId, String status, Long updatedByUserId, String note);

    RoomOccupancyStats occupancyStats();

    /** True si la chambre a un blocage hors-vente (maintenance/rénovation/usage interne) chevauchant [checkIn, checkOut). */
    boolean isBlocked(Long roomId, LocalDate checkIn, LocalDate checkOut);

    /** Historique des changements de statut sur [from, to) — utilisé par le journal d'audit du reporting. */
    List<RoomStatusLogEntry> statusChangesBetween(Instant from, Instant to);

    /** Ids des chambres d'un établissement — utilisé pour cloisonner les listes housekeeping/maintenance par propriété (modules sans relation JPA directe vers Room). */
    List<Long> findRoomIdsByProperty(Long propertyId);

    RoomTypeSummary getRoomTypeById(Long roomTypeId);
}
