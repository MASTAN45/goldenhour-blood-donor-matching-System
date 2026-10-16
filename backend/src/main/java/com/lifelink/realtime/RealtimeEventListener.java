package com.lifelink.realtime;

import com.lifelink.dto.RequestDtos.NearbyView;
import com.lifelink.service.BloodLabel;
import com.lifelink.service.RequestFilledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Pushes only AFTER the database transaction has committed. */
@Component
@RequiredArgsConstructor
public class RealtimeEventListener {
    private final RealtimePublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAlerts(AlertsCreatedEvent e) {
        for (AlertsCreatedEvent.Target t : e.targets()) {
            NearbyView v = t.view();
            publisher.toUser(t.donorUserId(), RealtimeEvent.of("NEW_ALERT", v.requestId(),
                    v.urgency() + ": " + BloodLabel.of(v.bloodGroup()) + " blood needed",
                    v.unitsNeeded() + " unit(s) at " + v.hospitalName() + ", " + v.distanceKm() + " km away", v));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProgress(RequestProgressEvent e) {
        String title;
        String msg;
        switch (e.kind()) {
            case "DONOR_ACCEPTED" -> {
                title = "A donor accepted";
                msg = e.unitsAccepted() + " of " + e.unitsNeeded() + " units covered";
            }
            case "DONOR_DECLINED" -> {
                title = "A donor declined";
                msg = "Still searching: " + e.unitsAccepted() + " of " + e.unitsNeeded() + " units covered";
            }
            default -> {
                title = "Search widened to " + e.radiusKm() + " km";
                msg = e.newlyNotified() + " more donor(s) alerted";
            }
        }
        publisher.toUser(e.requesterId(), RealtimeEvent.of(e.kind(), e.requestId(), title, msg,
                new ProgressData(e.unitsAccepted(), e.unitsNeeded(), e.status(), e.radiusKm(), e.newlyNotified())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onFilled(RequestFilledEvent e) {
        for (Long donorId : e.closedDonorIds()) {
            publisher.toUser(donorId, RealtimeEvent.of("REQUEST_FILLED", e.requestId(), "Request filled",
                    "Thank you for being ready. Other donors have covered this request.", null));
        }
        publisher.toUser(e.requesterId(), RealtimeEvent.of("REQUEST_FILLED", e.requestId(), "All units covered",
                "Every required unit now has a confirmed donor.", null));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onClosed(RequestClosedEvent e) {
        for (Long donorId : e.donorIds()) {
            publisher.toUser(donorId, RealtimeEvent.of(e.type(), e.requestId(), "Request cancelled",
                    "The hospital cancelled this request. Thank you for being ready.", null));
        }
    }
}