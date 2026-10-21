package com.example.mysite.handler;

import com.example.mysite.manager.GameManager;
import com.example.mysite.session_store;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class GameHandler implements HttpHandler {
    private final session_store sessions;
    private final ObjectMapper mapper = new ObjectMapper();

    public GameHandler(session_store sessions) {
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String nickname = getNickname(ex);

        if (nickname == null) { sendError(ex, 401, "Unauthorized"); return; }

        Map<String, Object> response = new HashMap<>();

        if (path.equals("/api/game/join")) {
            GameManager.Game game = GameManager.joinQueue(nickname);
            response.put("status", game == null ? "waiting" : game.phase);
        }
        else if (path.equals("/api/game/state")) {
            GameManager.Game game = GameManager.getGame(nickname);
            if (game == null) {
                // Если игры нет, проверяем: может, мы просто стоим в очереди?
                if (nickname.equals(GameManager.waitingPlayer)) {
                    response.put("status", "waiting");
                } else {
                    response.put("status", "none");
                }
            } else {
                response.put("status", game.phase);
                response.put("game", getGameState(game, nickname));
            }
        }
        else if (path.equals("/api/game/place") && ex.getRequestMethod().equals("POST")) {
            JsonNode body = mapper.readTree(ex.getRequestBody());
            int[][] ships = new int[10][10];
            JsonNode arr = body.path("ships");
            for (int i = 0; i < 100; i++) {
                ships[i / 10][i % 10] = arr.get(i).asInt();
            }
            boolean ok = GameManager.placeShips(nickname, ships);
            response.put("success", ok);
        }
        else if (path.equals("/api/game/shoot") && ex.getRequestMethod().equals("POST")) {
            JsonNode body = mapper.readTree(ex.getRequestBody());
            boolean ok = GameManager.shoot(nickname, body.path("row").asInt(), body.path("col").asInt());
            response.put("success", ok);
        }
        else if (path.equals("/api/game/leave")) {
            GameManager.leaveGame(nickname);
            response.put("success", true);
        }

        sendJson(ex, 200, response);
    }

    private Map<String, Object> getGameState(GameManager.Game game, String me) {
        Map<String, Object> state = new HashMap<>();
        boolean isP1 = me.equals(game.p1);

        state.put("turn", game.turn);
        state.put("winner", game.winner);
        state.put("myShips", isP1 ? game.b1 : game.b2);
        state.put("myShots", isP1 ? game.s1 : game.s2); // Выстрелы по мне
        state.put("enemyShots", isP1 ? game.s2 : game.s1); // Мои выстрелы
        return state;
    }

    private String getNickname(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) return sessions.getNickname(header.substring(7));
        return null;
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
    }

    private void sendError(HttpExchange ex, int status, String msg) throws IOException {
        Map<String, String> err = new HashMap<>();
        err.put("error", msg);
        sendJson(ex, status, err);
    }
}