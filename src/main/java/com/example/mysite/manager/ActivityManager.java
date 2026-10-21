package com.example.mysite.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityManager {
    // Храним ник -> активность
    public static class UserActivity {
        public String nickname;
        public String action;
        public long lastSeen; // время последней активности

        public UserActivity(String nickname, String action, long lastSeen) {
            this.nickname = nickname;
            this.action = action;
            this.lastSeen = lastSeen;
        }
    }

    private static final Map<String, UserActivity> activeUsers = new ConcurrentHashMap<>();

    // Обновить статус пользователя
    public static void update(String nickname, String action) {
        activeUsers.put(nickname, new UserActivity(nickname, action, System.currentTimeMillis()));
    }

    // Получить список тех, кто активен за последние 15 секунд
    public static List<UserActivity> getActiveUsers() {
        long now = System.currentTimeMillis();
        List<UserActivity> result = new ArrayList<>();

        // Удаляем тех, кто отвалился
        activeUsers.entrySet().removeIf(entry -> now - entry.getValue().lastSeen > 15000);

        for (UserActivity ua : activeUsers.values()) {
            result.add(ua);
        }
        return result;
    }
}