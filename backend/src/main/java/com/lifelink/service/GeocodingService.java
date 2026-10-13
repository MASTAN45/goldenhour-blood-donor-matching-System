package com.lifelink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Address <-> coordinates using OpenStreetMap Nominatim (free, no API key).
 * We call it politely: identify the app in User-Agent, at most about 1 request per second, and cache results.
 */
@Service
public class GeocodingService {
    public record Place(String label, double latitude, double longitude) {
    }

    private static final String BASE = "https://nominatim.openstreetmap.org";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, List<Place>> cache = new ConcurrentHashMap<>();
    private long lastCallMs = 0;

    @Value("${lifelink.geo.user-agent:GoldenHour-Demo/1.0}")
    private String userAgent;

    public List<Place> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 3) return List.of();
        String key = q.toLowerCase();
        List<Place> hit = cache.get(key);
        if (hit != null) return hit;
        JsonNode root = call("/search?format=jsonv2&limit=5&countrycodes=in&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8));
        List<Place> out = new ArrayList<>();
        for (JsonNode n : root) {
            out.add(new Place(n.path("display_name").asText(), n.path("lat").asDouble(), n.path("lon").asDouble()));
        }
        if (cache.size() > 500) cache.clear();
        cache.put(key, out);
        return out;
    }

    public Place reverse(double lat, double lon) {
        JsonNode n = call("/reverse?format=jsonv2&zoom=16&lat=" + lat + "&lon=" + lon);
        return new Place(n.path("display_name").asText("Unknown location"), lat, lon);
    }

    /** Synchronized on purpose: it spaces calls at least 1.1 s apart to respect the free service. */
    private synchronized JsonNode call(String path) {
        try {
            long wait = 1100 - (System.currentTimeMillis() - lastCallMs);
            if (wait > 0) Thread.sleep(wait);
            lastCallMs = System.currentTimeMillis();
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", userAgent)
                    .header("Accept-Language", "en")
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new IllegalStateException("Geocoder status " + res.statusCode());
            return mapper.readTree(res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Location lookup was interrupted");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Location lookup is unavailable right now");
        }
    }
}