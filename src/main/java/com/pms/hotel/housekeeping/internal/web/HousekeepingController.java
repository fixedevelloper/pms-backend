package com.pms.hotel.housekeeping.internal.web;

import com.pms.hotel.housekeeping.internal.HousekeepingService;
import com.pms.hotel.housekeeping.internal.HousekeepingTask;
import com.pms.hotel.housekeeping.internal.HousekeepingTaskRepository;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.CreateTaskRequest;
import com.pms.hotel.housekeeping.internal.web.HousekeepingRequests.UpdateTaskStatusRequest;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.shared.security.CurrentUser;
import com.pms.hotel.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    private final HousekeepingService housekeepingService;
    private final RoomApi roomApi;
    private final CurrentUser currentUser;

    @GetMapping("/tasks")
    public PageResponse<HousekeepingTaskView> index(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return PageResponse.of(taskRepository.search(status, taskType, pageable), this::toView);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public HousekeepingTaskView store(@Valid @RequestBody CreateTaskRequest request) {
        HousekeepingTask task = housekeepingService.create(request.roomId(), request.taskType(), request.assignedTo(), request.notes());
        return toView(task);
    }

    @GetMapping("/my-tasks")
    public List<HousekeepingTaskView> myTasks(@RequestParam(required = false) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return housekeepingService.myTasksForDay(currentUser.userId(), target).stream().map(this::toView).toList();
    }

    @PatchMapping("/tasks/{id}/status")
    public HousekeepingTaskView updateTaskStatus(@PathVariable Long id, @Valid @RequestBody UpdateTaskStatusRequest request) {
        HousekeepingTask task = housekeepingService.updateStatus(id, request.status(), currentUser.userId());
        return toView(task);
    }

    private HousekeepingTaskView toView(HousekeepingTask task) {
        String roomNumber = roomApi.getById(task.getRoomId()).roomNumber();
        return new HousekeepingTaskView(
                task.getId(), task.getRoomId(), roomNumber, task.getTaskType(),
                task.getStatus(), task.getAssignedTo(), task.getNotes(), task.getCreatedAt());
    }
}
