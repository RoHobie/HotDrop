package com.hotdrop.event.dto;

import com.hotdrop.event.EventStatus;

public record CancelEventResponse(
        Long id,
        EventStatus status,
        String message
) {}
