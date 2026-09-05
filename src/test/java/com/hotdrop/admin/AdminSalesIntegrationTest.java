package com.hotdrop.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.booking.Booking;
import com.hotdrop.booking.BookingRepository;
import com.hotdrop.booking.BookingStatus;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.queue.QueueEntry;
import com.hotdrop.queue.QueueEntryRepository;
import com.hotdrop.queue.QueueStatus;
import com.hotdrop.security.JwtService;
import com.hotdrop.user.Role;
import com.hotdrop.user.User;
import com.hotdrop.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminSalesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String adminToken;
    private String userToken;
    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        queueEntryRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Admin & User
        User admin = new User("Admin", "admin@hotdrop.local", "hashed", Role.ADMIN);
        admin = userRepository.save(admin);
        adminToken = jwtService.generateToken(admin);

        User regular = new User("User", "user@hotdrop.local", "hashed", Role.USER);
        regular = userRepository.save(regular);
        userToken = jwtService.generateToken(regular);

        // 2. Create Event 1 (100 total, 25 sold)
        event1 = new Event("Festival One", "Desc 1", 100, Instant.now().minus(1, ChronoUnit.HOURS), 300);
        event1.setStatus(EventStatus.LIVE);
        event1.setTicketsSold(25);
        event1 = eventRepository.save(event1);

        // Add 25 bookings for Event 1
        for (int i = 1; i <= 25; i++) {
            User buyer = new User("Buyer " + i, "b" + i + "@hotdrop.local", "hashed", Role.USER);
            buyer = userRepository.save(buyer);
            bookingRepository.save(new Booking(buyer.getId(), event1.getId(), BookingStatus.CONFIRMED));

            QueueEntry q = new QueueEntry(event1.getId(), buyer.getId());
            q.setStatus(QueueStatus.COMPLETED);
            queueEntryRepository.save(q);
        }

        // 3. Create Event 2 (50 total, 0 sold)
        event2 = new Event("Festival Two", "Desc 2", 50, Instant.now().plus(2, ChronoUnit.HOURS), 300);
        event2.setStatus(EventStatus.UPCOMING);
        event2.setTicketsSold(0);
        event2 = eventRepository.save(event2);
    }

    @Test
    void adminCanQueryPerEventSales() throws Exception {
        mockMvc.perform(get("/admin/events/" + event1.getId() + "/sales")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(event1.getId()))
                .andExpect(jsonPath("$.eventName").value("Festival One"))
                .andExpect(jsonPath("$.totalTickets").value(100))
                .andExpect(jsonPath("$.ticketsSold").value(25))
                .andExpect(jsonPath("$.remainingTickets").value(75))
                .andExpect(jsonPath("$.totalInQueue").value(25))
                .andExpect(jsonPath("$.completedBookings").value(25))
                .andExpect(jsonPath("$.percentSold").value(25.0));
    }

    @Test
    void adminCanQuerySalesSummary() throws Exception {
        mockMvc.perform(get("/admin/sales/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(2))
                .andExpect(jsonPath("$.totalTicketsAvailable").value(150))
                .andExpect(jsonPath("$.totalTicketsSold").value(25))
                .andExpect(jsonPath("$.totalBookingsConfirmed").value(25))
                .andExpect(jsonPath("$.activeEventsCount").value(2));
    }

    @Test
    void nonAdminCannotAccessSalesEndpoints() throws Exception {
        mockMvc.perform(get("/admin/events/" + event1.getId() + "/sales")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/sales/summary")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/sales/summary"))
                .andExpect(status().isForbidden());
    }
}
