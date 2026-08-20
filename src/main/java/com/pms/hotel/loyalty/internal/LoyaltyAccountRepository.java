package com.pms.hotel.loyalty.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    Optional<LoyaltyAccount> findByGuestId(Long guestId);
}
