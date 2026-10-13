package com.lifelink.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaveScheduler {
    private final WaveService waves;

    @Scheduled(fixedDelayString = "${lifelink.matching.tick-ms:15000}")
    public void tick() {
        try {
            int expired = waves.expireOverdue();
            if (expired > 0) log.info("{} request(s) expired", expired);
        } catch (Exception e) {
            log.warn("Expiry pass failed: {}", e.toString());
        }
        for (Long id : waves.activeRequestIds()) {
            try {
                waves.expandIfDue(id);
            } catch (Exception e) {
                log.warn("Wave expansion failed for request {}: {}", id, e.toString());
            }
        }
    }
}