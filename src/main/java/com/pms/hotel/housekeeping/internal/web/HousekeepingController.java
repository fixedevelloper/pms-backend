package com.pms.hotel.housekeeping.internal.web;

import com.pms.hotel.housekeeping.internal.HousekeepingService;
import com.pms.hotel.housekeeping.internal.HousekeepingTask;
import com.pms.hotel.housekeeping.internal.HousekeepingTaskRepository;
import com.pms.hotel.housekeeping.internal.LostFoundItem;
import com.pms.hotel.housekeeping.internal.MinibarConsumption;
import com.pms.hotel.housekeeping.internal.MinibarItem;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.CreateMinibarItemRequest;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.CreateTaskRequest;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.RecordMinibarConsumptionRequest;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.ReportLostFoundItemRequest;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.UpdateLostFoundStatusRequest;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.UpdateTaskPriorityRequest;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.UpdateTaskStatusRequest;
import com.pms.hotel.property.CurrentProperty;
import com.pms.hotel.housekeeping.internal.LostFoundItemRepository;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import com.pms.hotel.shared.security.CurrentUser;
import com.pms.hotel.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/housekeeping")
@RequiredArgsConstructor
class HousekeepingController {

    private final HousekeepingTaskRepository taskRepository;
    private final LostFoundItemRepository lostFoundItemRepository;
    private final HousekeepingService housekeepingService;
    private final RoomApi roomApi;
    private final CurrentUser currentUser;
    private final CurrentProperty currentProperty;

    @GetMapping("/tasks")
    public PageResponse<HousekeepingTaskView> index(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        List<Long> roomIds = roomApi.findRoomIdsByProperty(currentProperty.resolve());
        if (roomIds.isEmpty()) {
            return PageResponse.of(Page.<HousekeepingTask>empty(pageable), this::toView);
        }
        return PageResponse.of(taskRepository.search(roomIds, status, taskType, pageable), this::toView);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public HousekeepingTaskView store(@Valid @RequestBody CreateTaskRequest request) {
        if (!roomApi.findRoomIdsByProperty(currentProperty.resolve()).contains(request.roomId())) {
            throw new BusinessRuleException("Cette chambre n'appartient pas à l'établissement courant.");
        }
        HousekeepingTask task = housekeepingService.create(
                request.roomId(), request.taskType(), request.priority(), request.assignedTo(), request.notes());
        return toView(task);
    }

    @GetMapping("/my-tasks")
    public List<HousekeepingTaskView> myTasks(@RequestParam(required = false) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        List<Long> roomIds = roomApi.findRoomIdsByProperty(currentProperty.resolve());
        if (roomIds.isEmpty()) {
            return List.of();
        }
        return housekeepingService.myTasksForDay(roomIds, currentUser.userId(), target).stream().map(this::toView).toList();
    }

    @PatchMapping("/tasks/{id}/status")
    public HousekeepingTaskView updateTaskStatus(@PathVariable Long id, @Valid @RequestBody UpdateTaskStatusRequest request) {
        requireTaskInCurrentProperty(id);
        HousekeepingTask task = housekeepingService.updateStatus(id, request.status(), currentUser.userId());
        return toView(task);
    }

    @PatchMapping("/tasks/{id}/priority")
    public HousekeepingTaskView updateTaskPriority(@PathVariable Long id, @Valid @RequestBody UpdateTaskPriorityRequest request) {
        requireTaskInCurrentProperty(id);
        HousekeepingTask task = housekeepingService.updatePriority(id, request.priority());
        return toView(task);
    }

    // --- Minibar ---------------------------------------------------------

    @GetMapping("/minibar/items")
    public List<MinibarItem> minibarItems() {
        return housekeepingService.listMinibarItems();
    }

    @PostMapping("/minibar/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MinibarItem createMinibarItem(@Valid @RequestBody CreateMinibarItemRequest request) {
        return housekeepingService.createMinibarItem(request.name(), request.unitPrice());
    }

    @GetMapping("/rooms/{roomId}/minibar/consumptions")
    public List<MinibarConsumptionView> minibarConsumptions(@PathVariable Long roomId) {
        requireRoomInCurrentProperty(roomId);
        return housekeepingService.minibarConsumptionForRoom(roomId).stream().map(c -> toView(roomId, c)).toList();
    }

    @PostMapping("/rooms/{roomId}/minibar/consumptions")
    @ResponseStatus(HttpStatus.CREATED)
    public MinibarConsumptionView recordMinibarConsumption(
            @PathVariable Long roomId, @Valid @RequestBody RecordMinibarConsumptionRequest request) {
        requireRoomInCurrentProperty(roomId);
        MinibarConsumption consumption = housekeepingService.recordMinibarConsumption(
                roomId, request.minibarItemId(), request.quantity(), currentUser.userId());
        return toView(roomId, consumption);
    }

    // --- Objets trouvés ----------------------------------------------------

    @GetMapping("/lost-found")
    public List<LostFoundItem> lostFoundItems(@RequestParam(required = false) String status) {
        return housekeepingService.listLostFoundItems(status);
    }

    @PostMapping("/lost-found")
    @ResponseStatus(HttpStatus.CREATED)
    public LostFoundItem reportLostFoundItem(@Valid @RequestBody ReportLostFoundItemRequest request) {
        return housekeepingService.reportLostFoundItem(
                request.roomId(), request.description(), request.foundLocation(), currentUser.userId(), request.notes());
    }

    @PatchMapping("/lost-found/{id}/status")
    public LostFoundItem updateLostFoundStatus(@PathVariable Long id, @Valid @RequestBody UpdateLostFoundStatusRequest request) {
        LostFoundItem item = lostFoundItemRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Objet trouvé", id));
        if (!roomApi.findRoomIdsByProperty(currentProperty.resolve()).contains(item.getRoomId())) {
            throw new BusinessRuleException("Cet objet trouvé n'appartient pas à l'établissement courant.");
        }
        return housekeepingService.updateLostFoundStatus(id, request.status(), request.claimantName());
    }

    private void requireTaskInCurrentProperty(Long taskId) {
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tâche", taskId));
        if (!roomApi.findRoomIdsByProperty(currentProperty.resolve()).contains(task.getRoomId())) {
            throw new BusinessRuleException("Cette tâche n'appartient pas à l'établissement courant.");
        }
    }

    private void requireRoomInCurrentProperty(Long roomId) {
        if (!roomApi.findRoomIdsByProperty(currentProperty.resolve()).contains(roomId)) {
            throw new BusinessRuleException("Cette chambre n'appartient pas à l'établissement courant.");
        }
    }

    private MinibarConsumptionView toView(Long roomId, MinibarConsumption consumption) {
        String roomNumber = roomApi.getById(roomId).roomNumber();
        MinibarItem item = consumption.getMinibarItem();
        return new MinibarConsumptionView(
                consumption.getId(), roomId, roomNumber, item.getId(), item.getName(), item.getUnitPrice(),
                consumption.getQuantity(), item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(consumption.getQuantity())),
                consumption.isBilled(), consumption.getCreatedAt());
    }

    private HousekeepingTaskView toView(HousekeepingTask task) {
        String roomNumber = roomApi.getById(task.getRoomId()).roomNumber();
        java.time.Instant checkoutAt = housekeepingService.resolveCheckoutAt(task.getRoomId());
        return new HousekeepingTaskView(
                task.getId(), task.getRoomId(), roomNumber, task.getTaskType(), task.getPriority(),
                task.getStatus(), task.getAssignedTo(), task.getNotes(), checkoutAt, task.getCreatedAt());
    }
}
