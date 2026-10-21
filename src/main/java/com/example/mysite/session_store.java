package com.example.mysite;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class session_store {

    private final Map<String, String> tokenToNickname = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String create(String nickname) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenToNickname.put(token, nickname);
        return token;
    }

    public String getNickname(String token) {
        return token == null ? null : tokenToNickname.get(token);
    }

    public void remove(String token) {
        if (token != null) tokenToNickname.remove(token);
    }
}