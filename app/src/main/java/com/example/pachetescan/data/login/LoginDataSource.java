package com.example.pachetescan.data.login;

import android.content.Context;

import com.example.pachetescan.config.ServerConfig;
import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.login.model.LoggedInUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {
    private final Context context;
    public LoginDataSource(Context context) {
        this.context = context;
    }

    public Result<LoggedInUser> login(String username, String password) {
        HttpURLConnection conn = null;

        try {
            String baseUrl = ServerConfig.getBaseUrl(context);
            URL url = new URL(baseUrl + "/api/android/login");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject json = new JSONObject(response.toString());
            boolean success = json.optBoolean("success", false);

            android.util.Log.d("LoginDataSource", "status=" + status + " body=" + response.toString());

            if (status == 200 && success) {
                String token = json.getString("token");
                String displayName = json.optString("displayName", username);

                LoggedInUser user = new LoggedInUser(username, displayName, token);
                return new Result.Success<>(user);
            } else {
                String errorMessage = "Login failed";
                JSONObject errors = json.optJSONObject("errors");
                if (errors != null && errors.keys().hasNext()) {
                    String firstKey = errors.keys().next();
                    errorMessage = errors.optString(firstKey, errorMessage);
                }
                return new Result.Error(new IOException(errorMessage));
            }

        } catch (SocketTimeoutException e) {
            android.util.Log.e("LoginDataSource", "Timeout", e);
            return new Result.Error(new IOException("Server timeout — please try again", e));

        } catch (JSONException e) {
            android.util.Log.e("LoginDataSource", "JSON error", e);
            return new Result.Error(new IOException("Invalid response format from server", e));

        } catch (IOException e) {
            android.util.Log.e("LoginDataSource", "IO error: " + e.getMessage(), e);
            return new Result.Error(new IOException("Network error — check your connection", e));

        } catch (Exception e) {
            android.util.Log.e("LoginDataSource", "Unexpected error", e);
            return new Result.Error(new IOException("Error logging in", e));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}