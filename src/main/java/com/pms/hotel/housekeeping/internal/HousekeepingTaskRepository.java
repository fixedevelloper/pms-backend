package com.pms.hotel.housekeeping.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {

    @Query("""
            select t from HousekeepingTask t
            where (:status is null or t.status = :status)
                and (:taskType is null or t.taskType = :taskType)
            order by
                case t.priority when 'urgent' then 0 when 'high' then 1 when 'normal' then 2 else 3 end,
                t.createdAt desc
            """)
    Page<HousekeepingTask> search(@Param("status") String status, @Param("taskType") String taskType, Pageable pageable);

    @Query("""
            select t from HousekeepingTask t
            where t.assignedTo = :userId
                and t.createdAt >= :from and t.createdAt < :to
            order by
                case t.status when 'in_progress' then 0 when 'pending' then 1 else 2 end,
                case t.priority when 'urgent' then 0 when 'high' then 1 when 'normal' then 2 else 3 end,
                t.createdAt desc
            """)
    List<HousekeepingTask> findAssignedTasksForDay(
            @Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to);
}
