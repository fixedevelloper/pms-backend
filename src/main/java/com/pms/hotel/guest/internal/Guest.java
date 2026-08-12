package com.pms.hotel.guest.internal;

import com.pms.hotel.guest.GuestSummary;
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
@Table(name = "guests")
public class Guest extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(name = "passport_number")
    private String passportNumber;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public GuestSummary toSummary() {
        return new GuestSummary(getId(), firstName, lastName, getFullName(), email, phone, passportNumber);
    }
}
