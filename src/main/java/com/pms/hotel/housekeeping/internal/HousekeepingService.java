package com.pms.hotel.housekeeping.internal;

import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomStatuses;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HousekeepingService {

    private static final Set<String> ROOM_RESTORING_TASK_TYPES = Set.of("cleaning", "inspection");

    private final HousekeepingTaskRepository taskRepository;
    private final RoomApi roomApi;

    public HousekeepingTask create(Long roomId, String taskType, Long assignedTo, String notes) {
        HousekeepingTask task = new HousekeepingTask();
        task.setRoomId(roomId);
        task.setTaskType(taskType);
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

    @Transactional(readOnly = true)
    public List<HousekeepingTask> myTasksForDay(Long userId, java.time.LocalDate date) {
        java.time.Instant from = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant to = date.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        return taskRepository.findAssignedTasksForDay(userId, from, to);
    }

    HousekeepingTask findEntity(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tâche", id));
    }
}
