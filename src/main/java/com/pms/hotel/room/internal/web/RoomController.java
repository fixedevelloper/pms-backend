package com.pms.hotel.room.internal.web;

import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomTypeSummary;
import com.pms.hotel.room.internal.Room;
import com.pms.hotel.room.internal.RoomRepository;
import com.pms.hotel.room.internal.RoomType;
import com.pms.hotel.room.internal.RoomTypeRepository;
import com.pms.hotel.room.internal.web.RoomRequests.CreateRoomRequest;
import com.pms.hotel.room.internal.web.RoomRequests.CreateRoomTypeRequest;
import com.pms.hotel.room.internal.web.RoomRequests.UpdateRoomStatusRequest;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import com.pms.hotel.shared.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final CurrentUser currentUser;

    @GetMapping("/room-types")
    public List<RoomTypeSummary> indexTypes() {
        return roomTypeRepository.findAll(Sort.by("name")).stream().map(RoomType::toSummary).toList();
    }

    @PostMapping("/room-types")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeSummary storeType(@Valid @RequestBody CreateRoomTypeRequest request) {
        RoomType roomType = new RoomType();
        roomType.setName(request.name());
        roomType.setDescription(request.description());
        roomType.setBaseCapacity(request.baseCapacity());
        roomType.setBasePrice(request.basePrice());
        return roomTypeRepository.save(roomType).toSummary();
    }

    @GetMapping("/rooms")
    public List<RoomDetails> index() {
        return roomRepository.findAll(Sort.by("roomNumber")).stream().map(Room::toSummary).toList();
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDetails storeRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Type de chambre", request.roomTypeId()));

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
        return roomApi.updateStatus(id, request.status(), currentUser.userId(), request.note());
    }
}
