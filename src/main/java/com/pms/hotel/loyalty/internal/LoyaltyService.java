package com.pms.hotel.loyalty.internal;

import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.loyalty.LoyaltyAccountView;
import com.pms.hotel.loyalty.LoyaltyApi;
import com.pms.hotel.loyalty.LoyaltyTier;
import com.pms.hotel.loyalty.LoyaltyTransactionView;
import com.pms.hotel.settings.SettingsApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Programme de fidélité : points gagnés automatiquement à la sortie d'un
 * séjour facturé (voir BookingEventListener#onBookingCheckedOut), palier
 * calculé à la volée à partir du solde de points — jamais stocké, pour
 * qu'un changement de seuil (réglages) s'applique immédiatement à tous les
 * comptes sans script de migration.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyService implements LoyaltyApi {

    private static final BigDecimal DEFAULT_POINTS_PER_1000_SPENT = BigDecimal.ONE;
    private static final long DEFAULT_GOLD_THRESHOLD = 5000;
    private static final long DEFAULT_PLATINUM_THRESHOLD = 15000;

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final GuestApi guestApi;
    private final SettingsApi settingsApi;

    @Override
    @Transactional(readOnly = true)
    public LoyaltyAccountView getAccount(Long guestId) {
        long totalPoints = accountRepository.findByGuestId(guestId).map(LoyaltyAccount::getTotalPoints).orElse(0L);
        return toView(guestId, totalPoints);
    }

    /** Appelé à la sortie d'un séjour facturé — voir BookingCheckedOutEvent. Jamais rétroactif : le taux appliqué est celui en vigueur au moment du départ. */
    public void earnPoints(Long guestId, Long bookingId, BigDecimal totalAmountSpent) {
        BigDecimal pointsPer1000 = settingsApi.get("loyalty_points_per_1000_spent")
                .map(BigDecimal::new).orElse(DEFAULT_POINTS_PER_1000_SPENT);
        long points = totalAmountSpent
                .divide(BigDecimal.valueOf(1000), 4, RoundingMode.DOWN)
                .multiply(pointsPer1000)
                .setScale(0, RoundingMode.DOWN)
                .longValue();
        if (points <= 0) {
            return;
        }
        applyTransaction(guestId, bookingId, points, LoyaltyTransaction.EARN, "Séjour #" + bookingId);
    }

    /** Ajustement manuel (geste commercial, correction) — points positifs ou négatifs. */
    public LoyaltyAccountView adjustPoints(Long guestId, long points, String description) {
        guestApi.getById(guestId); // 404 si le client n'existe pas
        if (points == 0) {
            throw new BusinessRuleException("Le nombre de points doit être différent de zéro.");
        }
        applyTransaction(guestId, null, points, LoyaltyTransaction.ADJUST, description);
        return getAccount(guestId);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionView> listTransactions(Long guestId) {
        return transactionRepository.findByGuestIdOrderByCreatedAtDesc(guestId).stream().map(LoyaltyTransaction::toView).toList();
    }

    private void applyTransaction(Long guestId, Long bookingId, long points, String type, String description) {
        LoyaltyAccount account = accountRepository.findByGuestId(guestId).orElseGet(() -> {
            LoyaltyAccount created = new LoyaltyAccount();
            created.setGuestId(guestId);
            return created;
        });
        account.setTotalPoints(account.getTotalPoints() + points);
        accountRepository.save(account);

        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setGuestId(guestId);
        transaction.setBookingId(bookingId);
        transaction.setPoints(points);
        transaction.setType(type);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }

    private LoyaltyAccountView toView(Long guestId, long totalPoints) {
        long goldThreshold = settingsApi.get("loyalty_tier_gold_threshold").map(Long::parseLong).orElse(DEFAULT_GOLD_THRESHOLD);
        long platinumThreshold = settingsApi.get("loyalty_tier_platinum_threshold").map(Long::parseLong).orElse(DEFAULT_PLATINUM_THRESHOLD);

        LoyaltyTier tier;
        LoyaltyTier nextTier;
        Long pointsToNextTier;
        if (totalPoints >= platinumThreshold) {
            tier = LoyaltyTier.PLATINUM;
            nextTier = null;
            pointsToNextTier = null;
        } else if (totalPoints >= goldThreshold) {
            tier = LoyaltyTier.GOLD;
            nextTier = LoyaltyTier.PLATINUM;
            pointsToNextTier = platinumThreshold - totalPoints;
        } else {
            tier = LoyaltyTier.SILVER;
            nextTier = LoyaltyTier.GOLD;
            pointsToNextTier = goldThreshold - totalPoints;
        }
        return new LoyaltyAccountView(guestId, totalPoints, tier, nextTier, pointsToNextTier);
    }
}
