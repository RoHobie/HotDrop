package com.hotdrop.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    Optional<Booking> findByUserIdAndEventId(Long userId, Long eventId);

    long countByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, BookingStatus status);

    long countByStatus(BookingStatus status);
}
