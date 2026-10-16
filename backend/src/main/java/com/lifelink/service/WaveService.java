package com.lifelink.service;

import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.RequestStatus;
import com.lifelink.realtime.RequestProgressEvent;
import com.lifelink.repository.BloodRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Expanding-radius waves (for example 5 km, then 10 km, then 25 km) and request expiry. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaveService {
    private final BloodRequestRepository requests;
    private final MatchingService matching;
    private final ApplicationEventPublisher events;

    @Value("${lifelink.matching.waves:5,10,25}")
    private String wavesCsv;
    @Value("${lifelink.matching.wave-interval-minutes:2}")
    private int intervalMinutes;

    @Transactional(readOnly = true)
    public List<Long> activeRequestIds() {
        return requests.findByStatusInAndExpiresAtAfter(
                        List.of(RequestStatus.OPEN, RequestStatus.PARTIALLY_FILLED), LocalDateTime.now())
                .stream().map(BloodRequest::getId).toList();
    }

    /** Each request runs in its own transaction, so one failure never blocks the others. */
    @Transactional
    public void expandIfDue(Long requestId) {
        LocalDateTime now = LocalDateTime.now();
        BloodRequest r = requests.findById(requestId).orElse(null);
        if (r == null || r.getExpiresAt().isBefore(now)
                || (r.getStatus() != RequestStatus.OPEN && r.getStatus() != RequestStatus.PARTIALLY_FILLED)) {
            return;
        }
        LocalDateTime last = r.getLastWaveAt() != null ? r.getLastWaveAt() : now.minusMinutes(intervalMinutes);
        if (now.isBefore(last.plusMinutes(intervalMinutes))) {
            return;
        }
        int next = nextRadius(r.getCurrentRadiusKm(), r.getMaxRadiusKm());
        if (next < 0) {
            return;
        }
        int notified = matching.notifyWave(r, next);
        r.setCurrentRadiusKm(next);
        r.setLastWaveAt(now);
        requests.save(r);
        log.info("Request {} widened to {} km, {} new donor(s) alerted", requestId, next, notified);
        events.publishEvent(new RequestProgressEvent(requestId, r.getRequester().getId(), "WAVE_EXPANDED",
                r.getUnitsAccepted(), r.getUnitsNeeded(), r.getStatus().name(), next, notified));
    }

    @Transactional
    public int expireOverdue() {
        return requests.expireOverdue(LocalDateTime.now());
    }

    private int nextRadius(int current, int max) {
        return Arrays.stream(wavesCsv.split(",")).map(String::trim).mapToInt(Integer::parseInt)
                .filter(w -> w > current && w <= max).findFirst().orElse(-1);
    }
}