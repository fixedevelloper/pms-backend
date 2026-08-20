package com.pms.hotel.loyalty.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    List<LoyaltyTransaction> findByGuestIdOrderByCreatedAtDesc(Long guestId);
}
