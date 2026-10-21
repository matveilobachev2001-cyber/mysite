package com.example.mysite.manager;

import java.util.*;

public class GameManager {

    public static class Game {
        public String id;
        public String p1, p2;
        public String phase; // "waiting", "placement", "playing", "finished"
        public String turn;
        public String winner;

        public int[][] b1 = new int[10][10]; // Корабли игрока 1 (1 - корабль)
        public int[][] b2 = new int[10][10]; // Корабли игрока 2
        public int[][] s1 = new int[10][10]; // Выстрелы по игроку 1 (1 - промах, 2 - попадание)
        public int[][] s2 = new int[10][10]; // Выстрелы по игроку 2

        public boolean ready1 = false, ready2 = false;

        public Game(String p1, String p2) {
            this.id = UUID.randomUUID().toString();
            this.p1 = p1;
            this.p2 = p2;
            this.phase = "placement";
            this.turn = p1;
        }
    }

    public static String waitingPlayer = null;
    public static Map<String, Game> playerGames = new HashMap<>();

    public static synchronized Game joinQueue(String nickname) {
        if (playerGames.containsKey(nickname)) return playerGames.get(nickname);

        if (waitingPlayer == null) {
            waitingPlayer = nickname;
            return null;
        } else {
            Game game = new Game(waitingPlayer, nickname);
            playerGames.put(waitingPlayer, game);
            playerGames.put(nickname, game);
            waitingPlayer = null;
            return game;
        }
    }

    public static Game getGame(String nickname) {
        return playerGames.get(nickname);
    }

    public static synchronized boolean placeShips(String nickname, int[][] ships) {
        Game game = playerGames.get(nickname);
        if (game == null || !game.phase.equals("placement")) return false;

        if (nickname.equals(game.p1)) {
            game.b1 = ships;
            game.ready1 = true;
        } else {
            game.b2 = ships;
            game.ready2 = true;
        }

        if (game.ready1 && game.ready2) {
            game.phase = "playing";
        }
        return true;
    }

    public static synchronized boolean shoot(String nickname, int row, int col) {
        Game game = playerGames.get(nickname);
        if (game == null || !game.phase.equals("playing") || !game.turn.equals(nickname)) return false;

        boolean isP1 = nickname.equals(game.p1);
        int[][] myShots = isP1 ? game.s2 : game.s1;       // Стреляем по чужому полю
        int[][] enemyShips = isP1 ? game.b2 : game.b1;    // Корабли врага

        if (myShots[row][col] != 0) return false; // Уже стреляли сюда

        if (enemyShips[row][col] == 1) {
            myShots[row][col] = 2; // Попал
            if (checkWin(myShots, enemyShips)) {
                game.phase = "finished";
                game.winner = nickname;
            }
            // При попадании ход не переходит!
        } else {
            myShots[row][col] = 1; // Промах
            game.turn = isP1 ? game.p2 : game.p1; // Переход хода
        }
        return true;
    }

    private static boolean checkWin(int[][] shots, int[][] ships) {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (ships[r][c] == 1 && shots[r][c] != 2) return false; // Есть целый корабль
            }
        }
        return true;
    }

    public static synchronized void leaveGame(String nickname) {
        if (nickname.equals(waitingPlayer)) waitingPlayer = null;
        Game game = playerGames.remove(nickname);
        if (game != null) {
            String opponent = nickname.equals(game.p1) ? game.p2 : game.p1;
            playerGames.remove(opponent);
        }
    }
}