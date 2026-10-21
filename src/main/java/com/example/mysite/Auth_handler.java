package com.example.mysite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class Auth_handler implements HttpHandler {

    private final store_user store;
    private final session_store sessions;
    private final ObjectMapper mapper = new ObjectMapper();

    public Auth_handler(store_user store, session_store sessions) {
        this.store = store;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        switch (path) {
            case "/api/auth/login"  -> handleLogin(ex);
            case "/api/auth/me"     -> handleMe(ex);
            case "/api/auth/logout" -> handleLogout(ex);
            default -> sendError(ex, 404, "Не найдено");
        }
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendError(ex, 405, "Разрешён только POST");
            return;
        }

        try {
            var body = mapper.readTree(ex.getRequestBody());
            String nickname = body.path("nickname").asText("").trim();

            if (nickname.length() < 3 || nickname.length() > 20) {
                sendError(ex, 400, "Ник должен быть от 3 до 20 символов");
                return;
            }
            if (!nickname.matches("[a-zA-Z0-9_а-яА-ЯёЁ]+")) {
                sendError(ex, 400, "Ник может содержать только буквы, цифры и _");
                return;
            }

            model_user user = store.findOrCreate(nickname);
            String token = sessions.create(user.getNickname());

            ObjectNode response = mapper.createObjectNode();
            response.put("token", token);
            response.set("user", mapper.valueToTree(user));

            sendJson(ex, 200, response);
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "Ошибка сервера");
        }
    }

    private void handleMe(HttpExchange ex) throws IOException {
        String nickname = sessions.getNickname(extractToken(ex));
        if (nickname == null) {
            sendError(ex, 401, "Не авторизован");
            return;
        }
        model_user user = store.find(nickname);
        if (user == null) {
            sendError(ex, 401, "Пользователь не найден");
            return;
        }
        sendJson(ex, 200, mapper.valueToTree(user));
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        sessions.remove(extractToken(ex));
        ObjectNode response = mapper.createObjectNode();
        response.put("ok", true);
        sendJson(ex, 200, response);
    }

    private String extractToken(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void sendError(HttpExchange ex, int status, String message) throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.put("error", message);
        byte[] bytes = mapper.writeValueAsBytes(node);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
