package com.lifelink.service;

import com.lifelink.entity.*;
import com.lifelink.repository.DonorProfileRepository;
import com.lifelink.repository.DonorResponseRepository;
import com.lifelink.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Finds compatible, eligible donors inside a radius around the hospital and alerts them.
 * Pipeline: compatible blood groups -> bounding box (DB) -> exact Haversine -> eligibility rules
 * -> rank (exact group first, nearer first, higher reliability first) -> notify each donor once.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {
    private final DonorProfileRepository donors;
    private final DonorResponseRepository responses;
    private final NotificationLogRepository notifications;
    private final CompatibilityService compatibility;
    private final GeoService geo;

    private record Candidate(DonorProfile donor, double distanceKm) {
    }

    /** Returns how many NEW donors were notified in this wave. A donor is never notified twice. */
    @Transactional
    public int notifyWave(BloodRequest request, int radiusKm) {
        List<BloodGroup> groups = compatibility.compatibleDonorGroups(request.getBloodGroupNeeded());
        double[] box = geo.boundingBox(request.getLatitude(), request.getLongitude(), radiusKm);
        LocalDate today = LocalDate.now();

        List<Candidate> ranked = donors.findInBox(groups, box[0], box[1], box[2], box[3]).stream()
                .filter(d -> compatibility.exclusionReasons(d, today).isEmpty())
                .map(d -> new Candidate(d, geo.haversineKm(request.getLatitude(), request.getLongitude(),
                        d.getLatitude(), d.getLongitude())))
                .filter(c -> c.distanceKm() <= Math.min(radiusKm, c.donor().getAlertRadiusKm()))
                .sorted(Comparator
                        .comparing((Candidate c) -> c.donor().getBloodGroup() != request.getBloodGroupNeeded())
                        .thenComparingDouble(Candidate::distanceKm)
                        .thenComparing(c -> -c.donor().getReliabilityScore()))
                .toList();

        int notified = 0;
        for (Candidate c : ranked) {
            Long donorUserId = c.donor().getUser().getId();
            if (responses.findByRequest_IdAndDonor_Id(request.getId(), donorUserId).isPresent()) {
                continue; // already notified in an earlier wave
            }
            responses.save(DonorResponse.builder()
                    .request(request).donor(c.donor().getUser())
                    .status(DonorResponseStatus.NOTIFIED)
                    .distanceKm(Math.round(c.distanceKm() * 10.0) / 10.0)
                    .build());
            notifications.save(NotificationLog.builder()
                    .userId(donorUserId).requestId(request.getId())
                    .channel(NotificationChannel.WEBSOCKET)
                    .title(request.getUrgency() + ": " + request.getBloodGroupNeeded() + " needed")
                    .message(request.getUnitsNeeded() + " unit(s) needed, about "
                            + Math.round(c.distanceKm() * 10.0) / 10.0 + " km from you. Tap to respond.")
                    .build());
            notified++;
        }
        return notified;
    }
}