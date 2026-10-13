package com.lifelink.service;

import com.lifelink.entity.NotificationChannel;
import com.lifelink.entity.NotificationLog;
import com.lifelink.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationLogRepository notifications;

    /** Runs only after the accept transaction has committed, in its own transaction. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRequestFilled(RequestFilledEvent event) {
        for (Long donorId : event.closedDonorIds()) {
            notifications.save(NotificationLog.builder()
                    .userId(donorId).requestId(event.requestId())
                    .channel(NotificationChannel.WEBSOCKET)
                    .title("Request filled")
                    .message("Thank you! This blood request has been filled by other donors.")
                    .build());
        }
        notifications.save(NotificationLog.builder()
                .userId(event.requesterId()).requestId(event.requestId())
                .channel(NotificationChannel.WEBSOCKET)
                .title("Request filled")
                .message("All required units now have a confirmed donor.")
                .build());
    }
}