# Implementation Plan - Optimize Write Food UI for Keyboard

Ensure the keyboard opens automatically in `WriteFoodFragment` and the "Tahlil qilish" button stays perfectly positioned above the keyboard.

## User Review Required

> [!NOTE]
> - **Automatic Keyboard**: The keyboard will now slide up as soon as the screen opens.
> - **Pinned Button**: The "Tahlil qilish" button will move up/down with the keyboard, ensuring it's always accessible without closing the keyboard.
> - **Styling**: The button will have a slight margin from the keyboard for better visual clarity.

## Proposed Changes

### UI Logic

#### [MODIFY] [WriteFoodFragment.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/food/WriteFoodFragment.kt)
- Use a `postDelayed` call for `showKeyboard()` to ensure reliability during animations.
- Implement `ViewCompat.setOnApplyWindowInsetsListener` to dynamically adjust the root layout's bottom padding based on the IME (keyboard) height. This ensures the bottom-constrained button "floats" above the keyboard.

### UI Layout

#### [MODIFY] [fragment_write_food.xml](file:///D:/Projects/Ozamiz/app/src/main/res/layout/fragment_write_food.xml)
- Set `android:id="@+id/root"` or similar to the root `ConstraintLayout` to apply insets.
- Ensure the `EditText` has proper focus properties.

## Verification Plan

### Manual Verification
1. Navigate to the "Yozish" (Write) screen.
2. Verify the keyboard opens immediately.
3. Observe the "Tahlil qilish" button; it should be pushed up by the keyboard.
4. Close the keyboard (back button); the button should slide back to the bottom of the screen.
5. Verify submitting text still works and navigates to analysis.
