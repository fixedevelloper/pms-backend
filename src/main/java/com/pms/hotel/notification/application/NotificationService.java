package com.pms.hotel.notification.application;

import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.settings.SettingsApi;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Communication client automatisée : confirmation de réservation, relance
 * pré-arrivée et enquête post-séjour, chacune envoyée par e-mail et SMS.
 * Échoue silencieusement (log + continue) : un envoi raté ne doit jamais
 * faire échouer la transaction métier qui l'a déclenché — voir chaque
 * appelant, tous {@code @Async}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final SmsSender smsSender;
    private final SettingsApi settingsApi;

    /** Vide = pas de lien de pré-enregistrement inséré dans les e-mails (widget public non déployé/configuré). */
    @Value("${pms.public-booking.site-url:}")
    private String publicSiteUrl;

    @Async
    public void sendBookingConfirmation(GuestSummary guest, BookingSummary booking) {
        String subject = "Confirmation de votre réservation - " + hotelName();
        String body = "Bonjour " + guest.fullName() + ",\n\n"
                + "Votre réservation a bien été confirmée.\n\n"
                + stayDetails(booking)
                + checkinLinkSuffix(booking)
                + "\n\nCordialement,\nL'équipe " + hotelName();
        sendEmail(guest.email(), subject, body);
        sendSms(guest.phone(), "Réservation confirmée du " + format(booking.checkedInAt()) + " au "
                + format(booking.checkedOutAt()) + ". " + hotelName());
    }

    /** Envoyée J-2 avant l'arrivée (voir NotificationScheduler#sendPreArrivalReminders). */
    @Async
    public void sendPreArrivalReminder(GuestSummary guest, BookingSummary booking) {
        String subject = "Votre arrivée approche - " + hotelName();
        String body = "Bonjour " + guest.fullName() + ",\n\n"
                + "Nous avons hâte de vous accueillir dans deux jours !\n\n"
                + stayDetails(booking)
                + checkinLinkSuffix(booking)
                + "\n\nÀ très bientôt,\nL'équipe " + hotelName();
        sendEmail(guest.email(), subject, body);
        sendSms(guest.phone(), "Votre séjour à " + hotelName() + " commence le " + format(booking.checkedInAt()) + ". À bientôt !");
    }

    /** Vide si le pré-enregistrement est déjà fait, ou si le site public n'est pas configuré (voir publicSiteUrl). */
    private String checkinLinkSuffix(BookingSummary booking) {
        if (publicSiteUrl == null || publicSiteUrl.isBlank()
                || booking.checkinToken() == null || booking.onlineCheckinCompletedAt() != null) {
            return "";
        }
        String link = publicSiteUrl.replaceAll("/$", "") + "/checkin?bookingId=" + booking.id() + "&token=" + booking.checkinToken();
        return "\n\nGagnez du temps à votre arrivée, pré-enregistrez-vous en ligne :\n" + link + "\n";
    }

    /** Envoyée J+1 après le départ (voir NotificationScheduler#sendPostStaySurveys). */
    @Async
    public void sendPostStaySurvey(GuestSummary guest, BookingSummary booking) {
        String surveyUrl = settingsApi.get("guest_survey_url").filter(u -> !u.isBlank()).orElse(null);
        String subject = "Votre avis compte pour nous - " + hotelName();
        StringBuilder body = new StringBuilder()
                .append("Bonjour ").append(guest.fullName()).append(",\n\n")
                .append("Merci d'avoir séjourné chez nous du ").append(format(booking.checkedInAt()))
                .append(" au ").append(format(booking.checkedOutAt())).append(".\n")
                .append("Nous serions ravis de connaître votre avis sur votre séjour.\n");
        if (surveyUrl != null) {
            body.append("\nRépondez à notre enquête de satisfaction : ").append(surveyUrl).append("\n");
        }
        body.append("\nAu plaisir de vous revoir,\nL'équipe ").append(hotelName());
        sendEmail(guest.email(), subject, body.toString());
    }

    private String stayDetails(BookingSummary booking) {
        return "Numéro de réservation : " + booking.id()
                + "\nArrivée : " + format(booking.checkedInAt())
                + "\nDépart : " + format(booking.checkedOutAt());
    }

    private String format(java.time.Instant instant) {
        return instant == null ? "-" : DATE_FORMAT.format(instant.atZone(ZoneOffset.UTC));
    }

    private String hotelName() {
        return settingsApi.get("hotel_name").filter(n -> !n.isBlank()).orElse("Hestia Resort");
    }

    private void sendEmail(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("E-mail non envoyé : aucune adresse pour le destinataire. Sujet : {}", subject);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("E-mail envoyé à {} ({})", toEmail, subject);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'e-mail à {} ({})", toEmail, subject, e);
        }
    }

    private void sendSms(String toPhoneNumber, String message) {
        try {
            smsSender.send(toPhoneNumber, message);
        } catch (Exception e) {
            log.error("Échec de l'envoi du SMS à {}", toPhoneNumber, e);
        }
    }
}
