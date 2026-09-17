package com.example.pachetescan.data.login.model;

public class LoggedInUser {

    private String userId;
    private String displayName;
    private String token;

    public LoggedInUser(String userId, String displayName, String token) {
        this.userId = userId;
        this.displayName = displayName;
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToken() {
        return token;
    }
}