package com.pms.hotel.maintenance.internal;

import com.pms.hotel.maintenance.MaintenanceTicketView;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceService {

    private final MaintenanceTicketRepository ticketRepository;
    private final RoomApi roomApi;

    /** {@code propertyRoomIds} : chambres de l'établissement courant (voir RoomApi#findRoomIdsByProperty) — {@code roomId}, s'il est fourni, doit en faire partie. */
    @Transactional(readOnly = true)
    public List<MaintenanceTicketView> list(List<Long> propertyRoomIds, String status, Long roomId) {
        if (propertyRoomIds.isEmpty()) {
            return List.of();
        }
        List<MaintenanceTicket> tickets;
        if (roomId != null) {
            if (!propertyRoomIds.contains(roomId)) {
                throw new BusinessRuleException("Cette chambre n'appartient pas à l'établissement courant.");
            }
            tickets = ticketRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
        } else if (status != null) {
            tickets = ticketRepository.findByRoomIdInAndStatusOrderByCreatedAtDesc(propertyRoomIds, status);
        } else {
            tickets = ticketRepository.findByRoomIdInOrderByCreatedAtDesc(propertyRoomIds);
        }
        return tickets.stream().map(t -> t.toView(roomApi.getById(t.getRoomId()).roomNumber())).toList();
    }

    public MaintenanceTicketView create(List<Long> propertyRoomIds, Long roomId, String title, String description, String priority, Long reportedBy) {
        if (!propertyRoomIds.contains(roomId)) {
            throw new BusinessRuleException("Cette chambre n'appartient pas à l'établissement courant.");
        }
        String roomNumber = roomApi.getById(roomId).roomNumber(); // 404 si la chambre n'existe pas

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setRoomId(roomId);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(priority != null ? priority : MaintenanceTicket.MEDIUM);
        ticket.setStatus(MaintenanceTicket.OPEN);
        ticket.setReportedBy(reportedBy);
        return ticketRepository.save(ticket).toView(roomNumber);
    }

    public MaintenanceTicketView update(Long id, String title, String description, String priority, String status, Long assignedTo, BigDecimal cost) {
        MaintenanceTicket ticket = findEntity(id);

        if (title != null) ticket.setTitle(title);
        if (description != null) ticket.setDescription(description);
        if (priority != null) ticket.setPriority(priority);
        if (assignedTo != null) ticket.setAssignedTo(assignedTo);
        if (cost != null) ticket.setCost(cost);

        if (status != null && !status.equals(ticket.getStatus())) {
            ticket.setStatus(status);
            ticket.setResolvedAt(MaintenanceTicket.RESOLVED.equals(status) ? Instant.now() : null);
        }

        String roomNumber = roomApi.getById(ticket.getRoomId()).roomNumber();
        return ticketRepository.save(ticket).toView(roomNumber);
    }

    private MaintenanceTicket findEntity(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Ticket de maintenance", id));
    }
}
