package com.example.mysite;

public interface store_user {
    model_user find(String nickname);
    model_user findOrCreate(String nickname);
}