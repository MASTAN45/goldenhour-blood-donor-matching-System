package com.lifelink.service;

import com.lifelink.dto.AdminDtos.HospitalAdminView;
import com.lifelink.dto.AdminDtos.Stats;
import com.lifelink.entity.Hospital;
import com.lifelink.entity.RequestStatus;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonorProfileRepository;
import com.lifelink.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final HospitalRepository hospitals;
    private final DonorProfileRepository donors;
    private final BloodRequestRepository requests;

    public static String statusOf(Hospital h) {
        if (h.isVerified()) return "VERIFIED";
        return h.getRejectionReason() != null ? "REJECTED" : "PENDING";
    }

    @Transactional(readOnly = true)
    public List<HospitalAdminView> list(String status) {
        String s = status == null ? "ALL" : status.toUpperCase();
        return hospitals.findAllWithUser().stream().map(this::view)
                .filter(v -> s.equals("ALL") || v.status().equals(s)).toList();
    }

    @Transactional
    public HospitalAdminView verify(Long id, Long adminId) {
        Hospital h = find(id);
        h.setVerified(true);
        h.setVerifiedBy(adminId);
        h.setVerifiedAt(LocalDateTime.now());
        h.setRejectionReason(null);
        return view(hospitals.save(h));
    }

    @Transactional
    public HospitalAdminView reject(Long id, Long adminId, String reason) {
        Hospital h = find(id);
        h.setVerified(false);
        h.setVerifiedBy(adminId);
        h.setVerifiedAt(null);
        h.setRejectionReason(reason.trim());
        return view(hospitals.save(h));
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        return new Stats(donors.count(), hospitals.count(), hospitals.countByVerifiedFalseAndRejectionReasonIsNull(),
                requests.countByStatusIn(List.of(RequestStatus.OPEN, RequestStatus.PARTIALLY_FILLED)),
                requests.countByStatusIn(List.of(RequestStatus.FILLED, RequestStatus.FULFILLED)),
                requests.count());
    }

    private Hospital find(Long id) {
        return hospitals.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital not found"));
    }

    private HospitalAdminView view(Hospital h) {
        return new HospitalAdminView(h.getId(), h.getUser().getId(), h.getUser().getEmail(), h.getName(),
                h.getRegistrationNo(), h.getAddress(), h.getCity(), h.getState(), h.getPhone(),
                h.getEmergencyContact(), h.getLatitude(), h.getLongitude(), statusOf(h), h.getRejectionReason(),
                h.getCreatedAt());
    }
}