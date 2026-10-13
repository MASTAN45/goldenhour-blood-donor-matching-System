package com.lifelink.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "donor_responses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DonorResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private BloodRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DonorResponseStatus status = DonorResponseStatus.NOTIFIED;

    @Builder.Default
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}