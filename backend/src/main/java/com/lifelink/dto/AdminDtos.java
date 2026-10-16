package com.lifelink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class AdminDtos {
    private AdminDtos() {
    }

    /** status is PENDING, VERIFIED or REJECTED. */
    public record HospitalAdminView(Long id, Long userId, String email, String name, String registrationNo,
                                    String address, String city, String state, String phone, String emergencyContact,
                                    Double latitude, Double longitude, String status, String rejectionReason,
                                    LocalDateTime createdAt) {
    }

    public record RejectRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record Stats(long donors, long hospitals, long pendingHospitals, long openRequests,
                        long filledRequests, long totalRequests) {
    }

    public record HospitalSelfView(String name, String city, String status, String rejectionReason,
                                   Double latitude, Double longitude) {
    }
}