package com.lifelink.dto;

import com.lifelink.entity.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class RequestDtos {
    private RequestDtos() {
    }

    public record CreateRequest(
            @NotNull BloodGroup bloodGroup,
            @Min(1) @Max(10) int units,
            @NotNull Urgency urgency,
            @NotBlank @Size(max = 100) String patientName,
            BloodComponent component,
            @Size(max = 500) String notes,
            Double latitude,
            Double longitude) {
    }

    /** What a donor sees: a request they were alerted about, with their own distance. */
    public record NearbyView(
            Long requestId,
            String hospitalName,
            String hospitalCity,
            Double hospitalLatitude,
            Double hospitalLongitude,
            BloodGroup bloodGroup,
            int unitsNeeded,
            int unitsAccepted,
            Urgency urgency,
            RequestStatus status,
            LocalDateTime expiresAt,
            Double distanceKm,
            DonorResponseStatus myStatus) {
    }

    public record DonorResponseView(
            Long responseId,
            Long requestId,
            String hospitalName,
            BloodGroup bloodGroup,
            Urgency urgency,
            DonorResponseStatus status,
            Double distanceKm,
            LocalDateTime respondedAt,
            RequestStatus requestStatus,
            LocalDateTime expiresAt) {
    }

    /** What a hospital sees as a summary of its own request. */
    public record MineView(Long id, BloodGroup bloodGroup, int unitsNeeded, int unitsAccepted, Urgency urgency,
                           RequestStatus status, LocalDateTime expiresAt, int currentRadiusKm,
                           int donorsNotified, long accepted, long declined) {
    }

    /** Donor name and phone are revealed only after the donor accepted. */
    public record ResponseView(String donorLabel, BloodGroup bloodGroup, DonorResponseStatus status,
                               Double distanceKm, String donorName, String donorPhone) {
    }

    public record TrackerView(MineView summary, List<ResponseView> responses) {
    }
}