# Technical Specification: Notification Alarm Interceptor

## Executive Summary
The Notification Alarm Interceptor ("NotiVib") is a persistent Android application designed to continuously monitor incoming device notifications and trigger a robust, un-ignorable hardware alarm (audio and vibration) when specific, user-defined conditions are met. This ensures the user never misses critical messages from specific apps or individuals. The app will feature a highly engaging, animated UI built with Jetpack Compose, showcasing a pulsing radar effect to indicate active monitoring.

## Architecture
The application will strictly adhere to Clean Architecture combined with MVVM (Model-View-ViewModel) to ensure separation of concerns, testability, and maintainability.

1.  **Presentation (UI) Layer**: 
    *   Built entirely with Jetpack Compose.
    *   **Theme**: Dark Mode default with vibrant, electric accents (neon green/blue) for active states.
    *   **UI Components**: Material 3 Cards, `AnimatedVisibility` for list manipulations, and an `InfiniteTransition` for the radar pulsing animation.
    *   **ViewModels**: Manage state and expose data via Kotlin `StateFlow`.
2.  **Domain Layer**:
    *   Pure Kotlin modules containing core business logic.
    *   **Use Cases**: `EvaluateNotificationUseCase` (checks incoming data against rules and time windows), `ManageRulesUseCase`.
    *   **Models**: Clean data classes representing `AlarmRule` (target app package, keyword, time window).
    *   No Android framework dependencies will exist in this layer.
3.  **Data & Framework Layer**:
    *   **DataStore**: Preferences DataStore for asynchronous, safe key-value persistence of user rules and settings, exposed as a `Flow`.
    *   **Services**: 
        *   `NotificationListenerService`: To intercept system notifications.
        *   `Foreground Service`: For media playback and vibration, ensuring the app remains alive during the alarm.

## System Requirements & Permissions
Due to Android 14+ background execution constraints, the following permissions and system configurations are mandatory:
*   `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`: To read incoming notifications.
*   `android.permission.FOREGROUND_SERVICE`: Base requirement for foreground services.
*   `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required to run the alarm service without being killed by the OS.
*   `android.permission.POST_NOTIFICATIONS`: (Android 13+) To display the ongoing notification and the kill-switch for the alarm.
*   `android.permission.VIBRATE`: To trigger the vibration waveform.
*   **Battery Optimization Exemption**: The user must be prompted to grant the app an exemption (`Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) to prevent Doze mode from killing the background listener.

## State Management
*   **DataStore to Domain**: Rule changes in DataStore are emitted as a Kotlin `Flow`.
*   **Domain to UI**: ViewModels collect this `Flow` and expose it as a `StateFlow` to Jetpack Compose.
*   **Unidirectional Data Flow**: The UI observes the state. User interactions (e.g., adding a rule) call ViewModel functions, which update the DataStore. The DataStore emits the new state, which flows back to the UI.
*   **Service Synchronization**: Background services collect the same DataStore `Flow`, ensuring they always evaluate notifications against the most up-to-date rules without requiring a service restart.

## Pipeline Checkpoint
This specification must be approved before proceeding to the Engineering phase.
