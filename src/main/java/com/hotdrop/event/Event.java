package com.hotdrop.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;

    @Column(name = "tickets_sold", nullable = false)
    private Integer ticketsSold = 0;

    @Column(name = "sale_start_time", nullable = false)
    private Instant saleStartTime;

    @Column(name = "waiting_room_open_offset_seconds", nullable = false)
    private Integer waitingRoomOpenOffsetSeconds = 900;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.UPCOMING;

    @Column(name = "queue_finalized_at")
    private Instant queueFinalizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Event() {
    }

    public Event(String name, String description, Integer totalTickets, Instant saleStartTime, Integer waitingRoomOpenOffsetSeconds) {
        this.name = name;
        this.description = description;
        this.totalTickets = totalTickets;
        this.ticketsSold = 0;
        this.saleStartTime = saleStartTime;
        this.waitingRoomOpenOffsetSeconds = waitingRoomOpenOffsetSeconds != null ? waitingRoomOpenOffsetSeconds : 900;
        this.status = EventStatus.UPCOMING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = EventStatus.UPCOMING;
        }
        if (this.ticketsSold == null) {
            this.ticketsSold = 0;
        }
        if (this.waitingRoomOpenOffsetSeconds == null) {
            this.waitingRoomOpenOffsetSeconds = 900;
        }
    }

    public EventStatus getEffectiveStatus(Instant now) {
        if (this.status == EventStatus.CANCELLED) {
            return EventStatus.CANCELLED;
        }
        if (this.ticketsSold >= this.totalTickets) {
            return EventStatus.ENDED;
        }
        if (now.isAfter(this.saleStartTime) || now.equals(this.saleStartTime)) {
            return EventStatus.LIVE;
        }
        return EventStatus.UPCOMING;
    }

    public boolean isWaitingRoomOpen(Instant now) {
        Instant waitingRoomOpenTime = this.saleStartTime.minusSeconds(this.waitingRoomOpenOffsetSeconds);
        return !now.isBefore(waitingRoomOpenTime);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(Integer totalTickets) {
        this.totalTickets = totalTickets;
    }

    public Integer getTicketsSold() {
        return ticketsSold;
    }

    public void setTicketsSold(Integer ticketsSold) {
        this.ticketsSold = ticketsSold;
    }

    public Instant getSaleStartTime() {
        return saleStartTime;
    }

    public void setSaleStartTime(Instant saleStartTime) {
        this.saleStartTime = saleStartTime;
    }

    public Integer getWaitingRoomOpenOffsetSeconds() {
        return waitingRoomOpenOffsetSeconds;
    }

    public void setWaitingRoomOpenOffsetSeconds(Integer waitingRoomOpenOffsetSeconds) {
        this.waitingRoomOpenOffsetSeconds = waitingRoomOpenOffsetSeconds;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Instant getQueueFinalizedAt() {
        return queueFinalizedAt;
    }

    public void setQueueFinalizedAt(Instant queueFinalizedAt) {
        this.queueFinalizedAt = queueFinalizedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
