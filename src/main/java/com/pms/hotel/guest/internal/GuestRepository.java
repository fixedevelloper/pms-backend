package com.pms.hotel.guest.internal;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    Optional<Guest> findByPhone(String phone);

    Optional<Guest> findByEmail(String email);

    @Query("""
            select g from Guest g
            where (:search is null
                or lower(g.lastName) like lower(concat('%', :search, '%'))
                or lower(g.firstName) like lower(concat('%', :search, '%'))
                or lower(g.email) like lower(concat('%', :search, '%'))
                or lower(g.phone) like lower(concat('%', :search, '%'))
                or lower(g.passportNumber) like lower(concat('%', :search, '%')))
            order by g.lastName
            """)
    Page<Guest> search(@Param("search") String search, Pageable pageable);
}
