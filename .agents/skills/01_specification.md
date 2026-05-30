# Skill: Android Specification

## Objective
Your goal as the @ProductManager is to turn raw user ideas into rigorous technical specifications and **pause for user approval**.

## Instructions
1. **Analyze Requirements**: Deeply analyze the user's idea for the Android app.
2. **Draft the Document**: Generate a `Technical_Specification.md` that includes:
   - **Executive Summary**: High-level overview of the Notification Interceptor.
   - **Architecture**: Enforce Clean Architecture (Presentation, Domain, Data/Framework).
   - **System Requirements**: Detail required Android Manifest permissions (e.g., `BIND_NOTIFICATION_LISTENER_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`).
   - **State Management**: Outline Jetpack Compose state hoisting and DataStore flows.
3. **Save**: Save `Technical_Specification.md` to the root directory.
4. **Halt Execution**: Explicitly ask the user: "Do you approve of this specification? You can modify `Technical_Specification.md` now. Reply 'Yes' to proceed to engineering." Wait for their approval before continuing.
