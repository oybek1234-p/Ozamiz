# Implementation Plan - Smart Food Search Ranking

Improve the food search functionality to prioritize prefix-based matches and provide intelligent ranking (e.g., searching "osh" should prioritize items starting with "Osh").

## Proposed Changes

### Feature Logic

#### [MODIFY] [FoodPagingSource.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/food/FoodPagingSource.kt)
- **Refined Scoring Algorithm:**
    - **Exact Match:** Highest priority (Score: 1000).
    - **Starts With (Full Name):** High priority (Score: 500).
    - **Word Starts With:** Medium priority (Score: 200 - 400). Earlier words in the name get higher scores.
    - **No Middle Matches:** Explicitly exclude matches that occur in the middle of a word (e.g., "osh" will not match "Toshkent").
- **Case Insensitive:** Continue to handle queries in lowercase for consistency.

#### [MODIFY] [SearchFoodViewModel.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/food/SearchFoodViewModel.kt)
- Update `searchResultCount` calculation logic to exactly match the criteria used in `FoodPagingSource` to ensure the UI result count is consistent with the displayed list.

## Verification Plan

### Manual Verification
- **Test Case "osh":** Search for "osh". Verify that "Osh palov" (if it exists) appears before "Palov (Osh)".
- **Test Case "non":** Search for "non". Verify that "Non" or "Patir non" appear while "Anjir" (if it was "Anjon") does not match "non" in the middle.
- **Test Case "tovuq":** Search for "tovuq". Verify that "Tovuq sho'rva" appears at the top.
- **Multilingual/Edge Cases:** Ensure parentheses and symbols (e.g., "Palov (Osh)") are handled correctly by the word splitter.
