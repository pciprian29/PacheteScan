package com.example.pachetescan.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenStorage {

    private static final String PREFS_NAME = "secure_auth_prefs";
    private static final String KEY_TOKEN = "auth_token";

    public static void saveToken(Context context, String token) {
        try {
            SharedPreferences prefs = getPrefs(context);
            prefs.edit().putString(KEY_TOKEN, token).apply();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to save token", e);
        }
    }

    public static String getToken(Context context) {
        try {
            SharedPreferences prefs = getPrefs(context);
            return prefs.getString(KEY_TOKEN, null);
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }
    }

    public static void clearToken(Context context) {
        try {
            SharedPreferences prefs = getPrefs(context);
            prefs.edit().remove(KEY_TOKEN).apply();
        } catch (GeneralSecurityException | IOException e) {
            // ignore
        }
    }

    private static SharedPreferences getPrefs(Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}