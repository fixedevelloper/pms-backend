package com.pms.hotel.rateplan;

import java.math.BigDecimal;

/**
 * Publié par RatePlanService quand pricePerNight change — s'applique à
 * toutes les chambres du type concerné. changedByUserId trace l'auteur pour
 * le journal d'audit du reporting (peut être null pour un ancien événement
 * rejoué ou une origine système).
 */
public record RatePlanPriceChangedEvent(Long ratePlanId, Long roomTypeId, BigDecimal pricePerNight, Long changedByUserId) {
}
