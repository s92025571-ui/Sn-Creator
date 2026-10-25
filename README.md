# SN Creator — AI Gaming Content Studio for Android

**SN Creator** (`com.sncreator.app`) is a native **Kotlin + Jetpack Compose (Material 3)** Android application paired with a **zero-paid-billing Cloudflare Workers + D1 + Google Gemini free-tier backend** and an automated **GitHub Actions debug APK build pipeline**.

---

## 1. Architecture & Zero-Billing Provider Evaluation

Every component in SN Creator was verified against the **strict zero-paid-billing requirement** (no credit card, no Blaze upgrade, no paid API fallback):

| Layer | Selected Technology | Free-Tier Verification & Limits |
| :--- | :--- | :--- |
| **Android App** | Kotlin `2.0.21`, Jetpack Compose BOM `2024.12.01` (Material 3), MVVM + Repositories, Credential Manager `1.3.0` | **Min SDK 26 (Android 8.0)**, Target/Compile SDK **35**. Min SDK 26 covers 97%+ of active Android phones, provides native `java.time` (`Asia/Karachi`), adaptive icons, and modern Credential Manager Google Sign-In. |
| **Authentication** | **Firebase Authentication** (Google Sign-In on **Spark No-Cost Plan**) | Free up to 50,000 MAUs for standard providers with **no billing account required**. *(Note: Firebase Cloud Functions were deliberately rejected because deploying Functions requires upgrading Firebase to the paid Blaze plan.)* |
| **Trusted Backend** | **Cloudflare Workers (Free) + Cloudflare D1 SQLite (Free)** (`backend/`) | **No credit card required** when enabling strictly Workers Free + D1 Free (do **not** enable R2 or paid add-ons). Includes 100,000 requests/day, 5M D1 rows read/day, 100k D1 rows written/day, and 5 GB SQLite storage. Verifies RS256 Firebase ID tokens via Google's official SecureToken JWKS. |
| **AI Model** | **Google Gemini API Free Tier** (`gemini-3.8-flash` via `@google/genai`) | Free in Google AI Studio on unbilled projects ($0 input/output tokens; rate-limited instead of billed). Paid-only models are explicitly blocked in `backend/src/gemini.ts`. |
| **CI / APK Build** | **GitHub Actions** (`.github/workflows/android-apk.yml`) | Runs on standard `ubuntu-latest` runners, validates Gradle `8.10.2` wrapper, executes unit tests, compiles `:app:assembleDebug`, and uploads `app-debug.apk` as a downloadable workflow artifact. |

---

## 2. Project Structure

```text
├── .github/workflows/
│   └── android-apk.yml              # GitHub Actions CI: JDK 17 + Android SDK 35 + Tests + APK Artifact
├── app/                             # Native Android Application (com.sncreator.app)
│   ├── build.gradle.kts             # Kotlin 2.0 Compose, Credential Manager, Firebase Auth, OkHttp
│   ├── google-services.json.example # Template (real google-services.json is git-ignored)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/sncreator/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/            # AuthRepository, ContentGenerationRepository, Preferences, Connectivity
│       │   │   ├── model/           # SupportedGame, AiToolType, OutputLanguage, VoiceOverDuration, Validators
│       │   │   ├── ui/              # SnCreatorApp, 3D SN Logo, Glass Cards, Theme & All 7 Screens
│       │   │   └── util/            # PakistanTime (Asia/Karachi), SupportLinks (WhatsApp & Placeholder)
│       │   └── res/                 # Vector adaptive launcher icons, themes, strings, network security XML
│       └── test/java/com/sncreator/app/
│           └── SnCreatorUnitTest.kt # Executable JUnit4 + Coroutines + MockWebServer test suite
├── backend/                         # Standalone Zero-Billing Cloudflare Worker + D1 Backend
│   ├── schema.sql                   # Atomic daily_quotas & request_reservations D1 SQLite schema
│   ├── wrangler.toml.example        # Secret-free Wrangler config template
│   ├── src/
│   │   ├── auth.ts                  # Cryptographic RS256 Firebase ID token verification (JWKS)
│   │   ├── gemini.ts                # Server-side @google/genai free-tier client (25s timeout, zero paid fallback)
│   │   ├── index.ts                 # Authenticated /v1/quota, /v1/generate, /v1/translate router
│   │   ├── prompts.ts               # All 5 gaming AI tools, English/Roman Urdu, 6 voice-over durations
│   │   ├── quota.ts                 # Atomic Asia/Karachi 10/day reservation, duplicate guard & rollback
│   │   └── validation.ts            # Input bounds, 16KB body cap, requestId regex validation
│   └── test/
│       └── backend.test.ts          # Executable Vitest suite backed by real SQLite
├── gradle/                          # Gradle 8.10.2 wrapper & libs.versions.toml catalog
├── gradlew & gradlew.bat
├── PRIVACY_POLICY.md
├── TERMS_OF_USE.md
└── README.md
```

---

## 3. Syncing to GitHub & Downloading the Debug APK via GitHub Actions

### Step 1: Export / Sync from Google AI Studio to GitHub
1. In **Google AI Studio**, click the **GitHub / Export** icon in the top toolbar (or download the project archive and initialize a Git repository).
2. Create a new GitHub repository (private or public — **no secrets are committed** in this codebase) with default branch named `main`.
3. Push the project files to the `main` branch:
   ```bash
   git init
   git branch -M main
   git add .
   git commit -m "Initial commit: SN Creator Android app, Cloudflare Worker backend, and APK CI"
   git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>.git
   git push -u origin main
   ```

### Step 2: Run the GitHub Actions Workflow
1. Open your repository on GitHub and click the **Actions** tab.
2. The **Android Debug APK CI** workflow (`.github/workflows/android-apk.yml`) triggers automatically on every push to `main`.
3. You can also trigger it manually anytime: select **Android Debug APK CI** in the left sidebar -> click **Run workflow** -> select branch `main` -> click **Run workflow**.

### Step 3: Download & Install `app-debug.apk` on Your Android Phone
1. Once the workflow run turns green, click into the completed workflow run.
2. Scroll down to the **Artifacts** section at the bottom of the summary page.
3. Click **`SN-Creator-debug-apk`** to download the `.zip` archive, and extract **`app-debug.apk`** (produced from `app/build/outputs/apk/debug/app-debug.apk`).
4. Transfer `app-debug.apk` to an Android phone running **Android 8.0 (API 26) or newer**, open the file, allow installation from your file manager/browser if prompted, and launch **SN Creator**.

*(Tip: Once you configure Firebase and your Cloudflare Worker URL in Sections 4 & 5 below, add `GOOGLE_SERVICES_JSON`, `FIREBASE_WEB_CLIENT_ID`, and `SN_BACKEND_BASE_URL` under **GitHub Repo -> Settings -> Secrets and variables -> Actions** and re-run the workflow so your CI-built APK has live Google Sign-In and backend URL baked in without committing secrets to Git!)*

---

## 4. Configuring Firebase Google Sign-In (No Billing / Spark Plan)

A Spark-plan Firebase project (`enduring-agent-dt8c4`) is already provisioned for AI Studio, or you can use any free Firebase Spark project:

1. Open the [Firebase Console](https://console.firebase.google.com/) and select your project (**Spark free plan — do not enable billing**).
2. Go to **Build -> Authentication -> Sign-in method**, select **Google**, toggle **Enable**, choose a support email, and click **Save**.
3. Copy the **Web client ID** shown under Google Sign-In -> *Web SDK configuration* (looks like `1234567890-xxxx.apps.googleusercontent.com`). This is your `FIREBASE_WEB_CLIENT_ID`.
4. Go to **Project Settings (gear icon) -> General -> Your apps -> Add app -> Android**:
   - **Android package name:** `com.sncreator.app`
   - **App nickname:** `SN Creator`
   - **Debug signing certificate SHA-1:** Get the SHA-1 of the debug keystore that builds your APK:
     - **Local machine:**
       ```bash
       keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
       ```
     - *(Note on GitHub Actions debug builds: GitHub's runner generates an ephemeral debug keystore unless you either build locally or provide a consistent debug keystore; if you download the APK built on GitHub Actions, inspect its signing SHA-1 with `apksigner verify --print-certs app-debug.apk` and add that SHA-1 fingerprint to your Firebase Android app settings!)*
5. Click **Register app** and download **`google-services.json`**.
6. For **local builds**: place `google-services.json` at `app/google-services.json` (already protected by `.gitignore`).
7. For **GitHub Actions builds**: go to **GitHub Repository -> Settings -> Secrets and variables -> Actions -> New repository secret**:
   - Name: `GOOGLE_SERVICES_JSON` -> Paste the entire JSON contents of `google-services.json`.
   - Name: `FIREBASE_WEB_CLIENT_ID` -> Paste your Web client ID (`...apps.googleusercontent.com`).

---

## 5. Deploying the Zero-Billing Backend (Cloudflare Workers + D1 + Gemini Free Key)

### Step 1: Get a Free Gemini API Key (No Billing Account)
1. Visit [Google AI Studio](https://aistudio.google.com/), sign in with Google, and click **Get API key**.
2. Create an API key in a project **without** a linked Cloud Billing account so it stays strictly on the $0 free tier.

### Step 2: Create Free Cloudflare Worker & D1 Database
1. Sign up or log in at [Cloudflare Dashboard](https://dash.cloudflare.com/) on the **Workers Free** plan (**no credit card required** — do not enable R2 or paid add-ons).
2. In a terminal on your computer (with Node.js 20+ installed):
   ```bash
   cd backend
   npm install
   npx wrangler login
   ```
3. Create the free D1 SQLite database for atomic daily quotas:
   ```bash
   npx wrangler d1 create sn-creator-quota-db
   ```
4. Copy `backend/wrangler.toml.example` to `backend/wrangler.toml` (git-ignored) and paste the returned `database_id` UUID and your `FIREBASE_PROJECT_ID`.
5. Initialize the D1 database tables (`daily_quotas` and `request_reservations`):
   ```bash
   npx wrangler d1 execute sn-creator-quota-db --remote --file=./schema.sql
   ```
6. Store your Gemini API key securely as an encrypted Cloudflare Worker secret (**never** in `wrangler.toml` or Git):
   ```bash
   npx wrangler secret put GEMINI_API_KEY
   ```
7. Deploy the Worker:
   ```bash
   npx wrangler deploy
   ```
   Cloudflare will print your HTTPS endpoint, e.g. `https://sn-creator-api.<your-subdomain>.workers.dev`.

### Step 3: Point the Android App to Your Worker Endpoint
- **Local Gradle build (`local.properties` in repo root, git-ignored):**
  ```properties
  SN_BACKEND_BASE_URL=https://sn-creator-api.<your-subdomain>.workers.dev
  FIREBASE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
  ```
- **GitHub Actions build:** Add `SN_BACKEND_BASE_URL` under **Settings -> Secrets and variables -> Actions -> Secrets (or Variables)** and re-run **Android Debug APK CI**.

*(Optional later configuration: When the official "Contact SN Creator" WhatsApp phone number for the Settings screen is finalized, set `SN_CONTACT_WHATSAPP_NUMBER=923XXXXXXXXX` in `local.properties` or GitHub Actions variables. Until then, it safely remains an explicit placeholder without inventing a phone number, while the Get Help tab opens `https://wa.me/923288501635` with the prefilled support message.)*

---

## 6. Running Tests & Verifying Real AI Generation

### Run Backend Unit Tests (SQLite Quota, Auth, Rollback, PKT Reset, Prompts)
```bash
npm run test:backend
```

### Run Android Unit Tests & Build Debug APK Locally
```bash
./gradlew testDebugUnitTest assembleDebug
```

### End-to-End Real AI Verification Checklist on Android Phone
1. Launch **SN Creator** -> verify the animated 3D **SN** splash logo transitions to the **Welcome to SN Creator** sign-in screen (no guest bypass).
2. Tap **Continue with Google** -> complete Google Sign-In -> verify **Home** greets `"Welcome back, <Your Google Name>"` and shows `"10/10 generations left"`.
3. Open **Create** -> test selecting `PUBG Mobile`, `Free Fire`, `Last Asylum: Plague`, `Gangstar Mirage City`, and `Other Game` (verify custom game name validation).
4. Test each of the **5 AI Tools** (`Gaming Titles`, `Hashtags`, `Voice-over Scripts` across `15s`–`10m` durations, `AI Thumbnail Prompts`, and `Gaming Video Ideas`) in **English** and **Roman Urdu**.
5. On the **Result** screen, verify **Copy Output** copies to the Android clipboard with confirmation, **Regenerate** creates a fresh AI response (`-1` quota), and **To Roman Urdu / To English** translates via AI (`-1` quota).
6. Turn on **Airplane Mode** and tap Generate -> verify `"Internet connection required for AI generation."` appears with a working **Retry** button and zero quota loss.
