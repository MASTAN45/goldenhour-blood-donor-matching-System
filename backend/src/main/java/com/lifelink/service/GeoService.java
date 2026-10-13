package com.lifelink.service;

import org.springframework.stereotype.Service;

@Service
public class GeoService {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    /** Great-circle distance in kilometres (Haversine formula). */
    public double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Cheap pre-filter box: returns {minLat, maxLat, minLon, maxLon}. */
    public double[] boundingBox(double lat, double lon, double radiusKm) {
        double dLat = Math.toDegrees(radiusKm / EARTH_RADIUS_KM);
        double dLon = Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(lat))));
        return new double[]{lat - dLat, lat + dLat, lon - dLon, lon + dLon};
    }

    public boolean isValidIndiaCoordinate(Double lat, Double lon) {
        return lat != null && lon != null && lat >= 5 && lat <= 40 && lon >= 65 && lon <= 100;
    }
}