package com.hotdrop.event.dto;

import com.hotdrop.event.Event;
import com.hotdrop.event.EventStatus;

import java.time.Instant;

public record EventDto(
        Long id,
        String name,
        String description,
        Integer totalTickets,
        Integer ticketsSold,
        Integer remainingTickets,
        Instant saleStartTime,
        Integer waitingRoomOpenOffsetSeconds,
        EventStatus status,
        Instant createdAt
) {
    public static EventDto fromEntity(Event event, Instant now) {
        EventStatus effectiveStatus = event.getEffectiveStatus(now);
        int remaining = Math.max(0, event.getTotalTickets() - event.getTicketsSold());
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getTotalTickets(),
                event.getTicketsSold(),
                remaining,
                event.getSaleStartTime(),
                event.getWaitingRoomOpenOffsetSeconds(),
                effectiveStatus,
                event.getCreatedAt()
        );
    }
}
