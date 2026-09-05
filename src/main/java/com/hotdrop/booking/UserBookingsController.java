package com.hotdrop.booking;

import com.hotdrop.booking.dto.BookingDto;
import com.hotdrop.security.SecurityUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/me")
public class UserBookingsController {

    private final BookingService bookingService;

    public UserBookingsController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingDto>> getMyBookings(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<BookingDto> bookings = bookingService.getUserBookings(securityUser.getId());
        return ResponseEntity.ok(bookings);
    }
}
