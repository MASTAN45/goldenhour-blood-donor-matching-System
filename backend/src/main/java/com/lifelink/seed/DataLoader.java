package com.lifelink.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Seeds demo data on startup. Disable with lifelink.seed.enabled=false (the test profile does). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lifelink.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataLoader implements ApplicationRunner {
    private final SeedService seed;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seed.seedIfNeeded();
        } catch (Exception e) {
            log.error("Seeding failed, the app keeps running without demo data", e);
        }
    }
}