# SN Creator Security Specification

## 1. Data & Privacy Invariants
1. **Zero Content History Persistence:** AI prompts and generated gaming content (titles, hashtags, scripts, thumbnail prompts, video ideas) must never be persisted in client databases, Firestore, or backend storage.
2. **Trusted Backend Quota Enforcement:** Daily quota (`10` successful generations per `Asia/Karachi` calendar day) is enforced exclusively on the authenticated backend (`Cloudflare Workers + D1` / server-side SQLite) using verified RS256 Firebase ID tokens (`sub` UID).
3. **Zero Direct Client Firestore Access:** All client reads and writes to Firestore (`/{document=**}`) are unconditionally denied (`allow read, write: if false;`).

## 2. Adversarial "Dirty Dozen" Payloads Blocked
1. Unauthenticated read on `/quotas/uid_1` -> `PERMISSION_DENIED`
2. Unauthenticated write on `/quotas/uid_1` -> `PERMISSION_DENIED`
3. Authenticated client attempting self-reset `{ used: 0 }` on `/quotas/{uid}` -> `PERMISSION_DENIED`
4. Authenticated client attempting history write on `/history/{docId}` -> `PERMISSION_DENIED`
5. Spoofed `ownerId` write on `/users/victim_uid` -> `PERMISSION_DENIED`
6. Unverified email spoof write on `/admins/{uid}` -> `PERMISSION_DENIED`
7. Shadow field injection `{ isVerified: true, unlimitedQuota: true }` -> `PERMISSION_DENIED`
8. 1 MB string payload resource exhaustion write -> `PERMISSION_DENIED`
9. List scraping query across `/users` -> `PERMISSION_DENIED`
10. Delete attempt on `/quotas/{uid}` -> `PERMISSION_DENIED`
11. Backend `/v1/generate` call without `Authorization: Bearer <Firebase JWT>` -> `401 AUTH_INVALID`
12. Simultaneous duplicate `requestId` replay on `/v1/generate` -> `409 DUPLICATE_REQUEST`
