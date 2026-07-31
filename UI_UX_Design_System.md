# NotiVib - UI/UX Design System & Guidelines

This document serves as the single source of truth for NotiVib's user interface and user experience design, based on the approved design mockups.

## 1. Core Visual Identity

### 1.1 Color Palette
The app uses a dark-mode-first aesthetic with a highly vibrant, high-contrast neon green accent to create a "premium hacker/utility" vibe.

*   **Primary Accent (Neon Green):** `#D9FF0B` - Used for primary actions, active states, active toggles, FABs, and primary buttons.
*   **Secondary/Pale Accent:** `#D9EA7D` - Used for secondary buttons (e.g., Cancel in modals) or subtle text accents.
*   **Danger/Destructive:** `#FF0B0B` - Used for delete buttons, error states, and destructive modal actions.
*   **Background (Dark Gray 1):** `#20201E` - The deepest background color, used for the main app scaffolding.
*   **Surface 1 (Dark Gray 2):** `#5B5B5B` - Used for borders, dividers, or elevated surfaces.
*   **Surface 2 (Dark Gray 3):** `#444444` - Used for cards (e.g., inactive rule cards, settings cards, text fields).
*   **Surface 3 (Dark Gray 4):** `#5E5E5E` - Used for lighter elevated components or disabled states.

### 1.2 Typography
We use a combination of serif and sans-serif fonts to create a modern, structured look.
*   **Google Fonts:** Enabled via Compose.
*   **Host Grotesk:** Primary font for Body, Labels, and general UI elements.
*   **Source Serif 4:** Secondary font, used for the App Title/Branding and highly stylized headings.

### 1.3 Iconography
Icons are primarily sourced from Material Symbols. Key icons explicitly requested:
*   `mop` (Cleaning/Clearing history)
*   `list_alt_add` (Adding to list/history)
*   `ecg_heart` or `monitor_heart` (Engine status/health)
*   `arrow_back_ios_new` (Standard back navigation)

---

## 2. Screen & Component Architecture

### 2.1 Homepage (Active / Inactive)
*   **Top App Bar:** Brand logo/text on the left. `ecg_heart` (Diagnostics) and Settings gear on the right.
*   **Navigation:** A full-width segmented control (Pill shape) to toggle between "Active" and "Inactive" rules. Active segment uses `#D9FF0B`.
*   **Rule Cards:**
    *   **Active Rule:** Bright background (Primary/Secondary Accent), dark text.
    *   **Inactive Rule:** Dark Gray (`#444444`) background, white/light text.
    *   **Contents:** App Icon + Name, Package name, Trigger Keywords, Active Days (M, T, W, Th, F, S, Su), Status Chip, and a 3-dot overflow menu.
*   **FAB:** "+ New Rule" extended floating action button pinned to the bottom right (`#D9FF0B`).

### 2.2 Edit / Create Rule Screen
*   **Top App Bar:** `arrow_back_ios_new`, "Edit Rule" title, and a Red Delete icon (if editing an existing rule).
*   **Form Elements:**
    *   **Keywords:** A large text area (`#444444` background) for comma-separated trigger keywords.
    *   **Target Application:** A dropdown selector with the app icon and name.
    *   **Active Days:** A horizontal row of 7 circular toggles (M-Su). Active state is `#D9FF0B`, inactive state is `#5E5E5E`.
*   **Bottom Bar:** "Cancel" text button and a prominent "Save Rule" filled button (`#D9FF0B`).

### 2.3 Modals & Dialogs
*   **Delete Rule Modal:**
    *   Dark background (`#20201E` or `#444444`).
    *   Clear Title and descriptive warning text.
    *   "Cancel" button (`#D9EA7D`) and "Delete" button (`#FF0B0B`).
*   **Engine Diagnostics Modal:**
    *   Appears as an overlay/bottom sheet.
    *   "Engine Diagnostics" title with a download/export icon.
    *   Terminal/Log window area.
    *   Full-width "Dismiss" button (`#D9FF0B`) at the bottom.

### 2.4 Settings Screen
*   **Top App Bar:** `arrow_back_ios_new`, "Settings" title.
*   **Settings Cards:** Rounded cards (`#444444`) grouping related settings.
    *   **Toggles:** "Engine Active" and "Persistent Notification" with standard Material switches (track is `#D9FF0B` when active).
    *   **Permissions:** Prominent warning text/cards for missing permissions. Full-width buttons inside the cards: "Grant Permission" and "Allow Background Usage" (`#D9FF0B`).
    *   **Navigation:** A full-width "Notification History" button at the bottom of the scrollable list.

### 2.5 Notification History Screen
*   **Top App Bar:** `arrow_back_ios_new`, "Notification History" title. Action icons: `list_alt_add` and `mop` on the right.
*   **List Items:** Simple, rounded rectangular cards (`#444444` background) stacked vertically.

---

## 3. Interaction & UX Guidelines
*   **Contrast is Key:** Ensure text on the neon green (`#D9FF0B`) background is dark (e.g., `#20201E`) for maximum readability.
*   **Feedback:** All interactive elements (buttons, cards, toggles) should have ripple effects and clear visual state changes.
*   **Destructive Actions:** Always protect destructive actions (like deleting a rule) behind a confirmation modal, and style the final action in pure Red (`#FF0B0B`).
