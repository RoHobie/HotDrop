package com.hotdrop.event;

import com.hotdrop.event.dto.CancelEventResponse;
import com.hotdrop.event.dto.CreateEventRequest;
import com.hotdrop.event.dto.EventDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventDto createEvent(CreateEventRequest request) {
        Event event = new Event(
                request.name(),
                request.description(),
                request.totalTickets(),
                request.saleStartTime(),
                request.waitingRoomOpenOffsetSeconds()
        );
        Event saved = eventRepository.save(event);
        return EventDto.fromEntity(saved, Instant.now());
    }

    @Transactional
    public CancelEventResponse cancelEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + id));

        Instant now = Instant.now();
        EventStatus currentStatus = event.getEffectiveStatus(now);

        if (currentStatus == EventStatus.CANCELLED) {
            throw new IllegalStateException("Event is already cancelled");
        }
        if (currentStatus == EventStatus.ENDED) {
            throw new IllegalStateException("Cannot cancel an event that has already ended");
        }

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        return new CancelEventResponse(event.getId(), EventStatus.CANCELLED, "Event cancelled successfully");
    }

    @Transactional(readOnly = true)
    public EventDto getEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + id));
        return EventDto.fromEntity(event, Instant.now());
    }

    @Transactional(readOnly = true)
    public Event getEventEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<EventDto> listEvents(EventStatus statusFilter) {
        Instant now = Instant.now();
        List<Event> allEvents = eventRepository.findAllByOrderBySaleStartTimeAsc();

        return allEvents.stream()
                .map(event -> EventDto.fromEntity(event, now))
                .filter(dto -> statusFilter == null || dto.status() == statusFilter)
                .toList();
    }

    @Transactional
    public void syncEventStatuses() {
        Instant now = Instant.now();

        List<Event> upcomingToLive = eventRepository.findUpcomingEventsReadyToGoLive(now);
        for (Event event : upcomingToLive) {
            if (event.getTicketsSold() >= event.getTotalTickets()) {
                event.setStatus(EventStatus.ENDED);
            } else {
                event.setStatus(EventStatus.LIVE);
            }
            eventRepository.save(event);
        }

        List<Event> liveToEnd = eventRepository.findLiveEventsReadyToEnd();
        for (Event event : liveToEnd) {
            event.setStatus(EventStatus.ENDED);
            eventRepository.save(event);
        }
    }
}
