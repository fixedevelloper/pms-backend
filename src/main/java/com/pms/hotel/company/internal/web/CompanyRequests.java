package com.pms.hotel.company.internal.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class CompanyRequests {

    private CompanyRequests() {}

    public record CreateCompanyRequest(
            @NotBlank String name,
            String address,
            @Email String contactEmail,
            String phoneNumber,
            Long negotiatedRatePlanId,
            @Pattern(regexp = "immediate|monthly") String billingCycle) {}

    public record UpdateCompanyRequest(
            String name,
            String address,
            @Email String contactEmail,
            String phoneNumber,
            Boolean active,
            Long negotiatedRatePlanId,
            @Pattern(regexp = "immediate|monthly") String billingCycle) {}
}
