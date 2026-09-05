package com.hotdrop.event;

import com.hotdrop.event.dto.CancelEventResponse;
import com.hotdrop.event.dto.CreateEventRequest;
import com.hotdrop.event.dto.EventDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventDto created = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<CancelEventResponse> cancelEvent(@PathVariable Long id) {
        CancelEventResponse response = eventService.cancelEvent(id);
        return ResponseEntity.ok(response);
    }
}
