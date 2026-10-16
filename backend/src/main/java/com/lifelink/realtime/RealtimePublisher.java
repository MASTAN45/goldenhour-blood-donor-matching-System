package com.lifelink.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimePublisher {
    private final SimpMessagingTemplate messaging;

    /** A push failure must never break the business transaction, so errors are only logged. */
    public void toUser(Long userId, RealtimeEvent event) {
        try {
            messaging.convertAndSendToUser(String.valueOf(userId), "/queue/alerts", event);
        } catch (Exception e) {
            log.warn("Realtime push to user {} failed: {}", userId, e.toString());
        }
    }
}