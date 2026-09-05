package com.hotdrop.queue.dto;

import com.hotdrop.queue.QueueStatus;

import java.time.Instant;

public record WaitingRoomJoinResponse(
        Long eventId,
        Long userId,
        QueueStatus status,
        Instant joinedAt
) {}
