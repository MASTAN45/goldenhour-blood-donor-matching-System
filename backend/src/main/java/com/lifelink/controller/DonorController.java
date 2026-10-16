package com.lifelink.controller;

import com.lifelink.dto.RequestDtos;
import com.lifelink.entity.BloodGroup;
import com.lifelink.entity.DonorProfile;
import com.lifelink.entity.Gender;
import com.lifelink.repository.DonorProfileRepository;
import com.lifelink.security.AuthUser;
import com.lifelink.service.CompatibilityService;
import com.lifelink.service.GeoService;
import com.lifelink.service.MatchingService;
import com.lifelink.service.RequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/donors")
@PreAuthorize("hasRole('DONOR')")
@RequiredArgsConstructor
public class DonorController {
    private final DonorProfileRepository donors;
    private final GeoService geo;
    private final CompatibilityService compatibility;
    private final MatchingService matching;
    private final RequestService requestService;

    /** eligibilityIssues explains exactly why the donor would not receive alerts (empty = eligible). */
    public record DonorView(String fullName, BloodGroup bloodGroup, boolean available, boolean healthDeclarationOk,
                            boolean locationConsent, Double latitude, Double longitude,
                            LocalDate lastDonationDate, LocalDate nextEligibleDate, boolean eligibleNow,
                            List<String> eligibilityIssues, int reliabilityScore) {
    }

    public record LocationRequest(@NotNull Double latitude, @NotNull Double longitude) {
    }

    public record AvailabilityRequest(boolean available) {
    }

    /** A null date means "never donated". */
    public record LastDonationRequest(@PastOrPresent LocalDate lastDonationDate) {
    }

    public record HealthRequest(boolean accepted) {
    }

    @GetMapping("/me")
    public DonorView me(@AuthenticationPrincipal AuthUser user) {
        return view(profile(user.id()));
    }

    @PutMapping("/me/location")
    public DonorView location(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody LocationRequest body) {
        if (!geo.isValidIndiaCoordinate(body.latitude(), body.longitude())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates must be within India (lat 5-40, lon 65-100)");
        }
        DonorProfile p = profile(user.id());
        p.setLatitude(body.latitude());
        p.setLongitude(body.longitude());
        p.setLocationConsent(true);
        p.setLocationUpdatedAt(LocalDateTime.now());
        DonorProfile saved = donors.save(p);
        matching.catchUpForDonor(user.id());
        return view(saved);
    }

    @PutMapping("/me/availability")
    public DonorView availability(@AuthenticationPrincipal AuthUser user, @RequestBody AvailabilityRequest body) {
        DonorProfile p = profile(user.id());
        p.setAvailable(body.available());
        DonorProfile saved = donors.save(p);
        matching.catchUpForDonor(user.id());
        return view(saved);
    }

    @PutMapping("/me/last-donation")
    public DonorView lastDonation(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody LastDonationRequest body) {
        DonorProfile p = profile(user.id());
        p.setLastDonationDate(body.lastDonationDate());
        DonorProfile saved = donors.save(p);
        matching.catchUpForDonor(user.id());
        return view(saved);
    }

    @PutMapping("/me/health-declaration")
    public DonorView health(@AuthenticationPrincipal AuthUser user, @RequestBody HealthRequest body) {
        DonorProfile p = profile(user.id());
        p.setHealthDeclarationOk(body.accepted());
        DonorProfile saved = donors.save(p);
        matching.catchUpForDonor(user.id());
        return view(saved);
    }

    @GetMapping("/me/responses")
    public List<RequestDtos.DonorResponseView> myResponses(@AuthenticationPrincipal AuthUser user) {
        return requestService.donorResponses(user.id());
    }

    private DonorProfile profile(Long userId) {
        return donors.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor profile not found"));
    }

    private DonorView view(DonorProfile p) {
        List<String> issues = compatibility.exclusionReasons(p, LocalDate.now());
        int cooldown = p.getGender() == Gender.FEMALE ? 120 : 90;
        LocalDate next = p.getLastDonationDate() == null ? null : p.getLastDonationDate().plusDays(cooldown);
        return new DonorView(p.getFullName(), p.getBloodGroup(), p.isAvailable(), p.isHealthDeclarationOk(),
                p.isLocationConsent(), p.getLatitude(), p.getLongitude(), p.getLastDonationDate(), next,
                issues.isEmpty(), issues, p.getReliabilityScore());
    }
}