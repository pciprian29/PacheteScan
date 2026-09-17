package com.example.pachetescan.data.login;

import android.os.Handler;
import android.os.Looper;

import com.example.pachetescan.data.Result;
import com.example.pachetescan.data.login.model.LoggedInUser;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepository {

    private static volatile LoginRepository instance;

    private LoginDataSource dataSource;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private LoggedInUser user = null;

    // private constructor : singleton access
    private LoginRepository(LoginDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static LoginRepository getInstance(LoginDataSource dataSource) {
        if (instance == null) {
            instance = new LoginRepository(dataSource);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public void logout() {
        user = null;
        dataSource.logout();
    }

    private void setLoggedInUser(LoggedInUser user) {
        this.user = user;
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    public interface LoginRepositoryCallback {
        void onResult(Result<LoggedInUser> result);
    }

    public void login(String username, String password, LoginRepositoryCallback callback) {
        executor.execute(() -> {
            Result<LoggedInUser> result;

            try {
                result = dataSource.login(username, password);

                if (result instanceof Result.Success) {
                    setLoggedInUser(((Result.Success<LoggedInUser>) result).getData());
                }

            } catch (Exception e) {
                result = new Result.Error(new IOException("Unexpected error during login", e));
            }

            Result<LoggedInUser> finalResult = result;
            mainHandler.post(() -> callback.onResult(finalResult));
        });
    }
}