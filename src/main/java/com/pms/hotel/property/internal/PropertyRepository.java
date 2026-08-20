package com.pms.hotel.property.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByActiveTrueOrderByName();

    boolean existsByCode(String code);
}
