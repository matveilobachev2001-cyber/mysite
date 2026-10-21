package com.example.mysite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mysite.model.WeatherData;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class WeatherService {
    // wttr.in понимает названия городов напрямую, без геокодинга
    private static final String WTTR_URL = "https://wttr.in/";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public WeatherData getWeather(String city) throws Exception {
        // Кодируем название города для URL и добавляем ?format=j1 для JSON
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = WTTR_URL + encodedCity + "?format=j1";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                // wttr.in, как и многие сервисы, лучше работает с User-Agent
                .header("User-Agent", "MultiSiteWeather/1.0 (your-real-email@example.com)")
                .timeout(Duration.ofSeconds(20))
                .GET().build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new RuntimeException("wttr.in вернул код: " + res.statusCode());
        }

        JsonNode root = mapper.readTree(res.body());
        // Структура ответа: current_condition[0] содержит текущую погоду
        JsonNode current = root.path("current_condition").get(0);
        if (current == null) {
            throw new IllegalArgumentException("Город не найден или не удалось получить данные: " + city);
        }

        WeatherData data = new WeatherData();
        data.setCity(city); // wttr.in может не вернуть точное название, используем введённое
        data.setCountry(""); // В current_condition нет страны, можно опустить

        // Температура в °C
        double temp = current.path("temp_C").asDouble();
        data.setTemperature(temp);
        // wttr.in возвращает "FeelsLikeC", что является ощущаемой температурой
        data.setFeelsLike(current.path("FeelsLikeC").asDouble());
        data.setHumidity(current.path("humidity").asInt());
        // Скорость ветра в wttr.in по умолчанию в км/ч, а нам нужны м/с
        double windKmh = current.path("windspeedKmph").asDouble();
        data.setWindSpeed(windKmh / 3.6); // Конвертация км/ч -> м/с

        // Описание погоды находится в weatherDesc[0].value
        String description = current.path("weatherDesc").get(0).path("value").asText("Нет данных");
        data.setDescription(description);
        data.setIcon(getEmoji(description)); // Простая установка эмодзи по описанию

        return data;
    }

    // Простой маппинг описания на эмодзи (можно расширить)
    private String getEmoji(String desc) {
        String d = desc.toLowerCase();
        if (d.contains("sun") || d.contains("clear")) return "☀️";
        if (d.contains("cloud") || d.contains("overcast")) return "☁️";
        if (d.contains("rain") || d.contains("drizzle")) return "🌧️";
        if (d.contains("snow") || d.contains("blizzard")) return "❄️";
        if (d.contains("thunder") || d.contains("storm")) return "⛈️";
        if (d.contains("fog") || d.contains("mist")) return "🌫️";
        return "🌡️";
    }
}