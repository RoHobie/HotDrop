package com.hotdrop.event;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventLifecycleScheduler {

    private final EventService eventService;

    public EventLifecycleScheduler(EventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(fixedDelayString = "${hotdrop.queue.status-scheduler-rate-ms:2000}")
    public void runLifecycleSync() {
        eventService.syncEventStatuses();
    }
}
