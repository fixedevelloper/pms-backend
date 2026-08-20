package com.pms.hotel.billing.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyInvoiceRepository extends JpaRepository<CompanyInvoice, Long> {

    List<CompanyInvoice> findByCompanyIdOrderByPeriodStartDesc(Long companyId);
}
