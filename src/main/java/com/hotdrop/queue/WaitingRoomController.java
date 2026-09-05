package com.hotdrop.queue;

import com.hotdrop.queue.dto.WaitingRoomJoinResponse;
import com.hotdrop.queue.dto.WaitingRoomStatusResponse;
import com.hotdrop.security.SecurityUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events/{id}/waiting-room")
public class WaitingRoomController {

    private final QueueService queueService;

    public WaitingRoomController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/join")
    public ResponseEntity<WaitingRoomJoinResponse> joinWaitingRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        WaitingRoomJoinResponse response = queueService.joinWaitingRoom(id, securityUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<WaitingRoomStatusResponse> getWaitingRoomStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        WaitingRoomStatusResponse response = queueService.getWaitingRoomStatus(id, securityUser.getId());
        return ResponseEntity.ok(response);
    }
}
