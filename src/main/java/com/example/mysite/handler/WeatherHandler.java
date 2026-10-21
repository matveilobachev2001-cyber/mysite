package com.example.mysite.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.example.mysite.service.WeatherService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WeatherHandler implements HttpHandler {
    private final WeatherService service = new WeatherService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Map<String, String> params = parseQuery(ex.getRequestURI().getRawQuery());
        String city = params.get("city");

        if (city == null || city.isBlank()) {
            sendError(ex, 400, "Параметр city обязателен");
            return;
        }

        try {
            var data = service.getWeather(city);
            sendJson(ex, 200, data);
        } catch (IllegalArgumentException e) {
            sendError(ex, 404, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 502, "Не удалось получить погоду");
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            map.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
    }

    private void sendError(HttpExchange ex, int status, String msg) throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.put("error", msg);
        sendJson(ex, status, node);
    }
}