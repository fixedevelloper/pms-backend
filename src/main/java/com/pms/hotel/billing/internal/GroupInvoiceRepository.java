package com.pms.hotel.billing.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInvoiceRepository extends JpaRepository<GroupInvoice, Long> {

    List<GroupInvoice> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
