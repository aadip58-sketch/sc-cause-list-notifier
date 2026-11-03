# Supreme Court Cause List Notifier (Android App)

An Android application that monitors the Supreme Court of India cause list portal in the background and immediately sends a push notification whenever a new cause list drops.

## 🌟 Key Features
- **Instant Drop Notifications**: Runs periodic background checks (every 15 minutes via Android `WorkManager`). The moment the Supreme Court registry uploads the next day's Main, Supplementary, or Advance list, your phone alerts you with sound and vibration.
- **One-Tap PDF Viewing**: Directly tap the notification action button ("View PDF") or tap any cause list in the app to view the official PDF in your phone's PDF reader or browser.
- **Clean Material 3 UI**: Shows current monitoring status ("Active", "Last Checked: 10:45 AM"), manual refresh button, pull-to-refresh, categorized chips (`MAIN`, `SUPPLEMENTARY`, `ADVANCE`, `REGISTRAR`, `CHAMBER`), and share buttons.
- **Battery & Reboot Resilient**: Uses Android Jetpack `WorkManager` with `BOOT_COMPLETED` receiver to automatically resume monitoring even after device restart, without draining battery.

---

## 📦 How to Get Your `.apk` File

Since your local Mac currently does not have the Android SDK / Java build tools installed, we have included an automated **GitHub Actions CI workflow** that builds and packages the `.apk` for you in the cloud for free in under 2 minutes:

### Method 1: Automated 1-Click Cloud Build (Recommended & Fastest)
1. Initialize git and push this project folder to your GitHub account:
   ```bash
   cd /Users/aadipandey/.gemini/antigravity/scratch/sc-cause-list-notifier
   git init
   git add .
   git commit -m "Initial commit for Supreme Court Cause List Notifier"
   git branch -M main
   git remote add origin https://github.com/<YOUR_USERNAME>/sc-cause-list-notifier.git
   git push -u origin main
   ```
2. Open your repository on GitHub in any browser.
3. Click on the **Actions** tab -> Select **Build Supreme Court Cause List APK** -> Click **Run workflow**.
4. Once completed (~1-2 minutes), scroll down to **Artifacts** to download `SCCauseListNotifier-Debug-APK` (contains `app-debug.apk`), or check the generated **Releases** tab!

### Method 2: Open in Android Studio
1. Open **Android Studio** on your computer.
2. Select **Open** and choose `/Users/aadipandey/.gemini/antigravity/scratch/sc-cause-list-notifier`.
3. Go to the menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Android Studio will generate the APK at:
   `app/build/outputs/apk/debug/app-debug.apk`

### Method 3: Local Command Line (Requires JDK 17)
If you install Java (`brew install openjdk@17`):
```bash
./gradlew assembleDebug
```
The output file is located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📲 Installing on Your Phone
1. Transfer `app-debug.apk` to your Android phone (via WhatsApp, Google Drive, USB, or download directly from GitHub in your phone's browser).
2. Tap the file in your phone's **Files** or **Downloads** app.
3. If prompted, tap **Settings** and allow **Install unknown apps** for that app.
4. Tap **Install**.
5. Open the app and tap **Allow** when prompted for notification permissions (Android 13+).
6. *(Recommended)* If your phone manufacturer has aggressive battery saver (Xiaomi/Samsung/OnePlus), disable battery optimization for this app in phone settings so background checks run reliably without OS sleep interruption.

---

## 📂 Project Architecture
```
sc-cause-list-notifier/
├── .github/workflows/
│   └── build-apk.yml            # Automated GitHub Actions APK builder
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml  # Permissions (Internet, Notifications, Boot)
│   │   ├── java/in/gov/sci/causelist/
│   │   │   ├── MainActivity.kt            # Main screen & pull-to-refresh
│   │   │   ├── model/CauseListItem.kt     # Data model
│   │   │   ├── parser/CauseListParser.kt  # Scrapes sci.gov.in & detects new lists
│   │   │   ├── service/
│   │   │   │   ├── CauseListWorker.kt     # Background periodic worker
│   │   │   │   └── BootReceiver.kt        # Auto-starts worker on reboot
│   │   │   ├── ui/CauseListAdapter.kt     # RecyclerView list adapter
│   │   │   └── util/
│   │   │       ├── NotificationHelper.kt  # Notification builder & channel
│   │   │       └── PreferencesManager.kt  # Remembers seen cause lists
│   │   └── res/                           # Layouts, vector drawables, themes
│   └── build.gradle.kts
├── settings.gradle.kts
└── build.gradle.kts
```
