package com.lifelink.realtime;

public record ProgressData(int unitsAccepted, int unitsNeeded, String status, Integer radiusKm, Integer newlyNotified) {
}