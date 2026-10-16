package com.lifelink.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoServiceTest {
    private final GeoService geo = new GeoService();

    @Test
    void samePointIsZero() {
        assertEquals(0.0, geo.haversineKm(15.5, 80.0, 15.5, 80.0), 1e-9);
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        assertEquals(111.19, geo.haversineKm(15, 80, 16, 80), 0.1);
    }

    @Test
    void boundingBoxKeepsNearbyPointsAndDropsFarOnes() {
        double[] box = geo.boundingBox(15.5057, 80.0499, 5);
        assertTrue(15.5057 + 3 / 111.2 <= box[1]);   // 3 km north is inside
        assertTrue(15.5057 + 12 / 111.2 > box[1]);   // 12 km north is outside
    }

    @Test
    void indiaCoordinateValidation() {
        assertTrue(geo.isValidIndiaCoordinate(15.5, 80.0));
        assertFalse(geo.isValidIndiaCoordinate(51.5, -0.1));
        assertFalse(geo.isValidIndiaCoordinate(null, 80.0));
    }
}