package com.hotdrop.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record CreateEventRequest(
        @NotBlank(message = "Event name is required")
        String name,

        String description,

        @NotNull(message = "Total tickets is required")
        @Positive(message = "Total tickets must be positive")
        Integer totalTickets,

        @NotNull(message = "Sale start time is required")
        @Future(message = "Sale start time must be in the future")
        Instant saleStartTime,

        @PositiveOrZero(message = "Waiting room open offset must be zero or positive")
        Integer waitingRoomOpenOffsetSeconds
) {}
