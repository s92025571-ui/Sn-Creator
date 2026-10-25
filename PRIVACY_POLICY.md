# Privacy Policy — SN Creator

**Effective Date:** October 25, 2026  
**Application Name:** SN Creator  
**Android Package ID:** `com.sncreator.app`

## 1. Overview & Data Minimization Principle
SN Creator provides AI-powered content ideation and scriptwriting tools for gaming creators. We operate on a strict data-minimization architecture: we collect and process only the minimum information technically required to verify your account, enforce the daily free generation quota, and fulfill active AI generation requests.

## 2. Information Processed

### A. Verified Account Identifier
- **What is processed:** When you sign in using **Continue with Google** (powered by Firebase Authentication), the Android app displays your Google profile display name and email address locally on your screen. When your device sends an AI generation or quota request to our backend, it sends a short-lived cryptographic Firebase ID token (`Authorization: Bearer <token>`).
- **What the backend stores:** Our backend verifies the token signature and stores **only your opaque Firebase User ID (`uid`)** in the quota table. We do not store your Google password, profile photo, or contact list in our quota database.

### B. Daily Quota Records (`Asia/Karachi`)
- **What is stored:** For each verified `uid`, the quota database (`daily_quotas` and `request_reservations`) stores:
  - `uid`: Verified Firebase account identifier.
  - `pkt_date`: Current Pakistan calendar date (`YYYY-MM-DD` in `Asia/Karachi`).
  - `used_count` and `in_flight_count`: Integer counters (`0` to `10`) tracking daily usage.
  - `request_id` and `status`: Random request UUIDs used strictly to block accidental duplicate charges and roll back failed requests.

### C. Active AI Prompts & Generated Content (Not Stored as History)
- **Transient processing only:** When you submit a game name, topic, language choice, or active text for translation, the backend forwards that prompt server-side to the Google Gemini API (`gemini-3.8-flash` free tier) and streams/returns the generated text directly to your device.
- **Zero history persistence:** SN Creator **does not** save a history of your prompts or generated gaming titles, hashtags, voice-over scripts, thumbnail prompts, or video ideas in Cloudflare D1, Firebase, SharedPreferences, DataStore, or local device files. Once you leave the active session or sign out, the generated text is cleared from app memory.

### D. Local App Preferences
- **Output language preference:** Your selected default generated-content language (`English` or `Roman Urdu`) is saved locally on your device using Android Jetpack DataStore (`sn_creator_settings`).

### E. Minimal Security & Infrastructure Logs
- SN Creator application code does **not** log Gemini API keys, Firebase bearer tokens, or full user prompt bodies.
- Underlying infrastructure providers (Google Firebase Authentication, Cloudflare Workers, and Google Gemini API) process standard network metadata (such as IP addresses, TLS handshakes, HTTP status codes, and rate-limit events) under their respective operational policies to maintain service security and prevent abuse.
- **Free-Tier AI Notice:** Because SN Creator operates under a strict zero-paid-billing model using the Google AI Studio / Gemini API free tier, please consult Google's [Gemini API Additional Terms of Service](https://ai.google.dev/gemini-api/terms) regarding how Google handles free-tier API traffic.

## 3. Information We Do Not Collect
SN Creator does **not** request or collect:
- Precise or coarse GPS device location
- Contacts, call logs, SMS, camera, or microphone audio
- Advertising identifiers (AAID) or third-party ad-tracking SDKs
- Behavioral analytics telemetry

## 4. External Support Links (WhatsApp)
When you tap **Get Help on WhatsApp** (or **Contact SN Creator** once configured), Android opens WhatsApp or your browser externally (`https://wa.me/...`). Any phone number, profile information, or messages you choose to send inside WhatsApp are governed by WhatsApp's Privacy Policy.

## 5. Your Controls
- **Copy selectively:** Copy only the results you wish to keep using Android's system clipboard.
- **Sign out anytime:** Tap **Sign Out** in the **Settings** tab to clear your active Firebase session and in-memory state on your device.
- **Support:** Reach out via the **Get Help** tab in the app for questions regarding this policy.

*Note: This Privacy Policy accurately describes the technical behavior of the SN Creator codebase and is provided for transparency; it does not constitute formal legal advice or third-party compliance certification.*
