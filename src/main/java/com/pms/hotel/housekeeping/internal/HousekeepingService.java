package com.pms.hotel.housekeeping.internal;

import com.pms.hotel.booking.ActiveStay;
import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.pos.PosApi;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomStatuses;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HousekeepingService {

    private static final Set<String> ROOM_RESTORING_TASK_TYPES = Set.of("cleaning", "inspection");
    private static final List<String> STATUS_ORDER = List.of("in_progress", "pending", "completed");
    private static final List<String> PRIORITY_ORDER = List.of("urgent", "high", "normal", "low");

    private final HousekeepingTaskRepository taskRepository;
    private final MinibarItemRepository minibarItemRepository;
    private final MinibarConsumptionRepository minibarConsumptionRepository;
    private final LostFoundItemRepository lostFoundItemRepository;
    private final RoomApi roomApi;
    private final BookingApi bookingApi;
    private final PosApi posApi;

    public HousekeepingTask create(Long roomId, String taskType, String priority, Long assignedTo, String notes) {
        HousekeepingTask task = new HousekeepingTask();
        task.setRoomId(roomId);
        task.setTaskType(taskType);
        task.setPriority(priority != null ? priority : HousekeepingTask.NORMAL);
        task.setAssignedTo(assignedTo);
        task.setNotes(notes);
        task = taskRepository.save(task);

        // Mirrors the original Laravel controller: creating a maintenance task
        // takes the room out of service, other task types mark it dirty.
        if ("maintenance".equals(taskType)) {
            roomApi.updateStatus(roomId, RoomStatuses.MAINTENANCE, assignedTo, "Tâche de maintenance créée.");
        } else {
            var room = roomApi.getById(roomId);
            if (RoomStatuses.AVAILABLE.equals(room.status())) {
                roomApi.updateStatus(roomId, RoomStatuses.DIRTY, assignedTo, "Tâche de service d'étage créée.");
            }
        }

        return task;
    }

    public HousekeepingTask updateStatus(Long taskId, String status, Long actingUserId) {
        HousekeepingTask task = findEntity(taskId);
        task.setStatus(status);
        task = taskRepository.save(task);

        if (HousekeepingTask.COMPLETED.equals(status) && ROOM_RESTORING_TASK_TYPES.contains(task.getTaskType())) {
            roomApi.updateStatus(task.getRoomId(), RoomStatuses.AVAILABLE, actingUserId, "Tâche de service d'étage terminée.");
        }

        return task;
    }

    public HousekeepingTask updatePriority(Long taskId, String priority) {
        HousekeepingTask task = findEntity(taskId);
        task.setPriority(priority);
        return taskRepository.save(task);
    }

    /** Départ prévu du séjour actuellement en cours dans la chambre, si occupée — pour donner du contexte de priorisation au personnel d'étage. */
    @Transactional(readOnly = true)
    public Instant resolveCheckoutAt(Long roomId) {
        return bookingApi.findActiveCheckedInStay(roomId)
                .map(stay -> bookingApi.getById(stay.bookingId()).checkedOutAt())
                .orElse(null);
    }

    /**
     * Comme la requête SQL (tri par statut puis priorité), avec un
     * départager supplémentaire : à statut/priorité égaux, la chambre dont
     * le départ est le plus proche passe en premier — le personnel d'étage
     * traite d'abord les chambres qui doivent être prêtes le plus tôt.
     */
    @Transactional(readOnly = true)
    public List<HousekeepingTask> myTasksForDay(List<Long> roomIds, Long userId, java.time.LocalDate date) {
        java.time.Instant from = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant to = date.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        List<HousekeepingTask> tasks = taskRepository.findAssignedTasksForDay(roomIds, userId, from, to);
        return tasks.stream()
                .sorted(Comparator
                        .comparing((HousekeepingTask t) -> STATUS_ORDER.indexOf(t.getStatus()))
                        .thenComparing(t -> PRIORITY_ORDER.indexOf(t.getPriority()))
                        .thenComparing(t -> resolveCheckoutAt(t.getRoomId()), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    // --- Minibar --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MinibarItem> listMinibarItems() {
        return minibarItemRepository.findByActiveTrueOrderByName();
    }

    public MinibarItem createMinibarItem(String name, BigDecimal unitPrice) {
        MinibarItem item = new MinibarItem();
        item.setName(name);
        item.setUnitPrice(unitPrice);
        return minibarItemRepository.save(item);
    }

    /**
     * Enregistre une consommation minibar constatée en chambre et la facture
     * immédiatement au séjour actif de la chambre, s'il y en a un — sinon
     * (chambre vide, simple contrôle de réassort) elle reste non facturée.
     */
    public MinibarConsumption recordMinibarConsumption(Long roomId, Long minibarItemId, int quantity, Long recordedByUserId) {
        MinibarItem item = minibarItemRepository.findById(minibarItemId)
                .orElseThrow(() -> ResourceNotFoundException.of("Article minibar", minibarItemId));

        MinibarConsumption consumption = new MinibarConsumption();
        consumption.setRoomId(roomId);
        consumption.setMinibarItem(item);
        consumption.setQuantity(quantity);
        consumption.setRecordedBy(recordedByUserId);

        Optional<ActiveStay> activeStay = bookingApi.findActiveCheckedInStay(roomId);
        if (activeStay.isPresent()) {
            posApi.chargeToRoom(activeStay.get().bookingId(), "minibar", item.getName(), quantity, item.getUnitPrice(), null);
            consumption.setBilled(true);
        }

        return minibarConsumptionRepository.save(consumption);
    }

    @Transactional(readOnly = true)
    public List<MinibarConsumption> minibarConsumptionForRoom(Long roomId) {
        return minibarConsumptionRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    // --- Objets trouvés ---------------------------------------------------

    public LostFoundItem reportLostFoundItem(Long roomId, String description, String foundLocation, Long foundByUserId, String notes) {
        LostFoundItem item = new LostFoundItem();
        item.setRoomId(roomId);
        item.setDescription(description);
        item.setFoundLocation(foundLocation);
        item.setFoundBy(foundByUserId);
        item.setNotes(notes);
        return lostFoundItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<LostFoundItem> listLostFoundItems(String status) {
        return status != null
                ? lostFoundItemRepository.findByStatusOrderByCreatedAtDesc(status)
                : lostFoundItemRepository.findAllByOrderByCreatedAtDesc();
    }

    /** Marque un objet trouvé comme réclamé (avec le nom du client) ou éliminé. */
    public LostFoundItem updateLostFoundStatus(Long id, String status, String claimantName) {
        LostFoundItem item = lostFoundItemRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Objet trouvé", id));
        item.setStatus(status);
        if (claimantName != null) {
            item.setClaimantName(claimantName);
        }
        return lostFoundItemRepository.save(item);
    }

    HousekeepingTask findEntity(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tâche", id));
    }
}
