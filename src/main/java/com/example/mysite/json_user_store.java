package com.example.mysite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class json_user_store implements store_user {

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, model_user> users = new LinkedHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public json_user_store(Path file) throws IOException {
        this.file = file;
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(file)) return;
        JsonNode root = mapper.readTree(file.toFile());
        root.fields().forEachRemaining(entry -> {
            JsonNode node = entry.getValue();
            model_user u = new model_user(
                    node.path("nickname").asText(entry.getKey()),
                    node.path("createdAt").asLong()
            );
            users.put(entry.getKey(), u);
        });
        System.out.println("Загружено пользователей: " + users.size());
    }

    private void save() throws IOException {
        ObjectNode root = mapper.createObjectNode();
        users.forEach((nick, user) -> {
            ObjectNode node = root.putObject(nick);
            node.put("nickname", user.getNickname());
            node.put("createdAt", user.getCreatedAt());
        });
        Files.createDirectories(file.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
    }

    @Override
    public model_user find(String nickname) {
        lock.readLock().lock();
        try {
            return users.get(nickname);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public model_user findOrCreate(String nickname) {
        lock.writeLock().lock();
        try {
            model_user existing = users.get(nickname);
            if (existing != null) return existing;

            model_user created = new model_user(nickname, System.currentTimeMillis());
            users.put(nickname, created);
            save();
            return created;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить пользователя", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
