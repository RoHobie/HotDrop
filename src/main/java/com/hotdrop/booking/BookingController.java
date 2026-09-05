package com.hotdrop.booking;

import com.hotdrop.booking.dto.BookingDto;
import com.hotdrop.security.SecurityUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events/{id}")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/book")
    public ResponseEntity<BookingDto> bookTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        BookingDto booking = bookingService.bookTicket(id, securityUser.getId());
        return ResponseEntity.ok(booking);
    }
}
