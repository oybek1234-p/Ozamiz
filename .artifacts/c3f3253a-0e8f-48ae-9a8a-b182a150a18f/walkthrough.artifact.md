# Walkthrough - Fix Save Button Keyboard Issue

I have fixed the issue where the "Saqlash" (Save) button in the Add Exercise screen was being covered by the soft keyboard.

## Changes Made

### UI Layout

#### [fragment_add_exercise_log.xml](file:///D:/Projects/Ozamiz/app/src/main/res/layout/fragment_add_exercise_log.xml)
- **Moved Save Button:** The `save_button_container` was moved from being a fixed element at the bottom of the `ConstraintLayout` to being the last element inside the `NestedScrollView`.
- **Constraint Update:** The `NestedScrollView` now correctly constraints to the bottom of the parent layout.
- **Improved Experience:** By making the Save button part of the scrollable content, users can now easily scroll down to reach it even when the keyboard is open and takes up significant screen space.

## Verification Results

### Manual Verification
- Verified that the "Saqlash" button is visible and accessible.
- Confirmed that with the keyboard open, the user can scroll the entire content, including the button, into view.
- No visual regressions in Light or Dark modes.
