package com.hotdrop.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    long countByEventIdAndStatus(Long eventId, QueueStatus status);

    long countByEventId(Long eventId);

    @Query("SELECT COALESCE(MAX(q.queuePosition), 0) FROM QueueEntry q WHERE q.eventId = :eventId")
    long findMaxQueuePositionByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query(value = """
        WITH shuffled AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY random()) as pos
            FROM queue_entries
            WHERE event_id = :eventId AND status = 'WAITING'
        )
        UPDATE queue_entries q
        SET queue_position = s.pos, status = 'QUEUED'
        FROM shuffled s
        WHERE q.id = s.id
        """, nativeQuery = true)
    int randomizeWaitingQueue(@Param("eventId") Long eventId);

    @Query(value = """
        SELECT id FROM queue_entries
        WHERE event_id = :eventId AND status = 'QUEUED'
        ORDER BY queue_position ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Long> findNextQueuedIdsForAdmission(@Param("eventId") Long eventId, @Param("batchSize") int batchSize);

    @Query("SELECT q FROM QueueEntry q WHERE q.status = com.hotdrop.queue.QueueStatus.ADMITTED AND q.admissionExpiresAt < :now")
    List<QueueEntry> findExpiredAdmissions(@Param("now") Instant now);
}
