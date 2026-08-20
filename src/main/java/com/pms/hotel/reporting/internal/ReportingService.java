package com.pms.hotel.reporting.internal;

import com.pms.hotel.booking.ActiveStay;
import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingRoomLine;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.booking.RoomOccupant;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.pos.PosApi;
import com.pms.hotel.booking.RoomStayInterval;
import com.pms.hotel.reporting.internal.ReportingViews.AuditTrailReport;
import com.pms.hotel.reporting.internal.ReportingViews.DashboardStats;
import com.pms.hotel.reporting.internal.ReportingViews.OccupancyForecastPoint;
import com.pms.hotel.reporting.internal.ReportingViews.PoliceRegisterEntry;
import com.pms.hotel.reporting.internal.ReportingViews.ProductionByChannelReport;
import com.pms.hotel.reporting.internal.ReportingViews.RateChangeEntry;
import com.pms.hotel.reporting.internal.ReportingViews.RevenueReport;
import com.pms.hotel.reporting.internal.ReportingViews.RevenueStats;
import com.pms.hotel.reporting.internal.ReportingViews.RoomStatusChangeEntry;
import com.pms.hotel.reporting.internal.web.RoomOccupancyView;
import com.pms.hotel.reporting.internal.web.RoomOccupancyView.CurrentStayView;
import com.pms.hotel.rateplan.RatePlanApi;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomOccupancyStats;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService {

    private final BookingApi bookingApi;
    private final RoomApi roomApi;
    private final PosApi posApi;
    private final GuestApi guestApi;
    private final RatePlanApi ratePlanApi;
    private final RateChangeAuditLogRepository rateChangeAuditLogRepository;

    public DashboardStats dashboardStats() {
        YearMonth month = YearMonth.now();
        var from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var to = month.atEndOfMonth().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal monthlyRevenue = bookingApi.sumRevenueForStatusCreatedBetween("confirmed", from, to);

        RoomOccupancyStats occupancy = roomApi.occupancyStats();
        int occupancyRate = occupancy.totalRooms() > 0
                ? Math.round(100f * occupancy.occupiedRooms() / occupancy.totalRooms())
                : 0;

        long activeBookings = bookingApi.countByStatus("checked_in");
        BigDecimal activeRevenue = bookingApi.sumTotalAmountByStatus("checked_in");
        BigDecimal adr = activeBookings > 0
                ? activeRevenue.divide(BigDecimal.valueOf(activeBookings), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal revPar = adr.multiply(BigDecimal.valueOf(occupancyRate)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new DashboardStats(monthlyRevenue, occupancyRate, adr, revPar);
    }

    public RevenueReport revenueReport(LocalDate start, LocalDate end) {
        var data = bookingApi.revenueByCheckoutDateBetween(start, end);
        BigDecimal totalRevenue = data.stream().map(com.pms.hotel.booking.DailyRevenuePoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long daysCount = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(start, end));
        long totalRooms = roomApi.occupancyStats().totalRooms();
        BigDecimal avgRevPar = totalRooms > 0
                ? totalRevenue.divide(BigDecimal.valueOf(daysCount * totalRooms), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new RevenueReport(data, new RevenueStats(totalRevenue, avgRevPar));
    }

    /**
     * Comme {@code RoomApi#findAll()}, mais chaque chambre occupée porte en
     * plus le séjour en cours (client, date de départ, extras impayés) —
     * utilisé par les écrans Gouvernance/Rooms qui affichent le client
     * présent, pas seulement le statut. Vit ici (et pas dans le module room)
     * car composer room+booking+pos ne peut se faire que depuis un module
     * qui dépend des trois sans être dépendu en retour — voir le javadoc de
     * RoomOccupancyView. Un appel supplémentaire par chambre occupée
     * (findActiveCheckedInStay + getById + getOutstandingTotalForBooking) :
     * acceptable pour un parc hôtelier (dizaines/centaines de chambres), à
     * revoir si ça devient un goulot d'étranglement.
     */
    public List<RoomOccupancyView> roomOccupancy() {
        return roomApi.findAll().stream().map(this::toOccupancyView).toList();
    }

    private RoomOccupancyView toOccupancyView(RoomDetails room) {
        CurrentStayView currentBooking = bookingApi.findActiveCheckedInStay(room.id())
                .map(this::toCurrentStayView)
                .orElse(null);

        return new RoomOccupancyView(
                room.id(), room.roomNumber(), room.floor(), room.status(),
                room.roomTypeId(), room.roomTypeName(), room.basePricePerNight(),
                room.externalChannelRoomId(), currentBooking);
    }

    private CurrentStayView toCurrentStayView(ActiveStay stay) {
        BookingSummary booking = bookingApi.getById(stay.bookingId());
        var outstandingExtras = posApi.getOutstandingTotalForBooking(stay.bookingId());
        return new CurrentStayView(stay.bookingId(), stay.guestId(), stay.guestFullName(), booking.checkedOutAt(), outstandingExtras);
    }

    /**
     * Occupation projetée jour par jour sur [from, to) à partir des séjours
     * déjà réservés (hors annulés/no-show) — aucune notion de yield/overbooking
     * ici, juste un comptage. totalRooms vient d'occupancyStats() (exclut déjà
     * les chambres actuellement bloquées hors-vente).
     */
    public List<OccupancyForecastPoint> occupancyForecast(LocalDate from, LocalDate to) {
        List<RoomStayInterval> stays = bookingApi.findRoomStaysOverlapping(from, to);
        long totalRooms = roomApi.occupancyStats().totalRooms();

        List<OccupancyForecastPoint> points = new ArrayList<>();
        for (LocalDate date = from; date.isBefore(to); date = date.plusDays(1)) {
            LocalDate day = date;
            long occupied = stays.stream()
                    .filter(s -> {
                        LocalDate checkIn = s.checkIn().atZone(ZoneOffset.UTC).toLocalDate();
                        LocalDate checkOut = s.checkOut().atZone(ZoneOffset.UTC).toLocalDate();
                        return !day.isBefore(checkIn) && day.isBefore(checkOut);
                    })
                    .map(RoomStayInterval::roomId)
                    .distinct()
                    .count();
            int rate = totalRooms > 0 ? Math.round(100f * occupied / totalRooms) : 0;
            points.add(new OccupancyForecastPoint(day, occupied, totalRooms, rate));
        }
        return points;
    }

    /**
     * Registre des arrivées du jour, tel qu'exigible par les autorités
     * (police/immigration) dans beaucoup de pays : le titulaire (profil
     * complet — pièce d'identité, nationalité, date de naissance, voir
     * GuestSummary) et chaque accompagnant nommé (nom/passeport seulement,
     * faute de profil complet pour eux — voir RoomOccupant).
     */
    public List<PoliceRegisterEntry> policeRegister(LocalDate date) {
        List<PoliceRegisterEntry> entries = new ArrayList<>();
        for (BookingSummary booking : bookingApi.findArrivalsOn(date)) {
            GuestSummary guest = guestApi.getById(booking.guestId());
            LocalDate arrival = booking.checkedInAt().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate departure = booking.checkedOutAt().atZone(ZoneOffset.UTC).toLocalDate();
            String roomNumbers = booking.rooms().stream().map(BookingRoomLine::roomNumber)
                    .reduce((a, b) -> a + ", " + b).orElse("");

            entries.add(new PoliceRegisterEntry(
                    booking.id(), roomNumbers, guest.lastName(), guest.firstName(), true,
                    guest.dateOfBirth(), guest.nationality(), guest.idDocumentType(), guest.idDocumentNumber(),
                    arrival, departure));

            for (BookingRoomLine room : booking.rooms()) {
                for (RoomOccupant occupant : room.occupants()) {
                    entries.add(new PoliceRegisterEntry(
                            booking.id(), room.roomNumber(), occupant.lastName(), occupant.firstName(), false,
                            null, null, null, occupant.passportNumber(),
                            arrival, departure));
                }
            }
        }
        return entries;
    }

    /** Chiffre d'affaires par canal/segment (direct, OTA, etc.) — pour identifier quel intermédiaire génère le plus de valeur. */
    public ProductionByChannelReport productionByChannel(LocalDate start, LocalDate end) {
        var bySource = bookingApi.revenueBySourceCheckedOutBetween(start, end);
        BigDecimal total = bySource.stream()
                .map(com.pms.hotel.booking.SourceRevenuePoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProductionByChannelReport(bySource, total);
    }

    /**
     * Journal d'audit des modifications de tarifs et de statuts de chambres
     * sur [from, to) — pour prévenir la fraude interne (voir
     * RateChangeAuditLog et RoomStatusLog, déjà persistés à chaque
     * changement, simplement jamais exposés en un seul rapport jusqu'ici).
     */
    public AuditTrailReport auditTrail(Instant from, Instant to) {
        List<RateChangeEntry> rateChanges = rateChangeAuditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to).stream()
                .map(log -> new RateChangeEntry(
                        log.getRatePlanId(), ratePlanApi.getById(log.getRatePlanId()).name(), log.getRoomTypeId(),
                        log.getNewPrice(), log.getChangedByUserId(), log.getCreatedAt()))
                .toList();

        List<RoomStatusChangeEntry> roomStatusChanges = roomApi.statusChangesBetween(from, to).stream()
                .map(entry -> new RoomStatusChangeEntry(
                        entry.roomId(), entry.roomNumber(), entry.status(), entry.note(), entry.updatedByUserId(), entry.changedAt()))
                .toList();

        return new AuditTrailReport(rateChanges, roomStatusChanges);
    }
}
