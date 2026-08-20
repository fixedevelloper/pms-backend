package com.pms.hotel.room.internal.web;

import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomBlockView;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomTypeSummary;
import com.pms.hotel.room.internal.Room;
import com.pms.hotel.room.internal.RoomRepository;
import com.pms.hotel.room.internal.RoomService;
import com.pms.hotel.room.internal.RoomType;
import com.pms.hotel.room.internal.RoomTypeRepository;
import com.pms.hotel.room.internal.web.RoomRequests.CreateRoomBlockRequest;
import com.pms.hotel.room.internal.web.RoomRequests.CreateRoomRequest;
import com.pms.hotel.room.internal.web.RoomRequests.CreateRoomTypeRequest;
import com.pms.hotel.room.internal.web.RoomRequests.UpdateRoomStatusRequest;
import com.pms.hotel.room.internal.web.RoomRequests.UpdateRoomTypeRequest;
import com.pms.hotel.property.CurrentProperty;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import com.pms.hotel.shared.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class RoomController {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomApi roomApi;
    private final RoomService roomService;
    private final CurrentUser currentUser;
    private final CurrentProperty currentProperty;

    @GetMapping("/room-types")
    public List<RoomTypeSummary> indexTypes() {
        return roomTypeRepository.findByPropertyId(currentProperty.resolve(), Sort.by("name")).stream().map(RoomType::toSummary).toList();
    }

    @PostMapping("/room-types")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeSummary storeType(@Valid @RequestBody CreateRoomTypeRequest request) {
        RoomType roomType = new RoomType();
        roomType.setPropertyId(currentProperty.resolve());
        roomType.setName(request.name());
        roomType.setDescription(request.description());
        roomType.setBaseCapacity(request.baseCapacity());
        roomType.setBasePrice(request.basePrice());
        return roomTypeRepository.save(roomType).toSummary();
    }

    @PutMapping("/room-types/{id}")
    public RoomTypeSummary updateType(@PathVariable Long id, @Valid @RequestBody UpdateRoomTypeRequest request) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Type de chambre", id));

        if (request.name() != null) roomType.setName(request.name());
        if (request.description() != null) roomType.setDescription(request.description());
        if (request.baseCapacity() != null) roomType.setBaseCapacity(request.baseCapacity());
        if (request.basePrice() != null) roomType.setBasePrice(request.basePrice());

        return roomTypeRepository.save(roomType).toSummary();
    }

    @GetMapping("/rooms")
    public List<RoomDetails> index() {
        return roomRepository.findByPropertyId(currentProperty.resolve(), Sort.by("roomNumber")).stream().map(Room::toSummary).toList();
    }

    // GET /rooms/occupancy vit désormais dans le module reporting (RoomOccupancyController) —
    // sa composition room+booking+pos créait un cycle Spring Modulith depuis ce module-ci.

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDetails storeRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Type de chambre", request.roomTypeId()));
        if (!roomType.getPropertyId().equals(currentProperty.resolve())) {
            throw new BusinessRuleException("Ce type de chambre n'appartient pas à l'établissement courant.");
        }

        Room room = new Room();
        room.setRoomType(roomType);
        room.setRoomNumber(request.roomNumber());
        room.setFloor(request.floor());
        room.setStatus(request.status());
        return roomRepository.save(room).toSummary();
    }

    @PatchMapping("/rooms/{id}/status")
    @PreAuthorize("hasAuthority('manage housekeeping') or hasAuthority('manage rooms')")
    public RoomDetails updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateRoomStatusRequest request) {
        return roomService.updateStatusWithChecklist(
                id, request.status(), currentUser.userId(), request.note(),
                Boolean.TRUE.equals(request.linenChecked()),
                Boolean.TRUE.equals(request.bathroomChecked()),
                Boolean.TRUE.equals(request.minibarChecked()));
    }

    @GetMapping("/rooms/blocks")
    public List<RoomBlockView> indexAllBlocks() {
        return roomService.listAllBlocks(currentProperty.resolve());
    }

    @GetMapping("/rooms/{id}/blocks")
    public List<RoomBlockView> indexBlocks(@PathVariable Long id) {
        return roomService.listBlocksForRoom(id);
    }

    @PostMapping("/rooms/{id}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('manage rooms')")
    public RoomBlockView storeBlock(@PathVariable Long id, @Valid @RequestBody CreateRoomBlockRequest request) {
        return roomService.createBlock(id, request.startDate(), request.endDate(), request.reason(), request.notes());
    }

    @DeleteMapping("/rooms/{id}/blocks/{blockId}")
    @PreAuthorize("hasAuthority('manage rooms')")
    public void releaseBlock(@PathVariable Long id, @PathVariable Long blockId) {
        roomService.releaseBlock(id, blockId);
    }
}
