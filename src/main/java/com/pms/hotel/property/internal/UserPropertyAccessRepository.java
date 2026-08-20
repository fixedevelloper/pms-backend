package com.pms.hotel.property.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPropertyAccessRepository extends JpaRepository<UserPropertyAccess, Long> {

    /** Au plus une ligne par utilisateur — voir la contrainte unique(user_id), un membre du personnel n'est jamais affecté qu'à un seul établissement. */
    Optional<UserPropertyAccess> findByUserId(Long userId);

    List<UserPropertyAccess> findByPropertyId(Long propertyId);
}
