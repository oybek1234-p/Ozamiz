# Walkthrough - Plus Buttons for Water and Exercise

I have extended the quick-add functionality to the "Water" and "Exercise" cards on the Home screen, providing a consistent and fast way to log all daily activities.

## Key Changes

### 1. Consistent UI for All Cards
- **Water Card**: Added a prominent **40dp** circular "+" button (`btn_add_water`) to the right side of the Water card in [fragment_home.xml](file:///D:/Projects/Ozamiz/app/src/main/res/layout/fragment_home.xml).
- **Exercise Card**: Added an identical **40dp** circular "+" button (`btn_add_exercise`) to the Exercise card.
- **Unified Styling**: Both buttons use the `@drawable/bg_circle_surface` background and `?attr/colorPrimary` tint, matching the meal cards.

### 2. Streamlined Interaction
- **Direct Logging**: Clicking the "+" button on the Water card now directly opens the `LogWaterDialog`, while the Exercise "+" button navigates straight to the `SelectExerciseFragment`.
- **Card Tap Still Works**: The main area of the cards still functions as before, ensuring flexibility in how users interact with the dashboard.

### 3. Layout Optimization
- **Spaced Content**: Adjusted the text containers in both Water and Exercise cards to ensure that long descriptions or values do not overlap with the new buttons.

## Verification Results

### UI Verification
- [x] Confirmed the "+" button is visible and properly aligned on the Water card.
- [x] Confirmed the "+" button is visible and properly aligned on the Exercise card.
- [x] Verified consistent styling across all dashboard cards (Meals, Water, Exercise).

### Logic Verification
- [x] Verified that clicking the Water "+" button opens the water logging dialog.
- [x] Verified that clicking the Exercise "+" button navigates to the exercise selection screen.

render_diffs(file:///D:/Projects/Ozamiz/app/src/main/res/layout/fragment_home.xml)
render_diffs(file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/home/HomeFragment.kt)
