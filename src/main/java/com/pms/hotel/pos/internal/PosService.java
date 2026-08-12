package com.pms.hotel.pos.internal;

import com.pms.hotel.booking.ActiveStay;
import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.pos.ExtraChargeAddedEvent;
import com.pms.hotel.pos.ExtraChargeLine;
import com.pms.hotel.pos.PosApi;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomStatuses;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PosService implements PosApi {

    private final ExtraChargeRepository extraChargeRepository;
    private final RoomApi roomApi;
    private final BookingApi bookingApi;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<ExtraChargeLine> getChargesForBooking(Long bookingId) {
        return extraChargeRepository.findByBookingId(bookingId).stream().map(ExtraCharge::toLine).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getOutstandingTotalForBooking(Long bookingId) {
        return extraChargeRepository.sumOutstandingForBooking(bookingId);
    }

    /** Step 1 at the POS terminal: confirm the room is occupied by a checked-in guest today. */
    @Transactional(readOnly = true)
    public RoomCheckResult checkRoomStatus(String roomNumber) {
        RoomDetails room = roomApi.getByRoomNumber(roomNumber);
        if (!RoomStatuses.OCCUPIED.equals(room.status())) {
            throw new ResourceNotFoundException(
                    "La chambre " + roomNumber + " n'est pas marquée occupée sur le planning hôtelier.");
        }

        ActiveStay stay = bookingApi.findActiveCheckedInStay(room.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun client n'est actuellement enregistré (Checked-in) dans la chambre " + roomNumber + "."));

        return new RoomCheckResult(stay.bookingId(), stay.guestFullName(), room.id());
    }

    /** Step 2 at the POS terminal: bill the order to the room's folio. */
    public ExtraChargeLine chargeToRoom(Long bookingId, String department, String itemName, int quantity,
            BigDecimal unitPrice, String externalOrderId) {
        bookingApi.getById(bookingId); // 404s if the booking doesn't exist

        ExtraCharge charge = new ExtraCharge();
        charge.setBookingId(bookingId);
        charge.setDepartment(department);
        charge.setItemName(itemName);
        charge.setQuantity(quantity);
        charge.setUnitPrice(unitPrice);
        charge.setExternalOrderId(externalOrderId);
        charge.setPaymentStatus(ExtraCharge.CHARGED_TO_ROOM);
        charge = extraChargeRepository.save(charge);

        events.publishEvent(new ExtraChargeAddedEvent(bookingId, charge.getTotalPrice()));

        return charge.toLine();
    }

    public record RoomCheckResult(Long bookingId, String guestName, Long roomId) {
    }
}
