package com.lifelink.realtime;

/** kind: DONOR_ACCEPTED | DONOR_DECLINED | WAVE_EXPANDED. Delivered to the hospital that owns the request. */
public record RequestProgressEvent(Long requestId, Long requesterId, String kind, int unitsAccepted, int unitsNeeded,
                                   String status, Integer radiusKm, Integer newlyNotified) {
}