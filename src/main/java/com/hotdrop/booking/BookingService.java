package com.hotdrop.booking;

import com.hotdrop.booking.dto.BookingDto;
import com.hotdrop.booking.exception.SoldOutException;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.queue.AdmissionService;
import com.hotdrop.queue.QueueEntry;
import com.hotdrop.queue.QueueEntryRepository;
import com.hotdrop.queue.QueueStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final AdmissionService admissionService;

    public BookingService(
            BookingRepository bookingRepository,
            EventRepository eventRepository,
            QueueEntryRepository queueEntryRepository,
            AdmissionService admissionService
    ) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.admissionService = admissionService;
    }

    @Transactional
    public BookingDto bookTicket(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

        // 1. Verify user holds active admission
        QueueEntry queueEntry = admissionService.validateAndGetActiveAdmission(eventId, userId);

        // 2. Prevent duplicate booking per user
        if (bookingRepository.findByUserIdAndEventId(userId, eventId).isPresent()) {
            throw new IllegalStateException("You have already booked a ticket for this event");
        }

        // 3. Atomically increment tickets_sold ONLY if tickets_sold < total_tickets
        int rowsUpdated = eventRepository.incrementTicketsSoldAtomically(eventId);
        if (rowsUpdated == 0) {
            // Check if sold out and mark ENDED
            Event currentEvent = eventRepository.findById(eventId).orElse(event);
            if (currentEvent.getTicketsSold() >= currentEvent.getTotalTickets()) {
                currentEvent.setStatus(EventStatus.ENDED);
                eventRepository.save(currentEvent);
            }
            throw new SoldOutException("Event is sold out");
        }

        // 4. Record confirmed booking
        Booking booking = new Booking(userId, eventId, BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        // 5. Mark queue entry as COMPLETED
        queueEntry.setStatus(QueueStatus.COMPLETED);
        queueEntryRepository.save(queueEntry);

        return BookingDto.fromEntity(savedBooking, event.getName());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookedAtDesc(userId);
        return bookings.stream()
                .map(b -> {
                    String eventName = eventRepository.findById(b.getEventId())
                            .map(Event::getName)
                            .orElse("Unknown Event");
                    return BookingDto.fromEntity(b, eventName);
                })
                .toList();
    }
}
