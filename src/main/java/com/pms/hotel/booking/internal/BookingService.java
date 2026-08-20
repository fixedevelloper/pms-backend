package com.pms.hotel.booking.internal;

import com.pms.hotel.booking.ActiveStay;
import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingAvailabilityChangedEvent;
import com.pms.hotel.booking.BookingDepositCollectedEvent;
import com.pms.hotel.booking.BookingRoomLine;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.booking.DailyRevenuePoint;
import com.pms.hotel.booking.ExternalBookingUpsert;
import com.pms.hotel.booking.RoomOccupant;
import com.pms.hotel.booking.RoomStayInterval;
import com.pms.hotel.company.CompanyApi;
import com.pms.hotel.groupbooking.GroupBookingApi;
import com.pms.hotel.groupbooking.GroupSummary;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.property.PropertyApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.guest.GuestUpsertRequest;
import com.pms.hotel.rateplan.RatePlanApi;
import com.pms.hotel.rateplan.RatePlanSummary;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomOccupancyStats;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ForbiddenActionException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final RatePlanApi ratePlanApi;
    private final CompanyApi companyApi;
    private final GroupBookingApi groupBookingApi;
    private final PropertyApi propertyApi;
    private final ApplicationEventPublisher events;

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

        LocalDate checkInDate = command.checkIn().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate checkOutDate = command.checkOut().atZone(ZoneOffset.UTC).toLocalDate();

        if (Booking.GUARANTEE_COMPANY.equals(command.guaranteeType())) {
            if (command.companyId() == null) {
                throw new BusinessRuleException("Une société garante est requise pour une garantie de type \"company\".");
            }
            companyApi.getById(command.companyId()); // 404 si la société n'existe pas
        }

        if (command.groupId() != null) {
            GroupSummary group = groupBookingApi.getById(command.groupId()); // 404 si le groupe n'existe pas
            if (!group.propertyId().equals(command.propertyId())) {
                throw new BusinessRuleException("Ce groupe n'appartient pas à l'établissement de cette réservation.");
            }
        }

        for (BookingCreateCommand.RoomAllocation allocation : command.rooms()) {
            RoomDetails room = roomApi.getById(allocation.roomId());
            if (!room.propertyId().equals(command.propertyId())) {
                throw new BusinessRuleException(
                        "La chambre numéro " + room.roomNumber() + " n'appartient pas à l'établissement de cette réservation.",
                        Map.of("room_ids", List.of("La chambre numéro " + room.roomNumber() + " n'appartient pas à l'établissement de cette réservation.")));
            }
            if (bookingRoomRepository.existsOverlap(allocation.roomId(), command.checkIn(), command.checkOut())) {
                throw new BusinessRuleException(
                        "La chambre numéro " + room.roomNumber() + " est déjà réservée pour ces dates.",
                        Map.of("room_ids", List.of("La chambre numéro " + room.roomNumber() + " est déjà réservée pour ces dates.")));
            }
            if (roomApi.isBlocked(allocation.roomId(), checkInDate, checkOutDate)) {
                throw new BusinessRuleException(
                        "La chambre numéro " + room.roomNumber() + " est bloquée (hors-vente) pour ces dates.",
                        Map.of("room_ids", List.of("La chambre numéro " + room.roomNumber() + " est bloquée (hors-vente) pour ces dates.")));
            }
        }

        Booking booking = new Booking();
        booking.setPropertyId(command.propertyId());
        booking.setGuestId(guest.id());
        booking.setCheckedInAt(command.checkIn());
        booking.setCheckedOutAt(command.checkOut());
        booking.setStatus(Booking.CONFIRMED);
        booking.setSource(command.source());
        booking.setGuaranteeType(command.guaranteeType());
        booking.setCompanyId(command.companyId());
        booking.setGroupId(command.groupId());
        booking.setCheckinToken(UUID.randomUUID().toString());
        booking.setDepositAmount(command.depositAmount());
        booking.setTotalAmount(command.totalAmount());
        booking = bookingRepository.save(booking);

        // Le prix par nuit vient du rate plan choisi, pas d'un prix de base statique
        // (RoomType#basePrice n'est plus utilisé qu'en absence de rate plan applicable).
        for (BookingCreateCommand.RoomAllocation allocation : command.rooms()) {
            RatePlanSummary ratePlan = ratePlanApi.getById(allocation.ratePlanId());
            BookingRoom bookingRoom = new BookingRoom();
            bookingRoom.setBooking(booking);
            bookingRoom.setRoomId(allocation.roomId());
            bookingRoom.setRatePlanId(allocation.ratePlanId());
            bookingRoom.setPricePerNight(ratePlan.pricePerNight());
            bookingRoom.setAdultsCount(allocation.adultsCount() != null ? allocation.adultsCount() : 1);
            bookingRoom.setChildrenCount(allocation.childrenCount() != null ? allocation.childrenCount() : 0);
            if (allocation.occupants() != null) {
                for (BookingCreateCommand.OccupantInput occupantInput : allocation.occupants()) {
                    BookingRoomOccupant occupant = new BookingRoomOccupant();
                    occupant.setBookingRoom(bookingRoom);
                    occupant.setFirstName(occupantInput.firstName());
                    occupant.setLastName(occupantInput.lastName());
                    occupant.setPassportNumber(occupantInput.passportNumber());
                    bookingRoom.getOccupants().add(occupant);
                }
            }
            booking.getRooms().add(bookingRoom);
        }
        booking = bookingRepository.save(booking);

        if (command.depositAmount() != null && command.depositAmount().signum() > 0) {
            events.publishEvent(new BookingDepositCollectedEvent(booking.getId(), command.depositAmount()));
        }

        for (BookingCreateCommand.RoomAllocation allocation : command.rooms()) {
            events.publishEvent(new BookingAvailabilityChangedEvent(allocation.roomId(), checkInDate, checkOutDate, "booking_created"));
        }
        
        events.publishEvent(new com.pms.hotel.booking.BookingCreatedEvent(booking.getId()));

        return toSummary(booking);
    }

    public BookingSummary updateReservationStatus(Long bookingId, String status) {
        Booking booking = findEntity(bookingId);

        if (status.equals(Booking.CHECKED_OUT) && !Booking.CHECKED_IN.equals(booking.getStatus())) {
            throw new BusinessRuleException(
                    "Impossible de faire le check-out d'un client qui n'est pas encore enregistré (checked_in).");
        }

        if (status.equals(Booking.CANCELLED)
                && List.of(Booking.CHECKED_IN, Booking.CHECKED_OUT).contains(booking.getStatus())) {
            throw new BusinessRuleException("Impossible d'annuler un séjour déjà en cours ou terminé.");
        }

        if (status.equals(Booking.CHECKED_IN)) {
            booking.setCheckedInAt(java.time.Instant.now());
        } else if (status.equals(Booking.CHECKED_OUT)) {
            booking.setCheckedOutAt(java.time.Instant.now());
        } else if (status.equals(Booking.CANCELLED) && booking.getCancellationFeeAmount() == null) {
            booking.setCancellationFeeAmount(computeCancellationFee(booking));
        }

        booking.setStatus(status);
        BookingSummary summary = toSummary(bookingRepository.save(booking));

        if (status.equals(Booking.CANCELLED)) {
            LocalDate checkInDate = booking.getCheckedInAt().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate checkOutDate = booking.getCheckedOutAt().atZone(ZoneOffset.UTC).toLocalDate();
            for (BookingRoom room : booking.getRooms()) {
                events.publishEvent(new BookingAvailabilityChangedEvent(room.getRoomId(), checkInDate, checkOutDate, "booking_cancelled"));
            }
        }

        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummary> findNoShowCandidates(LocalDate onOrBeforeDate) {
        return bookingRepository.findNoShowCandidates(onOrBeforeDate).stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummary> findByCompanyCheckedOutBetween(Long companyId, LocalDate from, LocalDate to) {
        return bookingRepository.findByCompanyCheckedOutBetween(companyId, from, to).stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummary> findCheckedOutOn(LocalDate date) {
        return bookingRepository.findCheckedOutOn(date).stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummary> findByGroupId(Long groupId) {
        return bookingRepository.findByGroupIdOrderByCreatedAtAsc(groupId).stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.pms.hotel.booking.SourceRevenuePoint> revenueBySourceCheckedOutBetween(LocalDate start, LocalDate end) {
        return bookingRepository.revenueBySourceCheckedOutBetween(start, end).stream()
                .map(row -> new com.pms.hotel.booking.SourceRevenuePoint((String) row[0], (Long) row[1], (BigDecimal) row[2]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummary> findArrivalsOn(LocalDate date) {
        return bookingRepository.findArrivals(date).stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomStayInterval> findRoomStaysOverlapping(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.atStartOfDay(ZoneOffset.UTC).toInstant();
        return bookingRoomRepository.findOverlapping(fromInstant, toInstant).stream()
                .map(br -> new RoomStayInterval(br.getRoomId(), br.getBooking().getCheckedInAt(), br.getBooking().getCheckedOutAt()))
                .toList();
    }

    @Override
    public BookingSummary markNoShow(Long bookingId) {
        Booking booking = findEntity(bookingId);
        if (!List.of(Booking.PENDING, Booking.CONFIRMED).contains(booking.getStatus())) {
            throw new BusinessRuleException(
                    "Seule une réservation en attente ou confirmée peut être marquée no-show (statut actuel : " + booking.getStatus() + ").");
        }

        // Même calcul que l'annulation : un no-show est une annulation à délai
        // nul, ChronoUnit.DAYS.between(now, checkedInAt) y sera négatif ou nul,
        // donc toujours hors délai gratuit pour une politique flexible.
        if (booking.getCancellationFeeAmount() == null) {
            booking.setCancellationFeeAmount(computeCancellationFee(booking));
        }
        booking.setStatus(Booking.NO_SHOW);
        return toSummary(bookingRepository.save(booking));
    }

    /**
     * Additionne, par chambre, ce que la politique d'annulation de son rate
     * plan autorise à retenir : intégral pour non_refundable, le pourcentage
     * défini pour partial_refund, et pour flexible seulement ce qui dépasse
     * le délai gratuit — plafonné au dépôt réellement perçu, faute de
     * passerelle de paiement pour prélever davantage sans moyen enregistré.
     * Jamais plus que la valeur totale de la réservation.
     */
    private BigDecimal computeCancellationFee(Booking booking) {
        long stayNights = Math.max(1, ChronoUnit.DAYS.between(booking.getCheckedInAt(), booking.getCheckedOutAt()));
        long daysUntilCheckIn = ChronoUnit.DAYS.between(Instant.now(), booking.getCheckedInAt());

        BigDecimal enforceableFee = BigDecimal.ZERO;
        BigDecimal lateFlexibleExposure = BigDecimal.ZERO;

        for (BookingRoom room : booking.getRooms()) {
            if (room.getRatePlanId() == null) continue; // pas de politique connue (réservation historique / channel manager)
            RatePlanSummary plan = ratePlanApi.getById(room.getRatePlanId());
            BigDecimal roomTotal = room.getPricePerNight().multiply(BigDecimal.valueOf(stayNights));

            switch (plan.cancellationPolicy()) {
                case "non_refundable" -> enforceableFee = enforceableFee.add(roomTotal);
                case "partial_refund" -> {
                    BigDecimal percent = plan.cancellationFeePercent() != null ? plan.cancellationFeePercent() : BigDecimal.ZERO;
                    enforceableFee = enforceableFee.add(
                            roomTotal.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }
                case "flexible" -> {
                    Integer freeDays = plan.freeCancellationDays();
                    boolean withinFreeWindow = freeDays != null && daysUntilCheckIn >= freeDays;
                    if (!withinFreeWindow) {
                        lateFlexibleExposure = lateFlexibleExposure.add(roomTotal);
                    }
                }
                default -> { }
            }
        }

        BigDecimal depositForfeit = booking.getDepositAmount().min(lateFlexibleExposure);
        return enforceableFee.add(depositForfeit).min(booking.getTotalAmount());
    }

    @Override
    public BookingSummary upsertFromExternalChannel(ExternalBookingUpsert command) {
        Booking booking = bookingRepository.findByExternalReference(command.externalReference())
                .orElseGet(Booking::new);

        if (booking.getId() == null) {
            // Cloisonnement multi-propriété du channel manager non traité (canaux/mappings
            // encore globaux) : la propriété se déduit de la chambre si connue, sinon repli sur
            // le premier établissement actif — correct pour un déploiement mono-établissement.
            booking.setPropertyId(command.roomId() != null
                    ? roomApi.getById(command.roomId()).propertyId()
                    : propertyApi.findAllActivePropertyIds().stream().findFirst()
                            .orElseThrow(() -> new BusinessRuleException("Aucun établissement configuré.")));
        }

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
        Booking booking = findEntity(bookingId);
        booking.setStatus(Booking.CHECKED_OUT);
        events.publishEvent(new com.pms.hotel.booking.BookingCheckedOutEvent(booking.getId(), booking.getGuestId(), booking.getTotalAmount()));
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
        return new DailyFlux.StayFlux(
                booking.getId(), guest.fullName(), roomNumbers, booking.getStatus(), booking.getOnlineCheckinCompletedAt() != null);
    }

    public Booking findEntity(Long id) {
        return bookingRepository.findWithDetailsById(id).orElseThrow(() -> ResourceNotFoundException.of("Réservation", id));
    }

    /** Valide le jeton de pré-enregistrement en ligne — voir booking.internal.web.CheckinPublicController, jamais authentifié. */
    public Booking findEntityForCheckin(Long id, String token) {
        Booking booking = findEntity(id);
        if (token == null || booking.getCheckinToken() == null || !booking.getCheckinToken().equals(token)) {
            throw new ForbiddenActionException("Lien de pré-enregistrement invalide.");
        }
        return booking;
    }

    public BookingSummary completeOnlineCheckin(Long id, String token) {
        Booking booking = findEntityForCheckin(id, token);
        booking.setOnlineCheckinCompletedAt(Instant.now());
        return toSummary(bookingRepository.save(booking));
    }

    public BookingSummary toSummary(Booking booking) {
        List<BookingRoomLine> rooms = booking.getRooms().stream()
                .map(br -> {
                    String ratePlanName = br.getRatePlanId() != null ? ratePlanApi.getById(br.getRatePlanId()).name() : null;
                    List<RoomOccupant> occupants = br.getOccupants().stream()
                            .map(o -> new RoomOccupant(o.getFirstName(), o.getLastName(), o.getPassportNumber()))
                            .toList();
                    return new BookingRoomLine(
                            br.getRoomId(), roomApi.getById(br.getRoomId()).roomNumber(), br.getPricePerNight(),
                            br.getRatePlanId(), ratePlanName, br.getAdultsCount(), br.getChildrenCount(), occupants);
                })
                .toList();

        return new BookingSummary(
                booking.getId(),
                booking.getPropertyId(),
                booking.getGuestId(),
                booking.getCompanyId(),
                booking.getGroupId(),
                booking.getStatus(),
                booking.getSource(),
                booking.getGuaranteeType(),
                booking.getDepositAmount(),
                booking.getCancellationFeeAmount(),
                booking.getExternalReference(),
                booking.getCheckedInAt(),
                booking.getCheckedOutAt(),
                booking.getTaxAmount(),
                booking.getDiscountAmount(),
                booking.getTotalAmount(),
                rooms,
                booking.getCheckinToken(),
                booking.getOnlineCheckinCompletedAt());
    }
}
