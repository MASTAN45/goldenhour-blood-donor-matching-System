package com.lifelink.service;

import com.lifelink.dto.RequestDtos.NearbyView;
import com.lifelink.entity.*;
import com.lifelink.realtime.AlertsCreatedEvent;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonorProfileRepository;
import com.lifelink.repository.DonorResponseRepository;
import com.lifelink.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds compatible, eligible donors inside a radius around the hospital and alerts them.
 * Pipeline: compatible groups -> bounding box (DB) -> exact Haversine -> eligibility rules
 * -> rank (exact group first, nearer first, higher reliability first) -> alert each donor once.
 * Alerts are pushed in real time after commit (see RealtimeEventListener).
 */
@Service
@RequiredArgsConstructor
public class MatchingService {
    private final DonorProfileRepository donors;
    private final DonorResponseRepository responses;
    private final NotificationLogRepository notifications;
    private final BloodRequestRepository requests;
    private final CompatibilityService compatibility;
    private final GeoService geo;
    private final ApplicationEventPublisher events;

    private record Candidate(DonorProfile donor, double distanceKm) {
    }

    /** Returns how many NEW donors were alerted in this wave. A donor is never alerted twice. */
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

        List<AlertsCreatedEvent.Target> targets = new ArrayList<>();
        for (Candidate c : ranked) {
            User donorUser = c.donor().getUser();
            if (responses.findByRequest_IdAndDonor_Id(request.getId(), donorUser.getId()).isPresent()) {
                continue; // already alerted in an earlier wave
            }
            createAlert(request, donorUser, round1(c.distanceKm()), targets);
        }
        publish(targets);
        return targets.size();
    }

    /**
     * A donor who just became eligible (shared location, turned availability on, fixed their profile)
     * is matched against requests that are ALREADY open, so they do not miss an urgent request.
     */
    @Transactional
    public int catchUpForDonor(Long donorUserId) {
        DonorProfile d = donors.findByUser_Id(donorUserId).orElse(null);
        if (d == null || !compatibility.exclusionReasons(d, LocalDate.now()).isEmpty()) {
            return 0;
        }
        List<AlertsCreatedEvent.Target> targets = new ArrayList<>();
        for (BloodRequest r : requests.findByStatusInAndExpiresAtAfter(
                List.of(RequestStatus.OPEN, RequestStatus.PARTIALLY_FILLED), LocalDateTime.now())) {
            if (!compatibility.canDonate(d.getBloodGroup(), r.getBloodGroupNeeded())) continue;
            double km = geo.haversineKm(r.getLatitude(), r.getLongitude(), d.getLatitude(), d.getLongitude());
            if (km > Math.min(r.getCurrentRadiusKm(), d.getAlertRadiusKm())) continue;
            if (responses.findByRequest_IdAndDonor_Id(r.getId(), donorUserId).isPresent()) continue;
            createAlert(r, d.getUser(), round1(km), targets);
        }
        publish(targets);
        return targets.size();
    }

    private void createAlert(BloodRequest r, User donorUser, double km, List<AlertsCreatedEvent.Target> targets) {
        responses.save(DonorResponse.builder().request(r).donor(donorUser)
                .status(DonorResponseStatus.NOTIFIED).distanceKm(km).build());
        Hospital h = r.getHospital();
        notifications.save(NotificationLog.builder()
                .userId(donorUser.getId()).requestId(r.getId())
                .channel(NotificationChannel.WEBSOCKET)
                .title(r.getUrgency() + ": " + BloodLabel.of(r.getBloodGroupNeeded()) + " blood needed")
                .message(r.getUnitsNeeded() + " unit(s) at " + h.getName() + ", " + km + " km away")
                .build());
        targets.add(new AlertsCreatedEvent.Target(donorUser.getId(), new NearbyView(r.getId(), h.getName(), h.getCity(), h.getLatitude(), h.getLongitude(),
                r.getBloodGroupNeeded(), r.getUnitsNeeded(), r.getUnitsAccepted(), r.getUrgency(), r.getStatus(),
                r.getExpiresAt(), km, DonorResponseStatus.NOTIFIED)));
    }

    private void publish(List<AlertsCreatedEvent.Target> targets) {
        if (!targets.isEmpty()) {
            events.publishEvent(new AlertsCreatedEvent(targets));
        }
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}