package com.pms.hotel.housekeeping.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lost_found_items")
public class LostFoundItem extends BaseEntity {

    public static final String STORED = "stored";
    public static final String CLAIMED = "claimed";
    public static final String DISPOSED = "disposed";

    @Column(name = "room_id")
    private Long roomId;

    @Column(nullable = false)
    private String description;

    @Column(name = "found_location")
    private String foundLocation;

    @Column(name = "found_by")
    private Long foundBy;

    @Column(nullable = false)
    private String status = STORED;

    @Column(name = "claimant_name")
    private String claimantName;

    private String notes;
}
