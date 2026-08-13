package com.pms.hotel.nightaudit.internal;

import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.booking.DailyRevenuePoint;
import com.pms.hotel.nightaudit.NightAuditRunResult;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomOccupancyStats;
import com.pms.hotel.settings.SettingsApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The nightly closing process: process no-shows for the outgoing business
 * date (a no-show is treated as a zero-notice cancellation, reusing
 * BookingService's own cancellation-fee logic), snapshot occupancy/revenue,
 * then advance the business date. Idempotent per date — running it twice for
 * the same business date is rejected, not silently repeated.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NightAuditService {

    private static final Logger log = LoggerFactory.getLogger(NightAuditService.class);
    private static final String AUTO_RUN_SETTING_KEY = "night_audit_auto";

    private final NightAuditStateRepository stateRepository;
    private final NightAuditRunRepository runRepository;
    private final BookingApi bookingApi;
    private final RoomApi roomApi;
    private final SettingsApi settingsApi;

    @Transactional(readOnly = true)
    public LocalDate currentBusinessDate() {
        return getOrCreateState().getBusinessDate();
    }

    @Transactional(readOnly = true)
    public Optional<NightAuditRunResult> lastRun() {
        return runRepository.findFirstByOrderByBusinessDateDesc().map(NightAuditRun::toResult);
    }

    @Transactional(readOnly = true)
    public List<NightAuditRunResult> history(int limit) {
        return runRepository.findAllByOrderByBusinessDateDesc().stream().limit(limit).map(NightAuditRun::toResult).toList();
    }

    public NightAuditRunResult run() {
        NightAuditState state = getOrCreateState();
        LocalDate businessDate = state.getBusinessDate();

        if (runRepository.existsByBusinessDate(businessDate)) {
            throw new BusinessRuleException("Le night audit du " + businessDate + " a déjà été exécuté.");
        }

        List<BookingSummary> noShowCandidates = bookingApi.findNoShowCandidates(businessDate);
        BigDecimal noShowFees = BigDecimal.ZERO;
        for (BookingSummary candidate : noShowCandidates) {
            BookingSummary updated = bookingApi.markNoShow(candidate.id());
            if (updated.cancellationFeeAmount() != null) {
                noShowFees = noShowFees.add(updated.cancellationFeeAmount());
            }
        }

        RoomOccupancyStats occupancy = roomApi.occupancyStats();
        BigDecimal revenue = bookingApi.revenueByCheckoutDateBetween(businessDate, businessDate).stream()
                .map(DailyRevenuePoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NightAuditRun run = new NightAuditRun();
        run.setBusinessDate(businessDate);
        run.setRanAt(Instant.now());
        run.setOccupiedRooms((int) occupancy.occupiedRooms());
        run.setTotalRevenue(revenue);
        run.setNoShowsProcessed(noShowCandidates.size());
        run.setNoShowFeesTotal(noShowFees);
        run = runRepository.save(run);

        state.setBusinessDate(businessDate.plusDays(1));
        stateRepository.save(state);

        return run.toResult();
    }

    /**
     * Ne se déclenche que si l'employé a explicitement activé "night_audit_auto"
     * dans Paramètres Hôtel — ce réglage existait déjà côté frontend mais
     * n'avait jamais aucun effet réel. 3h du matin, heure du serveur.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void autoRunIfEnabled() {
        if (!settingsApi.isEnabled(AUTO_RUN_SETTING_KEY)) {
            return;
        }
        try {
            NightAuditRunResult result = run();
            log.info("Night audit automatique exécuté pour {} : {} no-show(s), {} FCFA de revenu constaté.",
                    result.businessDate(), result.noShowsProcessed(), result.totalRevenue());
        } catch (BusinessRuleException e) {
            log.warn("Night audit automatique ignoré : {}", e.getMessage());
        }
    }

    private NightAuditState getOrCreateState() {
        return stateRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            NightAuditState state = new NightAuditState();
            state.setBusinessDate(LocalDate.now());
            return stateRepository.save(state);
        });
    }
}
