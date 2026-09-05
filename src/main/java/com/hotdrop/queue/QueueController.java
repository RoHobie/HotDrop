package com.hotdrop.queue;

import com.hotdrop.queue.dto.QueueStatusResponse;
import com.hotdrop.security.SecurityUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events/{id}/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        QueueStatusResponse response = queueService.getQueueStatus(id, securityUser.getId());
        return ResponseEntity.ok(response);
    }
}
