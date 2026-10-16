package com.lifelink.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelink.entity.*;
import com.lifelink.repository.*;
import com.lifelink.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Demo data around Ongole: 1 admin, 8 verified hospitals, 120 synthetic donors, 2 live requests. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {
    private static final double CENTER_LAT = 15.5057;
    private static final double CENTER_LON = 80.0499;
    private static final String[] MALE = {"Aarav", "Vivaan", "Sai", "Arjun", "Krishna", "Rohan", "Karthik", "Venkat",
            "Ravi", "Suresh", "Anil", "Prasad", "Mahesh", "Naveen", "Srinivas", "Harsha"};
    private static final String[] FEMALE = {"Priya", "Anitha", "Lakshmi", "Divya", "Sravani", "Harika", "Meena", "Swathi",
            "Pooja", "Keerthi", "Bhavana", "Nandini", "Sirisha", "Madhavi", "Roja", "Tejaswini"};
    private static final String[] LAST = {"Reddy", "Naidu", "Rao", "Kumar", "Sharma", "Varma", "Chowdary", "Yadav",
            "Babu", "Raju", "Murthy", "Prasad", "Goud", "Nair", "Patel", "Singh"};

    private final UserRepository users;
    private final HospitalRepository hospitals;
    private final DonorProfileRepository donors;
    private final BloodRequestRepository requests;
    private final PasswordEncoder encoder;
    private final MatchingService matching;
    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public void seedIfNeeded() {
        if (users.existsByEmail("admin@lifelink.test")) {
            log.info("Seed data already present, skipping");
            return;
        }
        String adminHash = encoder.encode("Admin@123");
        String hospitalHash = encoder.encode("Hospital@123");
        String donorHash = encoder.encode("Donor@123");

        users.save(User.builder().email("admin@lifelink.test").phone("0000000000")
                .passwordHash(adminHash).role(Role.ADMIN).build());

        List<Hospital> hs = new ArrayList<>();
        int i = 0;
        for (JsonNode n : loadHospitals()) {
            i++;
            User u = users.save(User.builder().email("hospital" + i + "@lifelink.test").phone("0000000000")
                    .passwordHash(hospitalHash).role(Role.REQUESTER).build());
            hs.add(hospitals.save(Hospital.builder().user(u).name(n.get("name").asText())
                    .registrationNo(String.format("DEMO-ONG-%03d", i)).address(n.get("address").asText())
                    .city("Ongole").state("Andhra Pradesh")
                    .latitude(n.get("latitude").asDouble()).longitude(n.get("longitude").asDouble())
                    .phone("0000000000").verified(true).verifiedAt(LocalDateTime.now()).build()));
        }
        seedDonors(donorHash);
        LocalDateTime now = LocalDateTime.now();
        demoRequest(hs.get(0), BloodGroup.O_NEGATIVE, 3, Urgency.CRITICAL, "Ravi", now.plusHours(6));
        demoRequest(hs.get(1), BloodGroup.B_POSITIVE, 2, Urgency.URGENT, "Sita", now.plusHours(24));
        log.info("Seeded demo data: 1 admin, {} hospitals, 120 donors, 2 requests. Logins: admin@lifelink.test / Admin@123, "
                + "hospital1..{}@lifelink.test / Hospital@123, donor1..donor120@lifelink.test / Donor@123", hs.size(), hs.size());
    }

    private JsonNode loadHospitals() {
        try (InputStream in = new ClassPathResource("seed/hospitals.json").getInputStream()) {
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void seedDonors(String hash) {
        Random rnd = new Random(42); // fixed seed: same demo data every time
        List<BloodGroup> pool = weightedGroups();
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= 120; i++) {
            boolean universal = i <= 8; // O- donors close to the demo hospitals
            boolean female = rnd.nextInt(100) < 45;
            BloodGroup group = universal ? BloodGroup.O_NEGATIVE : pool.get(rnd.nextInt(pool.size()));
            String first = female ? FEMALE[rnd.nextInt(FEMALE.length)] : MALE[rnd.nextInt(MALE.length)];
            String name = first + " " + LAST[rnd.nextInt(LAST.length)];
            double weight = female ? 50 + rnd.nextInt(26) : 55 + rnd.nextInt(36);
            LocalDate dob = today.minusYears(19 + rnd.nextInt(37)).minusDays(rnd.nextInt(300));
            LocalDate last = null;
            if (!universal) {
                int roll = rnd.nextInt(100);
                if (roll < 15) last = today.minusDays(20 + rnd.nextInt(70));        // still in cooldown
                else if (roll < 65) last = today.minusDays(130 + rnd.nextInt(600)); // donated long ago
            }                                                                        // otherwise: never donated
            boolean available = universal || rnd.nextInt(100) < 70;
            boolean health = universal || rnd.nextInt(100) < 95;
            boolean consent = universal || rnd.nextInt(100) < 95;
            double[] loc = universal ? around(rnd, 3.0 * Math.sqrt(rnd.nextDouble())) : around(rnd, spreadDistance(rnd));

            User u = users.save(User.builder().email("donor" + i + "@lifelink.test")
                    .phone(String.format("00000%05d", i)) // clearly fake number
                    .passwordHash(hash).role(Role.DONOR).build());
            donors.save(DonorProfile.builder().user(u).fullName(name).dateOfBirth(dob)
                    .gender(female ? Gender.FEMALE : Gender.MALE).bloodGroup(group).weightKg(weight)
                    .lastDonationDate(last).available(available).healthDeclarationOk(health)
                    .locationConsent(consent)
                    .latitude(consent ? loc[0] : null).longitude(consent ? loc[1] : null)
                    .locationUpdatedAt(consent ? LocalDateTime.now() : null)
                    .reliabilityScore(70 + rnd.nextInt(31)).build());
        }
    }

    private void demoRequest(Hospital h, BloodGroup group, int units, Urgency urgency, String patient, LocalDateTime expires) {
        BloodRequest r = requests.save(BloodRequest.builder().hospital(h).requester(h.getUser())
                .patientNameMasked(patient.substring(0, 1) + "***").bloodGroupNeeded(group).unitsNeeded(units)
                .urgency(urgency).latitude(h.getLatitude()).longitude(h.getLongitude())
                .currentRadiusKm(5).lastWaveAt(LocalDateTime.now()).notes("Demo request").expiresAt(expires).build());
        int notified = matching.notifyWave(r, 5);
        log.info("Demo request {} ({} x{}) at {} alerted {} donor(s) in wave 1", r.getId(), group, units, h.getName(), notified);
    }

    /** 80% within 8 km, 15% within 8-20 km, 5% within 20-30 km of the Ongole centre. */
    private static double spreadDistance(Random rnd) {
        double p = rnd.nextDouble();
        if (p < 0.80) return 8 * Math.sqrt(rnd.nextDouble());
        if (p < 0.95) return 8 + 12 * rnd.nextDouble();
        return 20 + 10 * rnd.nextDouble();
    }

    private static double[] around(Random rnd, double distKm) {
        double bearing = rnd.nextDouble() * 2 * Math.PI;
        double lat = CENTER_LAT + (distKm * Math.cos(bearing)) / 111.32;
        double lon = CENTER_LON + (distKm * Math.sin(bearing)) / (111.32 * Math.cos(Math.toRadians(CENTER_LAT)));
        return new double[]{Math.round(lat * 1_000_000) / 1_000_000.0, Math.round(lon * 1_000_000) / 1_000_000.0};
    }

    /** Roughly Indian blood-group frequencies: O+ and B+ most common, AB- rare. */
    private static List<BloodGroup> weightedGroups() {
        List<BloodGroup> l = new ArrayList<>();
        add(l, BloodGroup.O_POSITIVE, 30);
        add(l, BloodGroup.B_POSITIVE, 28);
        add(l, BloodGroup.A_POSITIVE, 20);
        add(l, BloodGroup.AB_POSITIVE, 7);
        add(l, BloodGroup.O_NEGATIVE, 4);
        add(l, BloodGroup.B_NEGATIVE, 4);
        add(l, BloodGroup.A_NEGATIVE, 4);
        add(l, BloodGroup.AB_NEGATIVE, 3);
        return l;
    }

    private static void add(List<BloodGroup> l, BloodGroup g, int n) {
        for (int i = 0; i < n; i++) l.add(g);
    }
}