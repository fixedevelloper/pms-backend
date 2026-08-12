package com.pms.hotel.room;

import java.util.Set;

/** The room housekeeping/occupancy statuses, as defined by the {@code rooms.status} CHECK constraint. */
public final class RoomStatuses {

    public static final String AVAILABLE = "available";
    public static final String OCCUPIED = "occupied";
    public static final String DIRTY = "dirty";
    public static final String MAINTENANCE = "maintenance";

    public static final Set<String> ALL = Set.of(AVAILABLE, OCCUPIED, DIRTY, MAINTENANCE);

    private RoomStatuses() {
    }
}
