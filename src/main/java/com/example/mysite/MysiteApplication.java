package com.example.mysite;

import com.example.mysite.handler.ActivityHandler;
import com.example.mysite.handler.CurrencyHandler;
import com.example.mysite.handler.GameHandler;
import com.example.mysite.handler.WeatherHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class MysiteApplication {

    public static void main(String[] args) throws Exception {
        // 1. Динамический порт для Render
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            port = Integer.parseInt(envPort);
        }

        // 2. Абсолютные пути (чтобы сервер находил файлы внутри Docker-контейнера)
        String rootDir = System.getProperty("user.dir");
        Path dataPath = Path.of(rootDir, "data", "users.json");
        Path frontendPath = Path.of(rootDir, "frontend");

        // Создаем папку data, если её вдруг нет
        if (!dataPath.getParent().toFile().exists()) {
            dataPath.getParent().toFile().mkdirs();
        }

        // 3. Инициализация (используем ваши классы, они в этом же пакете)
        store_user userStore = new json_user_store(dataPath);
        session_store sessions = new session_store();
        Auth_handler authHandler = new Auth_handler(userStore, sessions);

        // 4. Запуск сервера
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/auth", authHandler);
        server.createContext("/api/weather", new WeatherHandler());
        server.createContext("/api/currency", new CurrencyHandler());
        server.createContext("/", new Static_handler(frontendPath));
        server.createContext("/api/activity", new ActivityHandler(sessions));
        server.createContext("/api/game", new GameHandler(sessions));

        // 5. ВАЖНО: Пул потоков. Без него игроки будут блокировать друг друга.
        server.setExecutor(Executors.newFixedThreadPool(8));

        server.start();
        System.out.println("Сервер запущен на порту: " + port);
        System.out.println("Логин: http://localhost:" + port + "/login.html");
    }
}