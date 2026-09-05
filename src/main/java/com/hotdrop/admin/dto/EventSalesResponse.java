package com.hotdrop.admin.dto;

import com.hotdrop.event.EventStatus;

public record EventSalesResponse(
        Long eventId,
        String eventName,
        int totalTickets,
        int ticketsSold,
        int remainingTickets,
        EventStatus status,
        long totalInQueue,
        long completedBookings,
        double percentSold
) {}
