package com.pms.hotel.guest.internal.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class GuestRequests {

    private GuestRequests() {
    }

    public record CreateGuestRequest(
            @NotBlank @Size(max = 255) String firstName,
            @NotBlank @Size(max = 255) String lastName,
            @NotBlank @Email String email,
            @Size(max = 20) String phone,
            @Size(max = 50) String passportNumber) {
    }

    public record UpdateGuestRequest(
            @Size(max = 255) String firstName,
            @Size(max = 255) String lastName,
            @Email String email,
            @Size(max = 20) String phone,
            @Size(max = 50) String passportNumber) {
    }
}
