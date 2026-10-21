package com.example.mysite.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mysite.manager.ActivityManager;
import com.example.mysite.session_store;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ActivityHandler implements HttpHandler {
    private final session_store sessions;
    private final ObjectMapper mapper = new ObjectMapper();

    public ActivityHandler(session_store sessions) {
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        if (path.equals("/api/activity/ping") && ex.getRequestMethod().equals("POST")) {
            handlePing(ex);
        } else if (path.equals("/api/activity/list")) {
            handleList(ex);
        } else {
            ex.sendResponseHeaders(404, -1);
        }
    }

    private void handlePing(HttpExchange ex) throws IOException {
        String nickname = getNickname(ex);
        if (nickname != null) {
            var body = mapper.readTree(ex.getRequestBody());
            String action = body.path("action").asText("Ничего не делает");
            ActivityManager.update(nickname, action);
        }
        ex.sendResponseHeaders(200, -1);
    }

    private void handleList(HttpExchange ex) throws IOException {
        var users = ActivityManager.getActiveUsers();
        byte[] bytes = mapper.writeValueAsBytes(users);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private String getNickname(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return sessions.getNickname(header.substring(7));
        }
        return null;
    }
}