package com.example.mysite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GeocodingService {
    private static final String NOMINATIM = "https://nominatim.openstreetmap.org/search";
    // ВАЖНО: Укажите здесь ссылку на ваш сайт или реальный email!
    private static final String USER_AGENT = "MultiSiteWeather/1.0 (matvei.l@inbox.ru)";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GeoResult geocode(String city) throws Exception {
        // Nominatim требует не более 1 запроса в секунду
        Thread.sleep(1000);

        String url = NOMINATIM + "?q=" + URLEncoder.encode(city, StandardCharsets.UTF_8) + "&format=json&limit=1&accept-language=ru";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ru-RU,ru;q=0.9")
                .timeout(Duration.ofSeconds(15))
                .GET().build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new RuntimeException("Nominatim код: " + res.statusCode());
        }

        JsonNode arr = mapper.readTree(res.body());
        if (arr.isEmpty()) return null;

        JsonNode first = arr.get(0);
        GeoResult r = new GeoResult();
        r.latitude = first.path("lat").asDouble();
        r.longitude = first.path("lon").asDouble();
        r.displayName = first.path("display_name").asText(city);
        return r;
    }

    public static class GeoResult {
        public double latitude;
        public double longitude;
        public String displayName;
    }
}