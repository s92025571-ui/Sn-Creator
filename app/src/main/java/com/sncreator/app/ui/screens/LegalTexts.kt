package com.sncreator.app.ui.screens

object LegalTexts {
    const val PRIVACY_POLICY_TEXT = """
SN Creator Privacy Policy
Effective Date: October 25, 2026

1. Overview
SN Creator ("the App") provides AI-assisted content creation tools for gaming creators. We design the App around strict data minimization.

2. Information We Process
- Verified Account Identity: When you sign in with Google via Firebase Authentication, we verify your Firebase authentication token to identify your account (UID) and display your Google profile name and email inside the App on your device.
- Daily Quota Records: Our backend stores your verified user UID, the current Pakistan calendar date (Asia/Karachi), and your daily successful generation count (up to 10 per day) alongside short-lived request IDs used solely to prevent duplicate charges.
- Minimal Operational & Security Logs: Basic HTTP status, error codes, and rate-limit events may be processed transiently by infrastructure providers (Firebase Authentication, Cloudflare Workers, and Google Gemini API) to keep the service reliable and prevent abuse. No API keys, bearer tokens, or full prompt contents are logged by SN Creator application code.

3. No Content History Storage
SN Creator does NOT save a history of your prompts or AI-generated titles, hashtags, scripts, thumbnail prompts, or video ideas in our database, cloud storage, or local device history files. Active results remain only in temporary screen memory while you view them.

4. Third-Party Services
- Google / Firebase Authentication: Authenticates your Google sign-in session.
- Google Gemini API (Free Tier): Processes your gaming topic, selected game, and tool instructions to generate text responses. Review Google's Gemini API Terms regarding how free-tier API requests are handled by Google.
- Cloudflare Workers & D1: Hosts the authenticated API gateway and daily quota counter.
- WhatsApp: When you tap "Get Help on WhatsApp" or "Contact SN Creator", your device opens WhatsApp externally; messages sent there are governed by WhatsApp's privacy policy.

5. What We Do Not Collect
We do not collect GPS location data, contacts, microphone recordings, advertising identifiers, or behavioral analytics profiles.

6. Account Sign-Out & Support
You can sign out at any time from Settings. For privacy questions, contact the SN Creator team via the support channel in the Get Help tab.
"""

    const val TERMS_OF_USE_TEXT = """
SN Creator Terms of Use
Effective Date: October 25, 2026

1. Acceptance of Terms
By signing in to and using SN Creator, you agree to these Terms of Use. If you do not agree, please discontinue use of the App.

2. Account & Access
- A valid Google account authenticated through Firebase Authentication is required to use SN Creator. Guest access is not offered.
- You are responsible for keeping your Google account secure and for not attempting to bypass authentication or daily usage quotas.

3. Daily Usage Quota & Service Availability
- Each authenticated user receives up to 10 successful AI generations per calendar day in the Asia/Karachi timezone.
- Initial generations, regenerations, and AI-powered language translations each count as 1 generation. Failed or timed-out requests do not count against your quota.
- SN Creator relies on free-tier AI and cloud infrastructure. Availability, speed, and upstream rate limits are provided on an "as-is" and "as-available" basis without uptime guarantees.

4. AI-Generated Output Disclaimer
- Generated gaming titles, hashtags, voice-over scripts, thumbnail text prompts, and video ideas are produced by an automated AI model (Google Gemini).
- SN Creator does NOT guarantee video views, audience growth, platform virality, monetization, or accuracy of game mechanics mentioned by the AI.
- You are responsible for reviewing, editing, and ensuring your final published content complies with YouTube, TikTok, Instagram, game publisher guidelines, and applicable laws.

5. Acceptable Use
You agree not to submit unlawful, abusive, hateful, or harmful prompts, and not to probe, overload, scrape, or reverse-engineer the SN Creator backend API.

6. Limitation of Liability
To the maximum extent permitted by applicable law, SN Creator and its creators are provided without warranties of any kind and shall not be liable for indirect, incidental, or consequential damages arising from use of the App or reliance on AI-generated text.
"""
}
