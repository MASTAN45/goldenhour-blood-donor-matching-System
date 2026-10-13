package com.lifelink.service;

import com.lifelink.entity.BloodGroup;
import com.lifelink.entity.DonorProfile;
import com.lifelink.entity.Gender;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CompatibilityService {

    /** Standard ABO/Rh red-cell compatibility: donor group -> recipient groups it can give to. */
    private static final Map<String, Set<String>> DONOR_TO_RECIPIENTS = Map.of(
            "O_NEGATIVE", Set.of("O_NEGATIVE", "O_POSITIVE", "A_NEGATIVE", "A_POSITIVE", "B_NEGATIVE", "B_POSITIVE", "AB_NEGATIVE", "AB_POSITIVE"),
            "O_POSITIVE", Set.of("O_POSITIVE", "A_POSITIVE", "B_POSITIVE", "AB_POSITIVE"),
            "A_NEGATIVE", Set.of("A_NEGATIVE", "A_POSITIVE", "AB_NEGATIVE", "AB_POSITIVE"),
            "A_POSITIVE", Set.of("A_POSITIVE", "AB_POSITIVE"),
            "B_NEGATIVE", Set.of("B_NEGATIVE", "B_POSITIVE", "AB_NEGATIVE", "AB_POSITIVE"),
            "B_POSITIVE", Set.of("B_POSITIVE", "AB_POSITIVE"),
            "AB_NEGATIVE", Set.of("AB_NEGATIVE", "AB_POSITIVE"),
            "AB_POSITIVE", Set.of("AB_POSITIVE"));

    public boolean canDonate(BloodGroup donor, BloodGroup recipient) {
        return DONOR_TO_RECIPIENTS.get(donor.name()).contains(recipient.name());
    }

    public List<BloodGroup> compatibleDonorGroups(BloodGroup recipient) {
        return Arrays.stream(BloodGroup.values()).filter(d -> canDonate(d, recipient)).toList();
    }

    /** Empty list means the donor is eligible; otherwise every failed rule is listed (explainability). */
    public List<String> exclusionReasons(DonorProfile d, LocalDate today) {
        List<String> reasons = new ArrayList<>();
        if (!d.isAvailable()) reasons.add("Donor marked unavailable");
        if (!d.isHealthDeclarationOk()) reasons.add("Health declaration not accepted");
        if (d.getDateOfBirth() != null) {
            int age = Period.between(d.getDateOfBirth(), today).getYears();
            if (age < 18 || age > 65) reasons.add("Age " + age + " outside 18-65");
        }
        if (d.getWeightKg() != null && d.getWeightKg() < 50) reasons.add("Weight below 50 kg");
        if (d.getLastDonationDate() != null) {
            int cooldown = d.getGender() == Gender.FEMALE ? 120 : 90;
            long days = ChronoUnit.DAYS.between(d.getLastDonationDate(), today);
            if (days < cooldown) reasons.add("In donation cooldown (" + days + " of " + cooldown + " days)");
        }
        if (!d.isLocationConsent() || d.getLatitude() == null || d.getLongitude() == null) {
            reasons.add("No location consent or location missing");
        }
        return reasons;
    }
}