package com.hotdrop.queue.dto;

import com.hotdrop.queue.QueueStatus;

import java.time.Instant;

public record WaitingRoomStatusResponse(
        Long eventId,
        Long userId,
        QueueStatus status,
        Instant joinedAt,
        Instant saleStartTime,
        long secondsUntilSale
) {}
