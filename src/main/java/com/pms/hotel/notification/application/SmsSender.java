package com.pms.hotel.notification.application;

/**
 * Extension point for SMS delivery. {@link LoggingSmsSender} is the only
 * implementation today (no SMS provider is wired up yet) — plugging a real
 * provider (Twilio, Orange/MTN Mobile Money SMS gateway, etc.) means adding a
 * new {@code @Component} implementing this interface and removing/qualifying
 * the logging one, without touching {@link NotificationService} or the
 * scheduler that call it.
 */
public interface SmsSender {

    void send(String toPhoneNumber, String message);
}
