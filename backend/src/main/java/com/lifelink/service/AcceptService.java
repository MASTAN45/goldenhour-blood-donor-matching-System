package com.lifelink.service;

import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.DonorResponse;
import com.lifelink.entity.DonorResponseStatus;
import com.lifelink.entity.RequestStatus;
import com.lifelink.exception.RequestAlreadyFilledException;
import com.lifelink.realtime.RequestProgressEvent;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonorResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Race-condition-safe accept flow.
 * 1) claimSlot: atomic conditional UPDATE on the request row (the database is the final arbiter).
 * 2) markAccepted: moves this donor NOTIFIED -> ACCEPTED (UNIQUE(request_id, donor_id) keeps it idempotent).
 * 3) Any failure rolls the whole transaction back, so a slot is never leaked.
 * Realtime events are published here and delivered only after the commit.
 */
@Service
@RequiredArgsConstructor
public class AcceptService {
    private final BloodRequestRepository requests;
    private final DonorResponseRepository responses;
    private final ApplicationEventPublisher events;

    public record AcceptResult(String status, int position, int of) {
    }

    @Transactional
    public AcceptResult accept(Long requestId, Long donorUserId) {
        LocalDateTime now = LocalDateTime.now();

        DonorResponse existing = responses.findByRequest_IdAndDonor_Id(requestId, donorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You were not notified for this request"));

        if (existing.getStatus() == DonorResponseStatus.ACCEPTED) {
            BloodRequest r = requests.findById(requestId).orElseThrow();
            return new AcceptResult("ACCEPTED", r.getUnitsAccepted(), r.getUnitsNeeded());
        }
        if (existing.getStatus() != DonorResponseStatus.NOTIFIED) {
            throw new RequestAlreadyFilledException();
        }
        if (requests.claimSlot(requestId, now) == 0) {
            throw new RequestAlreadyFilledException();
        }
        if (responses.markAccepted(requestId, donorUserId, now) == 0) {
            throw new RequestAlreadyFilledException(); // rolls back the claimed slot
        }

        BloodRequest r = requests.findById(requestId).orElseThrow();
        events.publishEvent(new RequestProgressEvent(requestId, r.getRequester().getId(), "DONOR_ACCEPTED",
                r.getUnitsAccepted(), r.getUnitsNeeded(), r.getStatus().name(), null, null));
        if (r.getStatus() == RequestStatus.FILLED) {
            List<Long> closed = responses.findDonorIdsByStatus(requestId, DonorResponseStatus.NOTIFIED);
            responses.closeOthers(requestId, now);
            events.publishEvent(new RequestFilledEvent(requestId, r.getRequester().getId(), closed));
        }
        return new AcceptResult("ACCEPTED", r.getUnitsAccepted(), r.getUnitsNeeded());
    }

    @Transactional
    public void decline(Long requestId, Long donorUserId) {
        int changed = responses.markDeclined(requestId, donorUserId, LocalDateTime.now());
        if (changed > 0) {
            requests.findById(requestId).ifPresent(r -> events.publishEvent(new RequestProgressEvent(
                    requestId, r.getRequester().getId(), "DONOR_DECLINED",
                    r.getUnitsAccepted(), r.getUnitsNeeded(), r.getStatus().name(), null, null)));
        }
    }
}