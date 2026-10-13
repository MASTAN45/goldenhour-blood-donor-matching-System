package com.lifelink.controller;

import com.lifelink.service.GeoService;
import com.lifelink.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/geo")
@RequiredArgsConstructor
public class GeoController {
    private final GeocodingService geocoding;
    private final GeoService geo;

    /** GET /api/v1/geo/search?q=Trunk Road Ongole  ->  up to 5 places with latitude/longitude. */
    @GetMapping("/search")
    public List<GeocodingService.Place> search(@RequestParam String q) {
        return geocoding.search(q);
    }

    /** GET /api/v1/geo/reverse?lat=15.5057&lon=80.0499  ->  readable address for a pin. */
    @GetMapping("/reverse")
    public GeocodingService.Place reverse(@RequestParam double lat, @RequestParam double lon) {
        if (!geo.isValidIndiaCoordinate(lat, lon)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates must be within India (lat 5-40, lon 65-100)");
        }
        return geocoding.reverse(lat, lon);
    }
}