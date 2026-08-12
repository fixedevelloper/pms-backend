package com.pms.hotel.room.internal;

import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomOccupancyStats;
import com.pms.hotel.room.RoomStatuses;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
class RoomService implements RoomApi {

    private final RoomRepository roomRepository;
    private final RoomStatusLogRepository roomStatusLogRepository;

    @Override
    @Transactional(readOnly = true)
    public RoomDetails getById(Long roomId) {
        return findEntity(roomId).toSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDetails getByRoomNumber(String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber)
                .map(Room::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable pour le numéro " + roomNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoomDetails> findByExternalChannelRoomId(String externalChannelRoomId) {
        return roomRepository.findByExternalChannelRoomId(externalChannelRoomId).map(Room::toSummary);
    }

    @Override
    public RoomDetails updateStatus(Long roomId, String status, Long updatedByUserId, String note) {
        Room room = findEntity(roomId);
        boolean changed = !status.equals(room.getStatus());
        room.setStatus(status);
        room = roomRepository.save(room);

        if (changed) {
            RoomStatusLog log = new RoomStatusLog();
            log.setRoom(room);
            log.setStatus(status);
            log.setNote(note != null ? note : "Changement d'état automatique ou manuel.");
            log.setUpdatedBy(updatedByUserId);
            roomStatusLogRepository.save(log);
        }

        return room.toSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomOccupancyStats occupancyStats() {
        long total = roomRepository.count();
        long occupied = roomRepository.countByStatus(RoomStatuses.OCCUPIED);
        long available = roomRepository.countByStatus(RoomStatuses.AVAILABLE);
        return new RoomOccupancyStats(total, occupied, available);
    }

    Room findEntity(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Chambre", id));
    }
}
