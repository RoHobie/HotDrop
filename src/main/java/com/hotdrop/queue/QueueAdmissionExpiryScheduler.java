package com.hotdrop.queue;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueAdmissionExpiryScheduler {

    private final AdmissionService admissionService;

    public QueueAdmissionExpiryScheduler(AdmissionService admissionService) {
        this.admissionService = admissionService;
    }

    @Scheduled(fixedDelayString = "${hotdrop.queue.admission-poll-rate-ms:3000}")
    public void runExpiryJob() {
        admissionService.expireLapsedAdmissions();
    }
}
