package com.pms.hotel.company;

public record CompanySummary(
        Long id,
        String name,
        String address,
        String contactEmail,
        String phoneNumber,
        boolean active,
        Long negotiatedRatePlanId,
        String billingCycle) {
}
