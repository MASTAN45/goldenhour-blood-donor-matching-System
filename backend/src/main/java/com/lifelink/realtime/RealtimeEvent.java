package com.lifelink.realtime;

import java.time.LocalDateTime;

/**
 * Envelope pushed to /user/queue/alerts.
 * type: NEW_ALERT | REQUEST_FILLED | REQUEST_CANCELLED | DONOR_ACCEPTED | DONOR_DECLINED | WAVE_EXPANDED
 */
public record RealtimeEvent(String type, Long requestId, String title, String message, Object data, LocalDateTime at) {
    public static RealtimeEvent of(String type, Long requestId, String title, String message, Object data) {
        return new RealtimeEvent(type, requestId, title, message, data, LocalDateTime.now());
    }
}