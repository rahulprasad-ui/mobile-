# 📱 Rivava TrackFi - Android Application

Native, modern Android financial intelligence and expense management application built using **Kotlin**, **Jetpack Compose**, and **Clean Architecture**.

---

## 🌟 Key Features

- 📊 **Smart Dashboard & Analytics:** Real-time expense breakdown, monthly budgeting, and category insights.
- 📩 **Automatic SMS Expense Tracker:** On-device SMS parsing for banking transactions and credit/debit alerts.
- 💼 **Investment & Portfolio Tracking:** Live market insights powered by Alpha Vantage & Finnhub APIs.
- 🤖 **AI Financial Review:** Automated smart tips and budget optimization suggestions.
- ⭐ **Elite Membership & Advisory:** Integrated payment gateway, booking advisory sessions, and premium financial tools.
- 🔐 **Biometric & Cloud Sync:** Biometric authentication, offline-first Room database, and Firebase Cloud Firestore synchronisation.
- 🎨 **Modern Jetpack Compose UI:** Fluid dark/light theme, custom financial charts, and glassmorphic micro-animations.

---

## 🏛 Tech Stack & Architecture

- **Language:** Kotlin (1.9.22)
- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture + MVVM (Data, Domain, UI layers)
- **Dependency Injection:** Dagger Hilt
- **Local Persistence:** Room Database + DataStore Preferences
- **Network / API:** Retrofit 2 + OkHttp + Moshi / Gson
- **Async & Reactive:** Kotlin Coroutines + StateFlow / SharedFlow
- **Firebase Services:** Firebase Auth, Cloud Firestore, Cloud Messaging (FCM)
- **Target SDK:** 35 (Android 15) | **Min SDK:** 24 (Android 7.0)

---

## 📂 Project Structure

```
mobile/
├── app/
│   ├── src/main/java/com/rivavafi/universal/
│   │   ├── data/             # Repositories, Room DB, DAO, Remote API clients
│   │   ├── di/               # Hilt Dependency Injection Modules
│   │   ├── domain/           # Models, UseCases, Business Logic
│   │   ├── sms/              # SMS Parsing & Banking Detection Engine
│   │   ├── ui/               # Jetpack Compose Screens & ViewModels
│   │   │   ├── add/          # Add Expense / Transaction Screen
│   │   │   ├── aireview/     # AI Review & Insights
│   │   │   ├── analytics/    # Deep Financial Analytics & Charts
│   │   │   ├── auth/         # Phone OTP & Google Login
│   │   │   ├── calculator/   # Financial & Investment Calculators
│   │   │   ├── elite/        # Elite Membership & Advisory Booking
│   │   │   ├── history/      # Transaction History & Filter
│   │   │   ├── home/         # Main Home Dashboard
│   │   │   ├── onboarding/   # App Onboarding flow
│   │   │   ├── portfolio/    # Stock & Crypto Portfolio
│   │   │   ├── profile/      # User Profile & KYC
│   │   │   ├── settings/     # Security, Preferences & Sync
│   │   │   └── theme/        # Color, Typography & Shape Design System
│   │   └── utils/            # Extensions, Date Formatters, Key Validators
│   ├── src/main/res/         # Drawables, Strings, Icons, XML Configs
│   ├── build.gradle.kts      # Application-level Gradle Build Script
│   └── google-services.json  # Firebase Configuration File
├── gradle/                   # Gradle Wrapper Files
├── build.gradle.kts          # Root-level Gradle Build Script
├── settings.gradle.kts       # Repository & Plugin Management
└── local.properties          # Local SDK & Secret API Keys (Git ignored)
```

---

## ⚙️ Setup & Configuration

### 1. Prerequisites
- **Android Studio:** Hedgehog | Iguana | Jellyfish | Koala or newer
- **JDK:** Java 17 or Java 21 (configured in Android Studio Gradle settings)
- **Android SDK:** API 35 installed

### 2. Configure `local.properties`
Create or edit `local.properties` inside the `mobile/` directory:

```properties
sdk.dir=/path/to/your/Android/Sdk

# Market Data API Keys (Optional for live stock data)
alphavantage.apikey=your_alpha_vantage_key
finnhub.apikey=your_finnhub_key
resend.apikey=your_resend_api_key

# Release Signing Keys (Optional for local debug builds)
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

### 3. Firebase Setup
Ensure [`mobile/app/google-services.json`](file:///e:/Rivavatrackfi-app/mobile/app/google-services.json) is present with your Firebase project credentials.

---

## 🚀 Build & Run Commands

From the `mobile/` directory:

### Debug Build
```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows (PowerShell / Command Prompt)
.\gradlew.bat assembleDebug
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Generate Release APK / Bundle
```bash
./gradlew assembleRelease
./gradlew bundleRelease
```
