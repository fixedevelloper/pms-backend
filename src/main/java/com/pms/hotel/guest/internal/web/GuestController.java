package com.pms.hotel.guest.internal.web;

import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.guest.internal.Guest;
import com.pms.hotel.guest.internal.GuestRepository;
import com.pms.hotel.guest.internal.web.GuestRequests.CreateGuestRequest;
import com.pms.hotel.guest.internal.web.GuestRequests.UpdateGuestRequest;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import com.pms.hotel.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guests")
@RequiredArgsConstructor
class GuestController {

    private final GuestRepository guestRepository;

    @GetMapping
    public PageResponse<GuestSummary> index(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return PageResponse.of(guestRepository.search(search, pageable), Guest::toSummary);
    }

    @GetMapping("/search")
    public List<GuestSummary> quickSearch(@RequestParam("q") String query) {
        Pageable topTen = PageRequest.of(0, 10, Sort.by("lastName"));
        return guestRepository.search(query, topTen).map(Guest::toSummary).getContent();
    }

    @GetMapping("/{id}")
    public GuestSummary show(@PathVariable Long id) {
        return findEntity(id).toSummary();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuestSummary store(@Valid @RequestBody CreateGuestRequest request) {
        Guest guest = new Guest();
        guest.setFirstName(request.firstName());
        guest.setLastName(request.lastName());
        guest.setEmail(request.email());
        guest.setPhone(request.phone());
        guest.setPassportNumber(request.passportNumber());
        return guestRepository.save(guest).toSummary();
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuestSummary> update(@PathVariable Long id, @Valid @RequestBody UpdateGuestRequest request) {
        Guest guest = findEntity(id);
        if (request.firstName() != null) guest.setFirstName(request.firstName());
        if (request.lastName() != null) guest.setLastName(request.lastName());
        if (request.email() != null) guest.setEmail(request.email());
        if (request.phone() != null) guest.setPhone(request.phone());
        if (request.passportNumber() != null) guest.setPassportNumber(request.passportNumber());
        return ResponseEntity.ok(guestRepository.save(guest).toSummary());
    }

    private Guest findEntity(Long id) {
        return guestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Client", id));
    }
}
