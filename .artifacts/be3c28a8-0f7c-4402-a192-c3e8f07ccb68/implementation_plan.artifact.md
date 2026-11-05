# Implementation Plan - Premium Navigation and LogFood UI Adjustments

## Goal Description
1. **HomeFragment Plus Button**: When the plus button is clicked and limits are reached, navigate directly to `PremiumFragment` instead of showing `FoodLimitBottomSheet` ("premium sheet"). If the user presses X or back on `PremiumFragment`, open the list screen (`LogFoodFragment`) where the user can add items.
2. **LogFoodFragment Updates**:
   - **Button Layout**: Rearrange action buttons in the order: **Rasm**, **Yozish**, **Ro'yxat** (Camera, Write, Search).
   - **Limit & Premium Badges**: If limits are reached, display a "Premium" badge on the edge of the Rasm and Yozish cards. Clicking them navigates directly to `PremiumFragment`. Clicking Ro'yxat works normally without premium/limit checks (`ruyhati bossa shart emas`).

## User Review Required
> [!IMPORTANT]
> Navigating from `PremiumFragment` back to `LogFoodFragment` when X or back is pressed is triggered specifically when navigating from HomeFragment plus click (when limit reached).

## Proposed Changes

### Home Feature (`uz.oybek.ozamiz.ui.feature.home`)
#### [MODIFY] [HomeViewModel.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/home/HomeViewModel.kt)
- Update `onAddMealClicked(mealId)`: when `!limits.canLogFood`, emit `HomeUiEffect.NavigateToPremium(mealId)` instead of `ShowFoodLimitSheet`.

#### [MODIFY] [HomeFragment.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/home/HomeFragment.kt)
- Handle `NavigateToPremium(mealId)` effect by passing `mealType` to `premiumFragment`.

### Premium Feature (`uz.oybek.ozamiz.ui.feature.premium`)
#### [MODIFY] [PremiumFragment.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/premium/PremiumFragment.kt)
- Read optional `mealType` argument.
- When X or back is pressed, if `mealType` is present, navigate to `LogFoodFragment` (`R.id.logFoodFragment`) with `LogFoodArgs(mealType)`. Otherwise pop back stack normally.

### Log Food Feature (`uz.oybek.ozamiz.ui.feature.log.food`)
#### [MODIFY] [LogFoodViewModel.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/food/LogFoodViewModel.kt)
- Include `canLog` and `isPremium` in `LogFoodUiState` based on `limitManagerRepo.getLimits()`.

#### [MODIFY] [fragment_log_food.xml](file:///D:/Projects/Ozamiz/app/src/main/res/layout/fragment_log_food.xml)
- Reorder bottom action buttons: Camera (Rasm), Write (Yozish), Search (Ro'yxat).
- Add "Premium" badge TextViews (`tv_premium_badge_camera`, `tv_premium_badge_write`) to camera and write cards.

#### [MODIFY] [LogFoodFragment.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/food/LogFoodFragment.kt)
- Observe `canLog` / `isPremium` in `renderUi`: show/hide premium badges on camera and write cards.
- Update click listeners:
  - Camera (`buttonActionCamera`) and Write (`buttonActionWrite`): if `canLog`, proceed to action (Camera/Write); else navigate to `premiumFragment` with `mealType`.
  - Search / Ro'yxat (`buttonActionSearch`): always navigate to `SearchFoodFragment` without check (`ruyhati bossa shart emas`).

## Verification Plan
### Automated Tests
- Build project using `gradle_build(":app:assembleDebug")`.
### Manual Verification
- Verify HomeFragment plus button opens PremiumFragment when limit reached.
- Verify pressing X/back on PremiumFragment opens LogFoodFragment.
- Verify LogFoodFragment button layout (Rasm, Yozish, Ro'yxat) and premium badges when limit reached, with Ro'yxat working normally.
