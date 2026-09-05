package com.hotdrop.booking;

import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.booking.exception.SoldOutException;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.queue.QueueEntry;
import com.hotdrop.queue.QueueEntryRepository;
import com.hotdrop.queue.QueueStatus;
import com.hotdrop.user.Role;
import com.hotdrop.user.User;
import com.hotdrop.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    private static final int TOTAL_TICKETS = 10;
    private static final int CONCURRENT_USERS = 50;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        queueEntryRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldPreventOversellingUnderHeavyParallelBookingRequests() throws Exception {
        // 1. Create flash sale event with ONLY 10 tickets
        Event event = new Event(
                "Super Limited Flash Sale",
                "Only 10 tickets available",
                TOTAL_TICKETS,
                Instant.now().minus(1, ChronoUnit.MINUTES),
                300
        );
        event.setStatus(EventStatus.LIVE);
        event.setQueueFinalizedAt(Instant.now().minus(30, ChronoUnit.SECONDS));
        event = eventRepository.save(event);
        final Long eventId = event.getId();

        // 2. Create 50 distinct users, all admitted into the checkout window
        List<User> users = new ArrayList<>();
        Instant admissionExpiry = Instant.now().plus(5, ChronoUnit.MINUTES);

        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            User user = new User("User " + i, "buyer" + i + "@hotdrop.local", "hashedpass", Role.USER);
            user = userRepository.save(user);
            users.add(user);

            QueueEntry entry = new QueueEntry(eventId, user.getId());
            entry.setStatus(QueueStatus.ADMITTED);
            entry.setQueuePosition((long) i);
            entry.setAdmittedAt(Instant.now());
            entry.setAdmissionExpiresAt(admissionExpiry);
            queueEntryRepository.save(entry);
        }

        // 3. Concurrently hammer the booking endpoint with 50 simultaneous threads
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successfulBookings = new AtomicInteger(0);
        AtomicInteger soldOutRejections = new AtomicInteger(0);
        AtomicInteger unexpectedFailures = new AtomicInteger(0);

        for (User user : users) {
            final Long currentUserId = user.getId();
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // wait for the gun to fire all threads together
                    bookingService.bookTicket(eventId, currentUserId);
                    successfulBookings.incrementAndGet();
                } catch (SoldOutException ex) {
                    soldOutRejections.incrementAndGet();
                } catch (Exception ex) {
                    unexpectedFailures.incrementAndGet();
                }
            });
        }

        // Wait until all 50 threads are waiting at startLatch
        boolean ready = readyLatch.await(10, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        // Fire all threads at once
        startLatch.countDown();

        executor.shutdown();
        boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();

        // 4. Invariants & Strict Assertions
        assertThat(unexpectedFailures.get())
                .as("Unexpected failures occurred during concurrent booking")
                .isEqualTo(0);

        assertThat(successfulBookings.get())
                .as("Exact ticket capacity must be reserved")
                .isEqualTo(TOTAL_TICKETS);

        assertThat(soldOutRejections.get())
                .as("All excess buyers must receive sold-out rejection")
                .isEqualTo(CONCURRENT_USERS - TOTAL_TICKETS);

        // Database checks:
        Event verifiedEvent = eventRepository.findById(eventId).orElseThrow();
        assertThat(verifiedEvent.getTicketsSold())
                .as("Database tickets_sold must never exceed total_tickets")
                .isEqualTo(TOTAL_TICKETS);

        long bookingCount = bookingRepository.countByEventId(eventId);
        assertThat(bookingCount)
                .as("Database bookings count must match exact tickets sold")
                .isEqualTo(TOTAL_TICKETS);

        // All confirmed bookings must belong to distinct users
        List<Booking> allBookings = bookingRepository.findAll();
        long uniqueUsers = allBookings.stream().map(Booking::getUserId).distinct().count();
        assertThat(uniqueUsers).isEqualTo(TOTAL_TICKETS);
    }
}
