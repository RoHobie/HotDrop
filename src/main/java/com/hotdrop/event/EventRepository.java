package com.hotdrop.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatus(EventStatus status);

    List<Event> findAllByOrderBySaleStartTimeAsc();

    @Query("SELECT e FROM Event e WHERE e.status = 'UPCOMING' AND e.saleStartTime <= :now")
    List<Event> findUpcomingEventsReadyToGoLive(@Param("now") Instant now);

    @Query("SELECT e FROM Event e WHERE e.status = 'LIVE' AND e.ticketsSold >= e.totalTickets")
    List<Event> findLiveEventsReadyToEnd();

    @Query("SELECT e FROM Event e WHERE e.saleStartTime <= :now AND e.queueFinalizedAt IS NULL AND e.status != 'CANCELLED'")
    List<Event> findEventsNeedingQueueFinalization(@Param("now") Instant now);

    @Modifying
    @Query(value = "UPDATE events SET tickets_sold = tickets_sold + 1 WHERE id = :eventId AND tickets_sold < total_tickets", nativeQuery = true)
    int incrementTicketsSoldAtomically(@Param("eventId") Long eventId);
}
