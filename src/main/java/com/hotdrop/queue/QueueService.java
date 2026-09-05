package com.hotdrop.queue;

import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.queue.dto.WaitingRoomJoinResponse;
import com.hotdrop.queue.dto.WaitingRoomStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final EventRepository eventRepository;

    public QueueService(QueueEntryRepository queueEntryRepository, EventRepository eventRepository) {
        this.queueEntryRepository = queueEntryRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public WaitingRoomJoinResponse joinWaitingRoom(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

        Instant now = Instant.now();
        EventStatus effectiveStatus = event.getEffectiveStatus(now);

        if (effectiveStatus == EventStatus.CANCELLED) {
            throw new IllegalStateException("Cannot join waiting room for a cancelled event");
        }
        if (effectiveStatus == EventStatus.ENDED) {
            throw new IllegalStateException("Cannot join waiting room for an ended event");
        }
        if (!event.isWaitingRoomOpen(now)) {
            throw new IllegalStateException("Waiting room is not open yet. Sale starts at " + event.getSaleStartTime());
        }

        Optional<QueueEntry> existing = queueEntryRepository.findByEventIdAndUserId(eventId, userId);
        if (existing.isPresent()) {
            QueueEntry entry = existing.get();
            return new WaitingRoomJoinResponse(eventId, userId, entry.getStatus(), entry.getJoinedAt());
        }

        QueueEntry entry = new QueueEntry(eventId, userId);

        // If the queue for this event has already been randomized, late joiners append to the end as QUEUED
        if (event.getQueueFinalizedAt() != null) {
            long nextPos = queueEntryRepository.findMaxQueuePositionByEventId(eventId) + 1;
            entry.setQueuePosition(nextPos);
            entry.setStatus(QueueStatus.QUEUED);
        } else {
            entry.setStatus(QueueStatus.WAITING);
            entry.setQueuePosition(null);
        }

        QueueEntry saved = queueEntryRepository.save(entry);
        return new WaitingRoomJoinResponse(eventId, userId, saved.getStatus(), saved.getJoinedAt());
    }

    @Transactional(readOnly = true)
    public WaitingRoomStatusResponse getWaitingRoomStatus(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

        QueueEntry entry = queueEntryRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NoSuchElementException("User is not in the waiting room for event: " + eventId));

        long secondsUntilSale = Math.max(0, event.getSaleStartTime().getEpochSecond() - Instant.now().getEpochSecond());

        return new WaitingRoomStatusResponse(
                eventId,
                userId,
                entry.getStatus(),
                entry.getJoinedAt(),
                event.getSaleStartTime(),
                secondsUntilSale
        );
    }

    @Transactional(readOnly = true)
    public com.hotdrop.queue.dto.QueueStatusResponse getQueueStatus(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

        QueueEntry entry = queueEntryRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NoSuchElementException("User is not in the queue for event: " + eventId));

        long totalInQueue = queueEntryRepository.countByEventId(eventId);
        return com.hotdrop.queue.dto.QueueStatusResponse.fromEntity(entry, totalInQueue, Instant.now());
    }

    @Transactional
    public boolean finalizeQueue(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

        if (event.getQueueFinalizedAt() != null || event.getStatus() == EventStatus.CANCELLED) {
            return false;
        }

        queueEntryRepository.randomizeWaitingQueue(eventId);

        Instant now = Instant.now();
        event.setQueueFinalizedAt(now);
        if (event.getStatus() == EventStatus.UPCOMING) {
            event.setStatus(EventStatus.LIVE);
        }
        eventRepository.save(event);
        return true;
    }
}
