# PacheteScan: Login Flow & Architecture Documentation

The app uses an **MVVM (Model-View-ViewModel) architecture** pattern combined with the standard Android Repository pattern. This ensures that the UI is decoupled from the business logic and networking data layers.

Here are the primary components involved:

## 1. Initialization and Setup
**Starting Point:** `LoginActivity`
*   When the activity is created, it inflates its layout (`ActivityLoginBinding`) and sets up window insets.
*   **Dependency Injection:** It instantiates the `LoginViewModel` using the `LoginViewModelFactory`.
*   The `LoginViewModelFactory` acts as a manual dependency injector. It creates the `LoginDataSource`, injects it into a singleton `LoginRepository`, and passes the repository to the `LoginViewModel`.

## 2. User Input Validation (Real-time)
**Files involved:** `LoginActivity` -> `LoginViewModel` -> `LoginFormState`
*   `LoginActivity` attaches a `TextWatcher` to the `username` and `password` EditText fields. 
*   Every time the user types, it calls `loginViewModel.loginDataChanged(username, password)`.
*   Inside `LoginViewModel`, it validates the input:
    *   Username must either be a valid email format or a non-empty string.
    *   Password must be greater than 5 characters long.
*   The ViewModel updates a LiveData object called `LoginFormState`.
*   The `LoginActivity` observes this state to dynamically enable/disable the "Login" button and display inline error messages on the text fields.

## 3. The Login Execution Flow
When the user clicks the "Login" button (or presses "Done" on the keyboard), the following sequence occurs:

### A. Triggering the Login
*   `LoginActivity` makes the loading `ProgressBar` visible and calls `loginViewModel.login(username, password)`.

### B. Hand-off to the Repository
*   `LoginViewModel` calls `loginRepository.login(username, password, callback)`. 
*   Because network calls cannot be made on the main UI thread, `LoginRepository` spins up a background thread using an `ExecutorService` (`Executors.newSingleThreadExecutor()`).

### C. The API Call (Network Layer)
**Execution jumps to:** `LoginDataSource`
*   On the background thread, the repository executes `dataSource.login(username, password)`.
*   `LoginDataSource` establishes a manual HTTP POST request using `HttpURLConnection`.
*   **The Request:**
    *   Target URL: `http://10.10.240.172:5021/api/android/login`
    *   Headers: `Content-Type: application/json; charset=utf-8`
    *   Body: It creates a `JSONObject` with the keys `username` and `password` and writes it to the output stream.
*   **The Response Handling:**
    *   It waits for the server response (with a 10-second timeout).
    *   If the HTTP status is `200` and the parsed JSON object has `"success": true`, it extracts the `token` and `displayName`.
    *   It wraps this data in a `LoggedInUser` model and returns a `Result.Success`.
    *   If it fails (HTTP error, parsing error, timeouts), it safely parses the server's `"errors"` JSON object and returns a `Result.Error` with the exact message (e.g., "Invalid credentials").

### D. Repository Caching & Main Thread Callback
**Execution jumps back to:** `LoginRepository`
*   Once `LoginDataSource` returns the `Result`, the repository checks if it's a success. If it is, it caches the `LoggedInUser` object in memory.
*   Because the execution is still on a background thread, the repository uses a `Handler(Looper.getMainLooper())` to post the final callback back to the main UI thread.

### E. ViewModel State Update
**Execution jumps back to:** `LoginViewModel`
*   The ViewModel's callback is triggered on the main thread.
*   If the API login succeeded, it maps the `LoggedInUser` object to a `LoggedInUserView` object (which exposes only what the UI needs to see).
*   It posts this data to the `loginResult` LiveData. 
*   If it failed, it posts an integer string resource (like `R.string.login_failed`) as an error to the LiveData.

### F. UI Update & Navigation
**Execution jumps back to:** `LoginActivity`
*   `LoginActivity` is actively observing `loginResult`.
*   When the data updates, it hides the loading `ProgressBar`.
*   **On Failure:** It pops up a Toast message letting the user know the login failed.
*   **On Success:**
    *   It retrieves the `LoggedInUserView`.
    *   It calls `TokenStorage.saveToken(getApplicationContext(), userView.getToken())` to persist the JWT authentication token securely on the device so the user remains logged in for future API calls.
    *   It displays a Welcome Toast.
    *   It creates an `Intent` to jump to `MenuActivity`, starts the activity, and calls `finish()` so the user can't navigate back to the login screen by pressing the Android back button.

## Summary
The architecture adheres to clean separation of concerns. `LoginActivity` handles only UI states, `LoginViewModel` maps data to UI states, `LoginRepository` manages threads and caching, and `LoginDataSource` strictly handles the manual `HttpURLConnection` to the internal API (`10.10.240.172`).