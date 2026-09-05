package com.hotdrop.queue.dto;

import com.hotdrop.queue.QueueEntry;
import com.hotdrop.queue.QueueStatus;

import java.time.Instant;

public record QueueStatusResponse(
        Long eventId,
        Long userId,
        QueueStatus status,
        Long queuePosition,
        long totalInQueue,
        Instant admittedAt,
        Instant admissionExpiresAt,
        Long secondsRemaining
) {
    public static QueueStatusResponse fromEntity(QueueEntry entry, long totalInQueue, Instant now) {
        Long secondsRemaining = null;
        if (entry.getStatus() == QueueStatus.ADMITTED && entry.getAdmissionExpiresAt() != null) {
            secondsRemaining = Math.max(0, entry.getAdmissionExpiresAt().getEpochSecond() - now.getEpochSecond());
        }
        return new QueueStatusResponse(
                entry.getEventId(),
                entry.getUserId(),
                entry.getStatus(),
                entry.getQueuePosition(),
                totalInQueue,
                entry.getAdmittedAt(),
                entry.getAdmissionExpiresAt(),
                secondsRemaining
        );
    }
}
