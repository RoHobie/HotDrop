package com.hotdrop.queue;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueAdmissionScheduler {

    private final AdmissionService admissionService;

    public QueueAdmissionScheduler(AdmissionService admissionService) {
        this.admissionService = admissionService;
    }

    @Scheduled(fixedDelayString = "${hotdrop.queue.admission-poll-rate-ms:3000}")
    public void runAdmissionJob() {
        admissionService.admitNextBatch();
    }
}
