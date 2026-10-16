package com.lifelink.service;

import com.lifelink.entity.*;
import com.lifelink.exception.RequestAlreadyFilledException;
import com.lifelink.repository.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 100 donors press "Accept" at the same instant for a request that needs 3 units.
 * Exactly 3 must win, 97 must be told it is filled, and the database must never over-book.
 */
@SpringBootTest
@ActiveProfiles("test")
class AcceptConcurrencyTest {
    @Autowired AcceptService accept;
    @Autowired UserRepository users;
    @Autowired HospitalRepository hospitals;
    @Autowired BloodRequestRepository requests;
    @Autowired DonorResponseRepository responses;

    @TestConfiguration
    static class CleanTestDatabase {
        @Bean
        FlywayMigrationStrategy cleanThenMigrate(@Value("${spring.datasource.url}") String url) {
            return (Flyway flyway) -> {
                if (!url.contains("lifelink_test")) {
                    throw new IllegalStateException("Refusing to clean a non-test database: " + url);
                }
                flyway.clean();
                flyway.migrate();
            };
        }
    }

    @RepeatedTest(5)
    void hundredDonorsAcceptThreeUnits_exactlyThreeWin() throws Exception {
        final int donorsCount = 100;
        final int units = 3;
        String s = UUID.randomUUID().toString().substring(0, 8);

        User hospitalUser = users.save(User.builder().email("h" + s + "@t.test").phone("0000000000")
                .passwordHash("x").role(Role.REQUESTER).build());
        Hospital hospital = hospitals.save(Hospital.builder().user(hospitalUser).name("Test Hospital " + s)
                .registrationNo("T-" + s).address("Test").city("Ongole").latitude(15.5057).longitude(80.0499)
                .phone("0000000000").verified(true).build());
        Long requestId = requests.save(BloodRequest.builder().hospital(hospital).requester(hospitalUser)
                .patientNameMasked("T***").bloodGroupNeeded(BloodGroup.O_NEGATIVE).unitsNeeded(units)
                .urgency(Urgency.CRITICAL).latitude(15.5057).longitude(80.0499)
                .expiresAt(LocalDateTime.now().plusHours(6)).build()).getId();
        BloodRequest saved = requests.findById(requestId).orElseThrow();

        List<Long> donorIds = new ArrayList<>();
        for (int i = 0; i < donorsCount; i++) {
            User d = users.save(User.builder().email("d" + i + "_" + s + "@t.test").phone("0000000000")
                    .passwordHash("x").role(Role.DONOR).build());
            responses.save(DonorResponse.builder().request(saved).donor(d)
                    .status(DonorResponseStatus.NOTIFIED).distanceKm(1.0).build());
            donorIds.add(d.getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(donorsCount);
        CountDownLatch ready = new CountDownLatch(donorsCount);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (Long donorId : donorIds) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        accept.accept(requestId, donorId);
                        return "WON";
                    } catch (RequestAlreadyFilledException e) {
                        return "FILLED";
                    }
                }));
            }
            ready.await();
            go.countDown(); // everyone clicks Accept at once
            int won = 0, filled = 0;
            for (Future<String> f : futures) {
                String outcome = f.get(120, TimeUnit.SECONDS); // any other exception fails the test
                if (outcome.equals("WON")) won++; else filled++;
            }
            assertEquals(units, won, "winners");
            assertEquals(donorsCount - units, filled, "donors told 'already filled'");
        } finally {
            pool.shutdownNow();
        }

        BloodRequest after = requests.findById(requestId).orElseThrow();
        assertEquals(units, after.getUnitsAccepted(), "units_accepted must never exceed units_needed");
        assertEquals(RequestStatus.FILLED, after.getStatus());
        assertEquals(units, responses.countByRequest_IdAndStatus(requestId, DonorResponseStatus.ACCEPTED));
        assertEquals(donorsCount - units, responses.countByRequest_IdAndStatus(requestId, DonorResponseStatus.AUTO_CLOSED));
        assertEquals(0, responses.countByRequest_IdAndStatus(requestId, DonorResponseStatus.NOTIFIED));
    }
}