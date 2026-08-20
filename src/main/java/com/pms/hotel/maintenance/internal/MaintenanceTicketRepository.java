package com.pms.hotel.maintenance.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {

    List<MaintenanceTicket> findByRoomIdInAndStatusOrderByCreatedAtDesc(List<Long> roomIds, String status);

    List<MaintenanceTicket> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    List<MaintenanceTicket> findByRoomIdInOrderByCreatedAtDesc(List<Long> roomIds);
}
