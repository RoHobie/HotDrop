package com.hotdrop.admin;

import com.hotdrop.admin.dto.EventSalesResponse;
import com.hotdrop.admin.dto.PlatformSalesSummaryResponse;
import com.hotdrop.booking.BookingRepository;
import com.hotdrop.booking.BookingStatus;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.queue.QueueEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminSalesService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final QueueEntryRepository queueEntryRepository;

    public AdminSalesService(
            EventRepository eventRepository,
            BookingRepository bookingRepository,
            QueueEntryRepository queueEntryRepository
    ) {
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
        this.queueEntryRepository = queueEntryRepository;
    }

    @Transactional(readOnly = true)
    public EventSalesResponse getEventSales(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

        int total = event.getTotalTickets();
        int sold = event.getTicketsSold();
        int remaining = Math.max(0, total - sold);
        long inQueue = queueEntryRepository.countByEventId(eventId);
        long completed = bookingRepository.countByEventIdAndStatus(eventId, BookingStatus.CONFIRMED);
        double percentSold = total > 0 ? ((double) sold / total) * 100.0 : 0.0;

        EventStatus effectiveStatus = event.getEffectiveStatus(Instant.now());

        return new EventSalesResponse(
                event.getId(),
                event.getName(),
                total,
                sold,
                remaining,
                effectiveStatus,
                inQueue,
                completed,
                Math.round(percentSold * 100.0) / 100.0
        );
    }

    @Transactional(readOnly = true)
    public PlatformSalesSummaryResponse getPlatformSalesSummary() {
        List<Event> allEvents = eventRepository.findAll();
        Instant now = Instant.now();

        long totalEvents = allEvents.size();
        long totalTicketsAvailable = allEvents.stream().mapToLong(Event::getTotalTickets).sum();
        long totalTicketsSold = allEvents.stream().mapToLong(Event::getTicketsSold).sum();
        long totalBookingsConfirmed = bookingRepository.countByStatus(BookingStatus.CONFIRMED);

        long activeEventsCount = allEvents.stream()
                .filter(e -> {
                    EventStatus s = e.getEffectiveStatus(now);
                    return s == EventStatus.UPCOMING || s == EventStatus.LIVE;
                })
                .count();

        return new PlatformSalesSummaryResponse(
                totalEvents,
                totalTicketsAvailable,
                totalTicketsSold,
                totalBookingsConfirmed,
                activeEventsCount
        );
    }
}
