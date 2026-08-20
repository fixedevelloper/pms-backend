package com.pms.hotel.groupbooking.internal;

import com.pms.hotel.company.CompanyApi;
import com.pms.hotel.groupbooking.GroupBookingApi;
import com.pms.hotel.groupbooking.GroupSummary;
import com.pms.hotel.groupbooking.internal.web.GroupBookingRequests.CreateAllotmentRequest;
import com.pms.hotel.groupbooking.internal.web.GroupBookingRequests.CreateGroupRequest;
import com.pms.hotel.groupbooking.internal.web.GroupBookingRequests.UpdateAllotmentRequest;
import com.pms.hotel.groupbooking.internal.web.GroupBookingRequests.UpdateGroupRequest;
import com.pms.hotel.rateplan.RatePlanApi;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupBookingService implements GroupBookingApi {

    private final BookingGroupRepository groupRepository;
    private final CompanyApi companyApi;
    private final RatePlanApi ratePlanApi;
    private final RoomApi roomApi;

    @Override
    @Transactional(readOnly = true)
    public GroupSummary getById(Long groupId) {
        return findEntity(groupId).toSummary();
    }

    @Transactional(readOnly = true)
    public List<GroupSummary> list(Long propertyId) {
        return groupRepository.findByPropertyIdOrderByCheckInDesc(propertyId).stream().map(BookingGroup::toSummary).toList();
    }

    public GroupSummary create(Long propertyId, CreateGroupRequest request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new BusinessRuleException("La date de départ doit être postérieure à la date d'arrivée.");
        }
        if (request.companyId() != null) {
            companyApi.getById(request.companyId()); // 404 si la société n'existe pas
        }

        BookingGroup group = new BookingGroup();
        group.setPropertyId(propertyId);
        group.setName(request.name());
        group.setCompanyId(request.companyId());
        group.setContactName(request.contactName());
        group.setContactEmail(request.contactEmail());
        group.setContactPhone(request.contactPhone());
        group.setCheckIn(request.checkIn());
        group.setCheckOut(request.checkOut());
        group.setNotes(request.notes());
        return groupRepository.save(group).toSummary();
    }

    public GroupSummary update(Long groupId, UpdateGroupRequest request) {
        BookingGroup group = findEntity(groupId);
        if (request.name() != null) group.setName(request.name());
        if (request.contactName() != null) group.setContactName(request.contactName());
        if (request.contactEmail() != null) group.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) group.setContactPhone(request.contactPhone());
        if (request.status() != null) group.setStatus(request.status());
        if (request.notes() != null) group.setNotes(request.notes());
        return groupRepository.save(group).toSummary();
    }

    public GroupSummary addAllotment(Long groupId, CreateAllotmentRequest request) {
        BookingGroup group = findEntity(groupId);
        roomApi.getRoomTypeById(request.roomTypeId()); // 404 si le type de chambre n'existe pas
        if (request.ratePlanId() != null) {
            ratePlanApi.getById(request.ratePlanId()); // 404 si le tarif n'existe pas
        }

        GroupRoomAllotment allotment = new GroupRoomAllotment();
        allotment.setGroup(group);
        allotment.setRoomTypeId(request.roomTypeId());
        allotment.setRatePlanId(request.ratePlanId());
        allotment.setAllottedRooms(request.allottedRooms());
        allotment.setNotes(request.notes());
        group.getAllotments().add(allotment);
        return groupRepository.save(group).toSummary();
    }

    public GroupSummary updateAllotment(Long groupId, Long allotmentId, UpdateAllotmentRequest request) {
        BookingGroup group = findEntity(groupId);
        GroupRoomAllotment allotment = group.getAllotments().stream()
                .filter(a -> a.getId().equals(allotmentId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Allotement", allotmentId));
        if (request.allottedRooms() != null) allotment.setAllottedRooms(request.allottedRooms());
        if (request.notes() != null) allotment.setNotes(request.notes());
        return groupRepository.save(group).toSummary();
    }

    public void removeAllotment(Long groupId, Long allotmentId) {
        BookingGroup group = findEntity(groupId);
        group.getAllotments().removeIf(a -> a.getId().equals(allotmentId));
        groupRepository.save(group);
    }

    BookingGroup findEntity(Long id) {
        return groupRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Groupe", id));
    }
}
