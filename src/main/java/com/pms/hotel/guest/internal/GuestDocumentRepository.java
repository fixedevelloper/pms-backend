package com.pms.hotel.guest.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestDocumentRepository extends JpaRepository<GuestDocument, Long> {

    List<GuestDocument> findByGuestIdOrderByCreatedAtDesc(Long guestId);
}
