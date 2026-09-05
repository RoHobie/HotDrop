package com.hotdrop.booking.dto;

import com.hotdrop.booking.Booking;
import com.hotdrop.booking.BookingStatus;

import java.time.Instant;

public record BookingDto(
        Long bookingId,
        Long eventId,
        String eventName,
        Long userId,
        BookingStatus status,
        Instant bookedAt
) {
    public static BookingDto fromEntity(Booking booking, String eventName) {
        return new BookingDto(
                booking.getId(),
                booking.getEventId(),
                eventName,
                booking.getUserId(),
                booking.getStatus(),
                booking.getBookedAt()
        );
    }
}
