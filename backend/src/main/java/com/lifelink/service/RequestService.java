package com.lifelink.service;

import com.lifelink.dto.RequestDtos;
import com.lifelink.dto.RequestDtos.*;
import com.lifelink.entity.*;
import com.lifelink.repository.*;
import com.lifelink.realtime.RequestClosedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final BloodRequestRepository requests;
    private final HospitalRepository hospitals;
    private final UserRepository users;
    private final DonorResponseRepository responses;
    private final DonorProfileRepository donorProfiles;
    private final MatchingService matching;
    private final GeoService geo;
    private final ApplicationEventPublisher events;

    @Value("${lifelink.expiry.critical-hours:6}")
    private int criticalHours;
    @Value("${lifelink.expiry.urgent-hours:24}")
    private int urgentHours;
    @Value("${lifelink.expiry.routine-hours:168}")
    private int routineHours;
    @Value("${lifelink.matching.waves:5,10,25}")
    private String wavesCsv;

    /** Creates the request and immediately runs wave 1 (the first, smallest radius). */
    @Transactional
    public MineView create(Long userId, CreateRequest in) {
        Hospital h = hospitals.findFirstByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No hospital profile for this account"));
        if (!h.isVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hospital is not verified yet");
        }
        double lat = in.latitude() != null ? in.latitude() : h.getLatitude();
        double lon = in.longitude() != null ? in.longitude() : h.getLongitude();
        if (!geo.isValidIndiaCoordinate(lat, lon)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates must be within India (lat 5-40, lon 65-100)");
        }
        LocalDateTime now = LocalDateTime.now();
        long hours = switch (in.urgency()) {
            case CRITICAL -> criticalHours;
            case URGENT -> urgentHours;
            default -> routineHours;
        };
        int firstRadius = Integer.parseInt(wavesCsv.split(",")[0].trim());
        String name = in.patientName().trim();

        BloodRequest saved = requests.save(BloodRequest.builder()
                .hospital(h).requester(users.getReferenceById(userId))
                .patientNameMasked(name.substring(0, 1).toUpperCase() + "***")
                .bloodGroupNeeded(in.bloodGroup())
                .component(in.component() != null ? in.component() : BloodComponent.WHOLE_BLOOD)
                .unitsNeeded(in.units()).urgency(in.urgency())
                .latitude(lat).longitude(lon)
                .currentRadiusKm(firstRadius).lastWaveAt(now)
                .notes(in.notes()).expiresAt(now.plusHours(hours))
                .build());

        matching.notifyWave(saved, firstRadius);
        return toMine(saved, responses.findByRequest_IdOrderByIdAsc(saved.getId()));
    }

    @Transactional(readOnly = true)
    public List<NearbyView> nearbyFor(Long donorUserId) {
        LocalDateTime now = LocalDateTime.now();
        return responses.findByDonor_IdAndStatusInOrderByIdDesc(donorUserId,
                        List.of(DonorResponseStatus.NOTIFIED, DonorResponseStatus.ACCEPTED, DonorResponseStatus.AUTO_CLOSED))
                .stream()
                .filter(dr -> dr.getStatus() != DonorResponseStatus.NOTIFIED || isOpen(dr.getRequest(), now))
                .limit(50)
                .map(dr -> {
                    BloodRequest r = dr.getRequest();
                    Hospital h = r.getHospital();
                    return new NearbyView(r.getId(), h.getName(), h.getCity(), h.getLatitude(), h.getLongitude(),
                            r.getBloodGroupNeeded(), r.getUnitsNeeded(), r.getUnitsAccepted(), r.getUrgency(),
                            r.getStatus(), r.getExpiresAt(), dr.getDistanceKm(), dr.getStatus());
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<RequestDtos.DonorResponseView> donorResponses(Long donorId) {
        return responses.findByDonor_IdOrderByIdDesc(donorId)
                .stream()
                .map(dr -> new RequestDtos.DonorResponseView(
                        dr.getId(),
                        dr.getRequest().getId(),
                        dr.getRequest().getHospital().getName(),
                        dr.getRequest().getBloodGroupNeeded(),
                        dr.getRequest().getUrgency(),
                        dr.getStatus(),
                        dr.getDistanceKm(),
                        dr.getRespondedAt(),
                        dr.getRequest().getStatus(),
                        dr.getRequest().getExpiresAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MineView> mine(Long userId) {
        return requests.findByRequester_IdOrderByCreatedAtDesc(userId).stream()
                .map(r -> toMine(r, responses.findByRequest_IdOrderByIdAsc(r.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public TrackerView tracker(Long requestId, Long userId) {
        BloodRequest r = owned(requestId, userId);
        List<DonorResponse> rs = responses.findByRequest_IdOrderByIdAsc(requestId);
        int[] n = {0};
        List<ResponseView> views = rs.stream().map(dr -> {
            n[0]++;
            DonorProfile p = donorProfiles.findByUser_Id(dr.getDonor().getId()).orElse(null);
            boolean revealed = dr.getStatus() == DonorResponseStatus.ACCEPTED
                    || dr.getStatus() == DonorResponseStatus.CONFIRMED_DONATED;
            return new ResponseView("Donor " + n[0], p == null ? null : p.getBloodGroup(), dr.getStatus(),
                    dr.getDistanceKm(),
                    revealed && p != null ? p.getFullName() : null,
                    revealed ? dr.getDonor().getPhone() : null);
        }).toList();
        return new TrackerView(toMine(r, rs), views);
    }

    @Transactional
    public MineView cancel(Long requestId, Long userId) {
        BloodRequest r = owned(requestId, userId);
        if (r.getStatus() != RequestStatus.OPEN && r.getStatus() != RequestStatus.PARTIALLY_FILLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is already closed");
        }
        r.setStatus(RequestStatus.CANCELLED);
        requests.saveAndFlush(r);
        List<Long> toNotify = responses.findDonorIdsByStatus(requestId, DonorResponseStatus.NOTIFIED);
        responses.closeOthers(requestId, LocalDateTime.now());
        events.publishEvent(new RequestClosedEvent(requestId, "REQUEST_CANCELLED", toNotify));
        return toMine(r, responses.findByRequest_IdOrderByIdAsc(requestId));
    }

    private BloodRequest owned(Long requestId, Long userId) {
        BloodRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!r.getRequester().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your request");
        }
        return r;
    }

    private boolean isOpen(BloodRequest r, LocalDateTime now) {
        return (r.getStatus() == RequestStatus.OPEN || r.getStatus() == RequestStatus.PARTIALLY_FILLED)
                && r.getExpiresAt().isAfter(now);
    }

    private MineView toMine(BloodRequest r, List<DonorResponse> rs) {
        long accepted = rs.stream().filter(x -> x.getStatus() == DonorResponseStatus.ACCEPTED
                || x.getStatus() == DonorResponseStatus.CONFIRMED_DONATED).count();
        long declined = rs.stream().filter(x -> x.getStatus() == DonorResponseStatus.DECLINED).count();
        return new MineView(r.getId(), r.getBloodGroupNeeded(), r.getUnitsNeeded(), r.getUnitsAccepted(),
                r.getUrgency(), r.getStatus(), r.getExpiresAt(), r.getCurrentRadiusKm(), rs.size(), accepted, declined);
    }
}