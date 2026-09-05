package com.hotdrop.queue;

import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class QueueFinalizationScheduler {

    private final EventRepository eventRepository;
    private final QueueService queueService;

    public QueueFinalizationScheduler(EventRepository eventRepository, QueueService queueService) {
        this.eventRepository = eventRepository;
        this.queueService = queueService;
    }

    @Scheduled(fixedDelayString = "${hotdrop.queue.status-scheduler-rate-ms:2000}")
    public void runQueueFinalization() {
        Instant now = Instant.now();
        List<Event> eventsNeedingFinalization = eventRepository.findEventsNeedingQueueFinalization(now);

        for (Event event : eventsNeedingFinalization) {
            queueService.finalizeQueue(event.getId());
        }
    }
}
