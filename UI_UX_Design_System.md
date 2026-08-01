# NotiVib — UI/UX Design System & Guidelines

This document serves as the single source of truth for NotiVib's user interface and user experience design.

---

## 1. Core Visual Identity

### 1.1 Color Palette
The app uses a dark-mode-first aesthetic with a vibrant neon green accent.

| Token | Hex | Usage |
|-------|-----|-------|
| **Primary Accent** | `#D9FF0B` | Primary buttons, active toggles, FAB, active day circles, text field values, switch tracks |
| **Background** | `#20201E` | Main app scaffolding, deepest background |
| **Surface (Input Fields)** | `#5B5B5B` | Text field backgrounds, time picker buttons, day circle inactive state |
| **Surface (Cards)** | `#444444` | Rule cards (inactive), settings cards, notification log cards |
| **Danger** | `#FF0B0B` | Delete buttons, error states, destructive actions |
| **Text Primary** | `#FFFFFF` | Headings, labels, primary text |
| **Text Secondary** | `#E0E0E0` | Body text, descriptions, helper text |
| **Text Muted** | `Color.Gray` | Placeholders, package names |

### 1.2 Typography
Two font families are used throughout the app:

| Font | Usage |
|------|-------|
| **Source Serif 4** | App title, screen titles, button labels, rule names, headings, time values |
| **Host Grotesk** | Body text, form labels, descriptions, helper text, placeholders |

### 1.3 Iconography
Icons sourced from Material Symbols and custom vector icons:

| Icon | Usage |
|------|-------|
| `mop` | Clear history |
| `list_alt_add` | Add to log/history |
| `ecg_heart` | Engine diagnostics |
| `arrow_back_ios_new` | Back navigation |
| `Icons.Outlined.Delete` | Delete rule (red tint) |
| `Icons.Outlined.Search` | App search in selector |
| `Icons.Outlined.Apps` | "Any App" target |
| `Icons.Outlined.ArrowDropDown` | App selector expand |
| `Icons.Outlined.Settings` | Navigate to settings |
| `Icons.Outlined.Security` | Permissions screen icon |

### 1.4 Shape System
| Element | Corner Radius |
|---------|--------------|
| Cards | `20dp` rounded |
| Text fields | `12dp` rounded |
| Buttons (primary) | `16dp` rounded |
| Settings buttons | `24dp` rounded |
| Day circles | `CircleShape` |
| Dialogs | `24dp` rounded |
| App selector dialog | `24dp` rounded |

---

## 2. Screen & Component Architecture

### 2.1 Permissions Screen (Onboarding)
Shown when required permissions are missing. Blocks access to the main app until granted.
- Centered layout with security icon, welcome text, and description.
- Permission cards for: System Notifications, Battery Optimization, Notification Interception.
- Each card has icon, title, description, and forward arrow. Tapping opens the relevant system settings.

### 2.2 Rules List Screen (Homepage)
- **Top App Bar**: Brand text on left. Diagnostics (`ecg_heart`) and Settings (`gear`) icons on right.
- **Tab Navigation**: Full-width `TabRow` with "Active (N)" and "Inactive (N)" tabs. Selected tab uses primary accent color.
- **Rule Cards**:
  - Display: App icon, app name, package name, rule name, trigger keywords (displayed as oblong chip pills, max 4 + "+N more" overflow pill), active days (M/T/W/Th/F/S/Su circles), status indicators.
  - Active rules: Bright accent background with dark text.
  - Inactive rules: Dark surface (`#444444`) background with light text.
  - Tap to navigate to EditRuleScreen.
  - Toggle switch to enable/disable without opening editor.
  - Dropdown menu (⋮) options: "Edit Rule", "Copy Rule" (copies rule payload to clipboard as JSON), and "Delete Rule".
- **FAB**: Large circular floating action button (`#D9FF0B`) with a `+` icon, bottom-right. When clicked, it animates (rotates 45 degrees) and expands an upward menu with two mini-FAB options: "Create Manually" and "Paste Copied Rule".
- **Empty State**: Centered message when no rules exist in the current tab.

### 2.3 Edit / Create Rule Screen
- **Top App Bar**: Back arrow, "Edit Rule" or "New Rule" title, red Delete icon (edit mode only).
- **Form Fields** (scrollable):
  1. **Rule Name** — Text field with placeholder "e.g. Work Rule"
  2. **Trigger Keywords** — Dynamic chip/tag input. Type text & tap `+` (or press keyboard Done) to add chip. Each chip features an `✕` remove button.
  3. **Ignored Keywords** — Dynamic chip/tag input for exclusion keywords. Helper text: "Notifications containing these words will be skipped even if they match trigger keywords."
  4. **Target Application** — Card-style selector showing app icon, name, and package. Tapping opens full-screen dialog with search bar and scrollable app list. "ALL APPLICATIONS" option at top.
  5. **Active Days** — Row of 7 circular toggles (M, T, W, Th, F, S, Su). Active: `#D9FF0B` with black text. Inactive: `#5B5B5B` with white text. Minimum 1 day must remain selected.
  6. **Custom Schedule Per Day** — Checkbox to enable per-day time windows. When disabled: single global Start/End time picker. When enabled: per-day rows with individual start/end time buttons.
  7. **Remind When Schedule Starts/Ends** — Checkbox option.
  8. **Mute Notifications Outside Schedule** — Checkbox option. Silently deletes notifications from the target app outside the active window.
  9. **Vibration Only Mode** — Switch toggle. Disables audio alarm.
- **Bottom Bar**: "Cancel" text button + "Save Rule" filled button (`#D9FF0B`).
- **Unsaved Changes Dialog**: Warns before navigating away with unsaved edits. "Keep Editing" (accent) / "Discard" (red) buttons.
- **Delete Confirmation Dialog**: "Delete Rule?" with warning text. "Cancel" (accent) / "Delete" (red) buttons.
- **Time Picker Dialogs**: Dark themed (`#161618` surface), 24-hour format, accent-colored selector and confirm button.

### 2.4 Settings Screen
- **Top App Bar**: Back arrow, "Settings" title.
- **Engine Status Card**: Status indicator dot (green=active, red=suspended), "Engine Active"/"Engine Suspended" text, toggle switch.
- **Persistent Notification Card**: Toggle to keep foreground notification visible.
- **Permission Cards** (shown conditionally when permissions are missing):
  - Display Over Other Apps — Full-width "Grant Permission" button.
  - Battery Optimization — Full-width "Allow Background Usage" button.
- **Notification History Button**: Full-width accent button at bottom of screen.

### 2.5 Notification History Screen
- **Top App Bar**: Back arrow, "Notification History" title. Action icons: `list_alt_add` (toggle tracked apps) and `mop` (clear history) on right.
- **Tab Navigation**: "Intercept Log" and "System Log" tabs.
- **List Items**: Rounded cards (`#444444`) showing app name, title, text, timestamp, and matched rule info.
- **Swipe-to-delete**: Individual log entries can be dismissed.

### 2.6 Alarm Activity (Full-Screen)
- Full-screen overlay displaying alarm information (app name, keyword, rule name).
- "Dismiss" button to stop the alarm.
- Works with screen off via `FLAG_SHOW_WHEN_LOCKED` and `FLAG_TURN_SCREEN_ON`.

---

## 3. Interaction & UX Guidelines

- **Contrast**: Text on neon green (`#D9FF0B`) background must be dark (`#20201E` or `Color.Black`).
- **Feedback**: All interactive elements have ripple effects and clear visual state changes.
- **Destructive Actions**: Protected behind confirmation dialogs. Final action styled in red (`#FF0B0B`).
- **Unsaved Changes**: Back navigation from edit screen warns if there are unsaved changes.
- **Minimum Constraints**: At least 1 active day must remain selected when toggling day circles.
- **Consistent Spacing**: `24dp` between major sections, `8dp` between labels and their fields, `16dp` between option rows.
- **Clickable Rows**: Checkbox/switch rows are fully clickable (not just the control itself).
