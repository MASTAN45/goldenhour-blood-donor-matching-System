package com.lifelink.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BloodRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "patient_name_masked", nullable = false)
    private String patientNameMasked;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group_needed", nullable = false)
    private BloodGroup bloodGroupNeeded;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BloodComponent component = BloodComponent.WHOLE_BLOOD;

    @Column(name = "units_needed", nullable = false)
    private int unitsNeeded;

    @Builder.Default
    @Column(name = "units_accepted")
    private int unitsAccepted = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Builder.Default
    @Column(name = "current_radius_km")
    private int currentRadiusKm = 5;

    @Builder.Default
    @Column(name = "max_radius_km")
    private int maxRadiusKm = 25;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus status = RequestStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_wave_at")
    private LocalDateTime lastWaveAt;

    @Version
    private Long version;
}