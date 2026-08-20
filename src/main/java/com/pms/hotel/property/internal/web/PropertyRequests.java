package com.pms.hotel.property.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class PropertyRequests {

    private PropertyRequests() {
    }

    public record CreatePropertyRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "[a-z0-9-]+", message = "lettres minuscules, chiffres et tirets uniquement") String code,
            String address) {
    }

    public record UpdatePropertyRequest(
            String name,
            String address,
            Boolean active) {
    }

    public record GrantAccessRequest(@NotNull Long userId) {
    }
}
