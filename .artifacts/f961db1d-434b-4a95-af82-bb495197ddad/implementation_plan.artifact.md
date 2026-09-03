# Implementation Plan - Fix Community Posts Sort Order

Fix the incorrect sort order in the Community "Ozganlar" (Weight Loss) screen where posts appear in random order instead of newest first.

## User Review Required

> [!IMPORTANT]
> The fix involves changing the Firestore query to prioritize sorting by `timestamp`. To achieve this, I will remove the database-level filter for "posts with photos" because Firestore requires the filtered field to be the primary sort key. Instead, I will filter out posts without photos on the client side.

## Proposed Changes

### [Community Feature]

#### [MODIFY] [CommunityPostRepository.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/data/community/CommunityPostRepository.kt)
- Remove `whereNotEqualTo("photos", emptyList<String>())` from the main query in `getPosts`.
- Remove `orderBy("photos")` which was causing the "random" sort order.
- Set `orderBy("timestamp", Query.Direction.DESCENDING)` as the primary sort field.
- Filter the resulting list to exclude posts where both `photos` is empty AND `photoUrl` is blank (client-side filtering).

#### [MODIFY] [CommunityPost.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/data/community/model/CommunityPost.kt)
- (Optional but recommended) Ensure `timestamp` is used consistently.

## Verification Plan

### Manual Verification
- Deploy the app.
- Navigate to the "Community" screen (Hamjamiyat/Ozganlar).
- Verify that the latest posts appear at the top.
- Verify that scroll-to-load-more (paging) still works correctly with the new sort order.
- Check that posts without images are correctly filtered out from the feed.
