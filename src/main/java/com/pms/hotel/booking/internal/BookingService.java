package com.pms.hotel.booking.internal;

import com.pms.hotel.booking.ActiveStay;
import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingRoomLine;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.booking.DailyRevenuePoint;
import com.pms.hotel.booking.ExternalBookingUpsert;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.guest.GuestUpsertRequest;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomOccupancyStats;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService implements BookingApi {

    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final GuestApi guestApi;
    private final RoomApi roomApi;

    @Override
    @Transactional(readOnly = true)
    public BookingSummary getById(Long bookingId) {
        return toSummary(findEntity(bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ActiveStay> findActiveCheckedInStay(Long roomId) {
        return bookingRoomRepository.findActiveCheckedInBookingForRoom(roomId, java.time.Instant.now())
                .map(booking -> {
                    GuestSummary guest = guestApi.getById(booking.getGuestId());
                    return new ActiveStay(booking.getId(), guest.id(), guest.fullName());
                });
    }

    /**
     * Creates a booking, mirroring the original controller's transaction: upsert the
     * guest by phone, run the anti-overbooking check for every requested room, then
     * attach the rooms with the nightly rate captured from the room module.
     */
    public BookingSummary create(BookingCreateCommand command) {
        GuestSummary guest = guestApi.findOrCreateByPhone(new GuestUpsertRequest(
                command.firstName(), command.lastName(), command.email(), command.phone(), command.passportNumber()));

        for (Long roomId : command.roomIds()) {
            if (bookingRoomRepository.existsOverlap(roomId, command.checkIn(), command.checkOut())) {
                RoomDetails room = roomApi.getById(roomId);
                throw new BusinessRuleException(
                        "La chambre numéro " + room.roomNumber() + " est déjà réservée pour ces dates.",
                        Map.of("room_ids", List.of("La chambre numéro " + room.roomNumber() + " est déjà réservée pour ces dates.")));
            }
        }

        Booking booking = new Booking();
        booking.setGuestId(guest.id());
        booking.setCheckedInAt(command.checkIn());
        booking.setCheckedOutAt(command.checkOut());
        booking.setStatus(Booking.CONFIRMED);
        booking.setSource(command.source());
        booking.setTotalAmount(command.totalAmount());
        booking = bookingRepository.save(booking);

        for (Long roomId : command.roomIds()) {
            RoomDetails room = roomApi.getById(roomId);
            BookingRoom bookingRoom = new BookingRoom();
            bookingRoom.setBooking(booking);
            bookingRoom.setRoomId(roomId);
            bookingRoom.setPricePerNight(room.basePricePerNight());
            booking.getRooms().add(bookingRoom);
        }
        booking = bookingRepository.save(booking);

        return toSummary(booking);
    }

    public BookingSummary updateReservationStatus(Long bookingId, String status) {
        Booking booking = findEntity(bookingId);

        if (status.equals(Booking.CHECKED_OUT) && !Booking.CHECKED_IN.equals(booking.getStatus())) {
            throw new BusinessRuleException(
                    "Impossible de faire le check-out d'un client qui n'est pas encore enregistré (checked_in).");
        }

        if (status.equals(Booking.CHECKED_IN)) {
            booking.setCheckedInAt(java.time.Instant.now());
        } else if (status.equals(Booking.CHECKED_OUT)) {
            booking.setCheckedOutAt(java.time.Instant.now());
        }

        booking.setStatus(status);
        return toSummary(bookingRepository.save(booking));
    }

    @Override
    public BookingSummary upsertFromExternalChannel(ExternalBookingUpsert command) {
        Booking booking = bookingRepository.findByExternalReference(command.externalReference())
                .orElseGet(Booking::new);

        booking.setGuestId(command.guestId());
        booking.setCheckedInAt(command.checkedInAt());
        booking.setCheckedOutAt(command.checkedOutAt());
        booking.setStatus(Booking.CONFIRMED);
        booking.setSource(command.source());
        booking.setTotalAmount(command.totalAmount());
        booking.setTaxAmount(command.taxAmount() != null ? command.taxAmount() : java.math.BigDecimal.ZERO);
        booking.setExternalReference(command.externalReference());
        booking = bookingRepository.save(booking);

        if (command.roomId() != null) {
            boolean alreadyAttached = booking.getRooms().stream().anyMatch(br -> br.getRoomId().equals(command.roomId()));
            if (!alreadyAttached) {
                BookingRoom bookingRoom = new BookingRoom();
                bookingRoom.setBooking(booking);
                bookingRoom.setRoomId(command.roomId());
                bookingRoom.setPricePerNight(command.pricePerNight() != null ? command.pricePerNight() : java.math.BigDecimal.ZERO);
                booking.getRooms().add(bookingRoom);
                booking = bookingRepository.save(booking);
            }
        }

        return toSummary(booking);
    }

    @Override
    public void cancelByExternalReference(String externalReference) {
        bookingRepository.findByExternalReference(externalReference)
                .ifPresent(booking -> booking.setStatus(Booking.CANCELLED));
    }

    @Override
    public void markCheckedOut(Long bookingId) {
        findEntity(bookingId).setStatus(Booking.CHECKED_OUT);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return bookingRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumTotalAmountByStatus(String status) {
        return bookingRepository.sumTotalAmountByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumRevenueForStatusCreatedBetween(String status, java.time.Instant from, java.time.Instant to) {
        return bookingRepository.sumRevenueForStatusCreatedBetween(status, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyRevenuePoint> revenueByCheckoutDateBetween(LocalDate start, LocalDate end) {
        return bookingRepository.revenueByCheckoutDateBetween(start, end).stream()
                .map(row -> new DailyRevenuePoint((LocalDate) row[0], (BigDecimal) row[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyFlux dailyFlux(LocalDate date) {
        List<DailyFlux.StayFlux> arrivals = bookingRepository.findArrivals(date).stream().map(this::toStayFlux).toList();
        List<DailyFlux.StayFlux> departures = bookingRepository.findDepartures(date).stream().map(this::toStayFlux).toList();

        RoomOccupancyStats occupancy = roomApi.occupancyStats();
        BigDecimal dailyRevenue = bookingRepository.sumRevenueForCheckoutDate(date);
        int occupancyRate = occupancy.totalRooms() > 0
                ? Math.round(100f * occupancy.occupiedRooms() / occupancy.totalRooms())
                : 0;

        DailyFlux.Stats stats = new DailyFlux.Stats(
                occupancyRate,
                occupancy.availableRooms(),
                occupancy.totalRooms(),
                dailyRevenue != null ? dailyRevenue : BigDecimal.ZERO);

        return new DailyFlux(arrivals, departures, stats);
    }

    private DailyFlux.StayFlux toStayFlux(Booking booking) {
        GuestSummary guest = guestApi.getById(booking.getGuestId());
        String roomNumbers = booking.getRooms().stream()
                .map(br -> roomApi.getById(br.getRoomId()).roomNumber())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return new DailyFlux.StayFlux(booking.getId(), guest.fullName(), roomNumbers, booking.getStatus());
    }

    public Booking findEntity(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Réservation", id));
    }

    public BookingSummary toSummary(Booking booking) {
        List<BookingRoomLine> rooms = booking.getRooms().stream()
                .map(br -> new BookingRoomLine(br.getRoomId(), roomApi.getById(br.getRoomId()).roomNumber(), br.getPricePerNight()))
                .toList();

        return new BookingSummary(
                booking.getId(),
                booking.getGuestId(),
                booking.getStatus(),
                booking.getSource(),
                booking.getExternalReference(),
                booking.getCheckedInAt(),
                booking.getCheckedOutAt(),
                booking.getTaxAmount(),
                booking.getDiscountAmount(),
                booking.getTotalAmount(),
                rooms);
    }
}
