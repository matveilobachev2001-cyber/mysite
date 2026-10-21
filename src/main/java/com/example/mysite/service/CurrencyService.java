package com.example.mysite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mysite.model.CurrencyData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class CurrencyService {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public CurrencyData getRates(String base) throws Exception {
        base = base.toUpperCase();
        String url = "https://api.frankfurter.dev/v1/latest?from=" + base + "&to=USD,EUR,GBP,JPY,CHF,CNY,RUB,TRY,KZT,UAH,PLN,CZK";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("Frankfurter код: " + res.statusCode());

        JsonNode root = mapper.readTree(res.body());
        CurrencyData data = new CurrencyData();
        data.setBase(root.path("base").asText(base));
        data.setDate(root.path("date").asText());

        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put(base, 1.0); // добавляем базовую валюту
        root.path("rates").fields().forEachRemaining(e -> rates.put(e.getKey(), e.getValue().asDouble()));
        data.setRates(rates);
        return data;
    }
}