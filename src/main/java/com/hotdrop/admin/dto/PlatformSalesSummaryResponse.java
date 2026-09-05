package com.hotdrop.admin.dto;

public record PlatformSalesSummaryResponse(
        long totalEvents,
        long totalTicketsAvailable,
        long totalTicketsSold,
        long totalBookingsConfirmed,
        long activeEventsCount
) {}
