package com.lifelink.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donor_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DonorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false)
    private BloodGroup bloodGroup;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Builder.Default
    private boolean available = true;

    @Builder.Default
    @Column(name = "health_declaration_ok")
    private boolean healthDeclarationOk = false;

    private Double latitude;
    private Double longitude;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @Builder.Default
    @Column(name = "alert_radius_km")
    private int alertRadiusKm = 25;

    @Builder.Default
    @Column(name = "location_consent")
    private boolean locationConsent = false;

    @Builder.Default
    @Column(name = "reliability_score")
    private int reliabilityScore = 100;

    @Builder.Default
    @Column(name = "no_show_count")
    private int noShowCount = 0;

    @Builder.Default
    @Column(name = "total_donations")
    private int totalDonations = 0;

    @Builder.Default
    @Column(name = "total_units_donated")
    private int totalUnitsDonated = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}