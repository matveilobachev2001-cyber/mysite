package com.example.mysite;

public class model_user {
    private String nickname;
    private long createdAt;

    public model_user() {}  // нужен для Jackson

    public model_user(String nickname, long createdAt) {
        this.nickname = nickname;
        this.createdAt = createdAt;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
