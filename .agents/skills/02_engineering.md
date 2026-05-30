# Skill: Android Generation

## Objective
Your goal as the @AndroidEngineer is to build the app based strictly on `Technical_Specification.md`, ensuring a highly engaging, modern, and animated UI.

## Instructions
1. **Scaffold**: Set up the standard Android Gradle project structure (app/src/main/...).
2. **Data & Domain Layers**: Implement the Preferences DataStore and pure Kotlin Use Cases for notification evaluation.
3. **Framework Layer**: Implement `NotificationListenerService` and the Foreground Service (MediaPlayer + Vibrator).
4. **UI Layer (Eye-Catching & Engaging)**: 
   - **Theme**: Implement a sleek Dark Mode default theme using Material 3. Use vibrant accent colors (e.g., neon green or electric blue) to indicate when the interceptor is "Active".
   - **Animations**: Use Compose `AnimatedVisibility` for list items when adding/deleting rules. 
   - **Visual Feedback**: On the main dashboard, implement an `InfiniteTransition` pulsing animation (like a radar or glowing ring) around a central icon to visually communicate that the app is actively listening to notifications.
   - **Components**: Use modern Material 3 Cards with elevation, rounded corners, and clear typography for the rules list.
5. **Save**: Ensure all code compiles and is saved to the file system.
