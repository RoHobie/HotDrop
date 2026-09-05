package com.hotdrop.queue;

import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AdmissionService {

    private final QueueEntryRepository queueEntryRepository;
    private final EventRepository eventRepository;
    private final int batchSize;
    private final long validitySeconds;

    public AdmissionService(
            QueueEntryRepository queueEntryRepository,
            EventRepository eventRepository,
            @Value("${hotdrop.queue.admission-batch-size:50}") int batchSize,
            @Value("${hotdrop.queue.admission-validity-seconds:120}") long validitySeconds
    ) {
        this.queueEntryRepository = queueEntryRepository;
        this.eventRepository = eventRepository;
        this.batchSize = batchSize;
        this.validitySeconds = validitySeconds;
    }

    @Transactional
    public int admitNextBatchForEvent(Long eventId, int customBatchSize) {
        int limit = customBatchSize > 0 ? customBatchSize : this.batchSize;
        List<Long> entryIds = queueEntryRepository.findNextQueuedIdsForAdmission(eventId, limit);
        if (entryIds.isEmpty()) {
            return 0;
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(validitySeconds);

        List<QueueEntry> entries = queueEntryRepository.findAllById(entryIds);
        for (QueueEntry entry : entries) {
            entry.setStatus(QueueStatus.ADMITTED);
            entry.setAdmittedAt(now);
            entry.setAdmissionExpiresAt(expiresAt);
        }
        queueEntryRepository.saveAll(entries);
        return entries.size();
    }

    @Transactional
    public void admitNextBatch() {
        List<Event> liveEvents = eventRepository.findByStatus(EventStatus.LIVE);
        for (Event event : liveEvents) {
            admitNextBatchForEvent(event.getId(), this.batchSize);
        }
    }

    @Transactional
    public int expireLapsedAdmissions() {
        Instant now = Instant.now();
        List<QueueEntry> expired = queueEntryRepository.findExpiredAdmissions(now);
        for (QueueEntry entry : expired) {
            entry.setStatus(QueueStatus.EXPIRED);
        }
        queueEntryRepository.saveAll(expired);
        return expired.size();
    }

    @Transactional
    public QueueEntry validateAndGetActiveAdmission(Long eventId, Long userId) {
        QueueEntry entry = queueEntryRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new IllegalStateException("User is not in the queue for this event"));

        Instant now = Instant.now();

        if (entry.getStatus() == QueueStatus.WAITING || entry.getStatus() == QueueStatus.QUEUED) {
            throw new IllegalStateException("It is not your turn yet. Current queue position: " + entry.getQueuePosition());
        }

        if (entry.getStatus() == QueueStatus.EXPIRED) {
            throw new IllegalStateException("Your admission window has expired. Please rejoin the queue");
        }

        if (entry.getStatus() == QueueStatus.COMPLETED) {
            throw new IllegalStateException("You have already booked a ticket for this event");
        }

        if (entry.getStatus() == QueueStatus.ADMITTED) {
            if (entry.getAdmissionExpiresAt() != null && entry.getAdmissionExpiresAt().isBefore(now)) {
                entry.setStatus(QueueStatus.EXPIRED);
                queueEntryRepository.save(entry);
                throw new IllegalStateException("Your admission window has expired. Please rejoin the queue");
            }
            return entry;
        }

        throw new IllegalStateException("Invalid queue state: " + entry.getStatus());
    }

    public int getBatchSize() {
        return batchSize;
    }

    public long getValiditySeconds() {
        return validitySeconds;
    }
}
