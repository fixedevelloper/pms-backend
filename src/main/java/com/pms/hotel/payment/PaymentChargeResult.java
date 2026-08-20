package com.pms.hotel.payment;

public record PaymentChargeResult(boolean success, String reference, String message) {
}
