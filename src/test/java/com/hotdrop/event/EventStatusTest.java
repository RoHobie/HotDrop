package com.hotdrop.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventStatusTest {

    @Test
    void shouldBeUpcomingBeforeSaleStart() {
        Instant now = Instant.parse("2026-09-05T12:00:00Z");
        Instant saleStart = Instant.parse("2026-09-05T14:00:00Z");

        Event event = new Event("Concert", "Description", 100, saleStart, 900);
        assertThat(event.getEffectiveStatus(now)).isEqualTo(EventStatus.UPCOMING);
    }

    @Test
    void shouldBeLiveAtOrAfterSaleStart() {
        Instant now = Instant.parse("2026-09-05T14:00:00Z");
        Instant saleStart = Instant.parse("2026-09-05T14:00:00Z");

        Event event = new Event("Concert", "Description", 100, saleStart, 900);
        assertThat(event.getEffectiveStatus(now)).isEqualTo(EventStatus.LIVE);

        Instant after = Instant.parse("2026-09-05T14:05:00Z");
        assertThat(event.getEffectiveStatus(after)).isEqualTo(EventStatus.LIVE);
    }

    @Test
    void shouldBeEndedWhenTicketsSoldOut() {
        Instant now = Instant.parse("2026-09-05T14:05:00Z");
        Instant saleStart = Instant.parse("2026-09-05T14:00:00Z");

        Event event = new Event("Concert", "Description", 100, saleStart, 900);
        event.setTicketsSold(100);

        assertThat(event.getEffectiveStatus(now)).isEqualTo(EventStatus.ENDED);
    }

    @Test
    void cancelledIsTerminalState() {
        Instant now = Instant.parse("2026-09-05T14:05:00Z");
        Instant saleStart = Instant.parse("2026-09-05T14:00:00Z");

        Event event = new Event("Concert", "Description", 100, saleStart, 900);
        event.setStatus(EventStatus.CANCELLED);

        assertThat(event.getEffectiveStatus(now)).isEqualTo(EventStatus.CANCELLED);
    }

    @Test
    void shouldDetermineWaitingRoomWindowCorrectly() {
        Instant saleStart = Instant.parse("2026-09-05T14:00:00Z");
        Event event = new Event("Concert", "Description", 100, saleStart, 600); // 10 minutes offset -> 13:50:00Z

        Instant beforeWindow = Instant.parse("2026-09-05T13:49:59Z");
        Instant windowOpen = Instant.parse("2026-09-05T13:50:00Z");
        Instant duringWindow = Instant.parse("2026-09-05T13:55:00Z");

        assertThat(event.isWaitingRoomOpen(beforeWindow)).isFalse();
        assertThat(event.isWaitingRoomOpen(windowOpen)).isTrue();
        assertThat(event.isWaitingRoomOpen(duringWindow)).isTrue();
    }
}
