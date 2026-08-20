package com.pms.hotel.room.internal;

import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomAvailabilityChangedEvent;
import com.pms.hotel.room.RoomBlockView;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomOccupancyStats;
import com.pms.hotel.room.RoomStatusLogEntry;
import com.pms.hotel.room.RoomStatuses;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService implements RoomApi {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomStatusLogRepository roomStatusLogRepository;
    private final RoomBlockRepository roomBlockRepository;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<RoomDetails> findAll() {
        return roomRepository.findAll(Sort.by("roomNumber")).stream().map(Room::toSummary).toList();
    }

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
        return updateStatusWithChecklist(roomId, status, updatedByUserId, note, false, false, false);
    }

    /**
     * Comme {@link #updateStatus}, mais persiste en plus la check-list qualité
     * (linge/salle de bain/minibar) sur le RoomStatusLog — jusqu'ici construite
     * côté frontend (HousekeepingRoomsGrid) puis jetée après usage, jamais
     * envoyée à l'API. Réservé au contrôleur du module room (pas exposé sur
     * RoomApi : aucun autre module n'a besoin de poser une check-list).
     */
    public RoomDetails updateStatusWithChecklist(
            Long roomId, String status, Long updatedByUserId, String note,
            boolean linenChecked, boolean bathroomChecked, boolean minibarChecked) {
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
            log.setLinenChecked(linenChecked);
            log.setBathroomChecked(bathroomChecked);
            log.setMinibarChecked(minibarChecked);
            roomStatusLogRepository.save(log);
        }

        return room.toSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomOccupancyStats occupancyStats() {
        List<Long> blockedRoomIds = roomBlockRepository.findBlockedRoomIds(LocalDate.now());
        long total = roomRepository.count() - blockedRoomIds.size();
        long occupied = roomRepository.countByStatus(RoomStatuses.OCCUPIED);
        long available = blockedRoomIds.isEmpty()
                ? roomRepository.countByStatus(RoomStatuses.AVAILABLE)
                : roomRepository.countByStatusAndIdNotIn(RoomStatuses.AVAILABLE, blockedRoomIds);
        return new RoomOccupancyStats(total, occupied, available);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return roomBlockRepository.existsOverlap(roomId, checkIn, checkOut);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findRoomIdsByProperty(Long propertyId) {
        return roomRepository.findIdsByPropertyId(propertyId);
    }

    @Override
    @Transactional(readOnly = true)
    public com.pms.hotel.room.RoomTypeSummary getRoomTypeById(Long roomTypeId) {
        return roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Type de chambre", roomTypeId))
                .toSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomStatusLogEntry> statusChangesBetween(Instant from, Instant to) {
        return roomStatusLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to).stream()
                .map(log -> new RoomStatusLogEntry(
                        log.getRoom().getId(), log.getRoom().getRoomNumber(), log.getStatus(),
                        log.getNote(), log.getUpdatedBy(), log.getCreatedAt()))
                .toList();
    }

    /** Réservé au module room (contrôleur) : liste des blocages d'une chambre, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<RoomBlockView> listBlocksForRoom(Long roomId) {
        return roomBlockRepository.findByRoomIdOrderByStartDateDesc(roomId).stream().map(RoomBlock::toView).toList();
    }

    /** Tous les blocages de l'établissement donné, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<RoomBlockView> listAllBlocks(Long propertyId) {
        return roomBlockRepository.findByPropertyIdOrderByStartDateDesc(propertyId).stream().map(RoomBlock::toView).toList();
    }

    public RoomBlockView createBlock(Long roomId, LocalDate startDate, LocalDate endDate, String reason, String notes) {
        if (!endDate.isAfter(startDate)) {
            throw new BusinessRuleException("La date de fin doit être postérieure à la date de début.");
        }
        if (roomBlockRepository.existsOverlap(roomId, startDate, endDate)) {
            throw new BusinessRuleException("Cette chambre a déjà un blocage sur une période qui chevauche ces dates.");
        }
        Room room = findEntity(roomId);
        RoomBlock block = new RoomBlock();
        block.setRoom(room);
        block.setStartDate(startDate);
        block.setEndDate(endDate);
        block.setReason(reason);
        block.setNotes(notes);
        RoomBlockView view = roomBlockRepository.save(block).toView();
        events.publishEvent(new RoomAvailabilityChangedEvent(roomId, startDate, endDate, "blocked"));
        return view;
    }

    public void releaseBlock(Long roomId, Long blockId) {
        RoomBlock block = roomBlockRepository.findById(blockId)
                .orElseThrow(() -> ResourceNotFoundException.of("Blocage", blockId));
        if (!block.getRoom().getId().equals(roomId)) {
            throw new ResourceNotFoundException("Ce blocage n'appartient pas à la chambre indiquée.");
        }
        LocalDate startDate = block.getStartDate();
        LocalDate endDate = block.getEndDate();
        roomBlockRepository.delete(block);
        events.publishEvent(new RoomAvailabilityChangedEvent(roomId, startDate, endDate, "unblocked"));
    }

    Room findEntity(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Chambre", id));
    }
}
