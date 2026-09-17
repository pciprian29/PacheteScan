package com.example.pachetescan.ui.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private String displayName;
    //... other data fields that may be accessible to the UI
    private String token;

    LoggedInUserView(String displayName, String token) {
        this.displayName = displayName;
        this.token = token;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToken() {
        return token;
    }
}