package com.pms.hotel.room;

import java.time.Instant;

/** Une entrée de l'historique des changements de statut d'une chambre — déjà persistée à chaque changement, voir RoomService#updateStatusWithChecklist. */
public record RoomStatusLogEntry(Long roomId, String roomNumber, String status, String note, Long updatedByUserId, Instant changedAt) {
}
