package com.pms.hotel.billing.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, Long> {

    List<BankStatementLine> findByStatusOrderByTransactionDateDesc(String status);

    List<BankStatementLine> findAllByOrderByTransactionDateDesc();
}
