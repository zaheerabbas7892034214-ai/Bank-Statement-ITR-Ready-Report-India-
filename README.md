# Bank Statement → ITR Ready Report (India)

A complete, production-ready Android application for parsing bank statement PDFs and generating ITR-ready financial reports for the Indian market.

## 📱 App Overview

**Package Name:** `com.yourcompany.itrstatement`  
**Min SDK:** 24 (Android 7.0)  
**Target SDK:** 34 (Android 14)  
**Language:** Kotlin  
**Architecture:** MVVM with Jetpack Compose

### Key Features

#### ✨ Core Features (Free)
- 📄 Import bank statement PDFs via Storage Access Framework (no permissions required)
- 🔍 Intelligent text extraction with OCR fallback (ML Kit)
- 🏷️ Automatic transaction categorization (10 categories)
- 📊 Interactive dashboard with financial summaries
- 📈 Category breakdown with pie charts
- 📉 Monthly trend analysis with bar charts
- 🔎 Search and filter transactions
- 💾 Export to CSV format
- 📱 Last 5 import history

#### 💎 Pro Features (₹699/year)
- ♾️ Unlimited import history
- 📊 Export to Excel (XLSX) with multiple sheets
- 📋 Export to JSON with metadata
- 📄 Export to professional PDF reports
- 🔀 Multi-file merge with duplicate removal
- 🎯 Priority support

## 🛠️ Tech Stack

### Core Technologies
- **UI Framework:** Jetpack Compose with Material 3
- **Language:** Kotlin 1.9.20
- **Architecture:** MVVM (ViewModel + StateFlow)
- **Database:** Room 2.6.1
- **Build System:** Gradle with Version Catalog

### Key Libraries
- **Billing:** Google Play Billing Library 6.1.0
- **PDF Processing:** PDFBox Android 2.0.27.0
- **OCR:** ML Kit Text Recognition 16.0.0
- **Excel Export:** Apache POI 5.2.5
- **JSON:** Gson 2.10.1
- **Coroutines:** Kotlin Coroutines 1.7.3
- **Navigation:** Navigation Compose 2.7.5
- **Image Loading:** Coil 2.5.0
- **Background Processing:** WorkManager 2.9.0

## 📁 Project Structure

```
app/src/main/java/com/yourcompany/itrstatement/
├── billing/
│   ├── BillingManager.kt          # Google Play Billing integration
│   └── EntitlementManager.kt      # Pro subscription management
├── data/
│   ├── local/
│   │   ├── entities/              # Room entities
│   │   ├── dao/                   # Room DAOs
│   │   └── AppDatabase.kt         # Room database
│   ├── model/                     # Domain models
│   └── repository/                # Data repositories
├── domain/
│   ├── parser/                    # PDF parsing and OCR
│   ├── categorizer/               # Transaction categorization
│   ├── exporter/                  # Export functionality
│   └── usecase/                   # Business logic
├── ui/
│   ├── components/                # Reusable UI components
│   ├── screens/                   # App screens (10 screens)
│   ├── viewmodel/                 # ViewModels (6 ViewModels)
│   ├── navigation/                # Navigation graph
│   └── theme/                     # Material 3 theme
├── utils/                         # Utility classes
├── MainActivity.kt
└── ITRApplication.kt
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Building the Project

1. **Clone the repository:**
```bash
git clone https://github.com/zaheerabbas7892034214-ai/Bank-Statement-ITR-Ready-Report-India-.git
cd Bank-Statement-ITR-Ready-Report-India-
```

2. **Open in Android Studio:**
- File → Open → Select project folder
- Wait for Gradle sync to complete

3. **Build the app:**
```bash
./gradlew assembleDebug
```

4. **Run on emulator/device:**
- Click "Run" in Android Studio, or:
```bash
./gradlew installDebug
```

## 📦 Google Play Console Setup

### 1. Create Subscription Product

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app (or create a new one)
3. Navigate to **Monetize → Products → Subscriptions**
4. Click **Create subscription**
5. Fill in the details:
   - **Product ID:** `itr_pro_yearly`
   - **Name:** ITR Pro - Yearly Subscription
   - **Description:** Unlock unlimited imports, all export formats, and advanced features

### 2. Add Base Plan

1. In the subscription product, click **Add base plan**
2. Configure:
   - **Base plan ID:** `yearly_base`
   - **Billing period:** 1 year (P1Y)
   - **Price:** ₹699 (or your preferred pricing)
   - **Renewal type:** Auto-renewing
3. Click **Save** and then **Activate**

### 3. Set Up License Testing

1. Go to **Settings → License testing**
2. Add test Gmail accounts:
   - Click **Add license testers**
   - Enter Gmail addresses (one per line)
   - Click **Save**
3. Test accounts can purchase without being charged

### 4. Create Internal Testing Track

1. Navigate to **Testing → Internal testing**
2. Click **Create new release**
3. Upload your signed AAB (see next section)
4. Add release notes
5. Click **Review release** → **Start rollout to internal testing**
6. Go to **Testers** tab
7. Add email list of internal testers
8. Copy the **Opt-in URL** and share with testers

## 🔐 Building Signed AAB

### 1. Generate Upload Keystore

```bash
keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

**Important:** Store the keystore and passwords securely! You'll need them for all future releases.

### 2. Configure Signing in `app/build.gradle.kts`

Create `keystore.properties` in the project root:
```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=upload
storeFile=../upload-keystore.jks
```

Add to `.gitignore`:
```
keystore.properties
upload-keystore.jks
```

Update `app/build.gradle.kts`:
```kotlin
android {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }
    
    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = keystoreProperties["storeFile"]?.let { file(it) }
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... existing config
        }
    }
}
```

### 3. Build Release AAB

```bash
./gradlew bundleRelease
```

The AAB will be at: `app/build/outputs/bundle/release/app-release.aab`

## 🧪 Testing Subscription Flow

### 1. Install via Internal Testing

1. Open the opt-in URL from Play Console on your test device
2. Accept the invitation to become a tester
3. Click **Download it on Google Play**
4. Install the app from Play Store

### 2. Test Purchase Flow

1. Open the app
2. Navigate to **Paywall** screen or tap **Upgrade to Pro**
3. Verify product details are displayed (₹699/year)
4. Tap **Subscribe Now**
5. Google Play purchase sheet should appear
6. Complete purchase with test account (no charge)
7. Verify Pro features are unlocked immediately

### 3. Test Restore Purchases

1. Uninstall and reinstall the app
2. Go to **Settings** → **Restore Purchases**
3. Verify subscription is restored and Pro status is active

### 4. Test Subscription Expiry

Test accounts have shorter expiry times:
- Check subscription status in Play Console
- Verify app handles expired subscriptions correctly
- Test re-subscription flow

## 🏗️ Architecture

### MVVM Pattern

```
View (Compose) → ViewModel → UseCase → Repository → Data Source
                     ↓
                 StateFlow
```

### Data Flow

1. **User Action** → Compose Screen
2. **Screen** → ViewModel (via events)
3. **ViewModel** → UseCase (business logic)
4. **UseCase** → Repository (data operations)
5. **Repository** → Room DAO / Network / File System
6. **Response** → StateFlow → Screen (reactive updates)

### Key Patterns

- ✅ Single source of truth (Room database)
- ✅ Unidirectional data flow
- ✅ Reactive UI with StateFlow
- ✅ Sealed classes for states
- ✅ Result wrapper for error handling
- ✅ Coroutines for async operations
- ✅ Dependency injection (manual)

## 📊 Database Schema

### TransactionEntity
```kotlin
id: Long (PK)
importSessionId: Long (FK)
date: Long (timestamp)
description: String
category: String
debitAmount: Double
creditAmount: Double
balance: Double
timestamp: Long
```

### ImportHistoryEntity
```kotlin
id: Long (PK)
fileName: String
importTimestamp: Long
transactionCount: Int
fileUri: String
parsingStatus: String
```

### EntitlementEntity
```kotlin
id: Int (PK, always 1)
isProActive: Boolean
subscriptionExpiryTimestamp: Long
purchaseToken: String
lastCheckedTimestamp: Long
```

## 🎨 UI Screens

1. **SplashScreen** - App launch with logo
2. **HomeScreen** - Import history and main navigation
3. **ImportScreen** - PDF import with progress tracking
4. **DashboardScreen** - Financial summaries and charts
5. **TransactionListScreen** - Searchable transaction list
6. **CategoryBreakdownScreen** - Pie chart and category details
7. **MonthlyChartsScreen** - Monthly trends analysis
8. **ExportScreen** - Multi-format export options
9. **PaywallScreen** - Subscription purchase flow
10. **SettingsScreen** - App settings and subscription status

## 🔒 Security & Privacy

- ✅ No storage permissions required (uses SAF)
- ✅ All data stored locally in encrypted Room database
- ✅ No data transmitted to external servers
- ✅ Secure purchase verification via Google Play
- ✅ ProGuard rules for release builds
- ✅ Purchase tokens securely stored

## 🐛 Troubleshooting

### Build Issues

**Problem:** Gradle sync fails
```bash
# Solution: Clear cache and rebuild
./gradlew clean
./gradlew build --refresh-dependencies
```

**Problem:** Dependency resolution fails
```bash
# Solution: Check gradle/libs.versions.toml versions
# Ensure all version references are correct
```

### Billing Issues

**Problem:** "Product not found" error
- Ensure subscription is **Active** in Play Console
- App must be signed with release key
- Test account must be added as license tester
- App must be installed from Play Store (not directly)

**Problem:** Purchase not acknowledged
- Check BillingManager logs
- Verify acknowledgePurchase is called
- Check network connectivity

### Runtime Issues

**Problem:** PDF parsing fails
- Check PDFBox initialization in ITRApplication
- Verify PDF file is valid and not corrupted
- Check file permissions (SAF should handle this)

**Problem:** Room database errors
- Check migration strategy if schema changes
- Clear app data to reset database
- Verify entity annotations

## 📝 License

This project is proprietary software. All rights reserved.

## 👥 Contributors

- Development Team: [Your Company]
- UI/UX Design: [Designer Name]
- QA Testing: [Tester Name]

## 📞 Support

For support, please contact:
- Email: support@yourcompany.com
- Website: https://yourcompany.com/support

## 🔄 Version History

### Version 1.0.0 (Current)
- ✅ Initial release
- ✅ Complete MVVM architecture
- ✅ Google Play Billing integration
- ✅ PDF parsing and OCR
- ✅ 10 categories with smart categorization
- ✅ Multi-format exports (CSV, XLSX, JSON, PDF)
- ✅ Material 3 UI with dark mode
- ✅ Pro subscription with yearly plan

## 🚧 Roadmap

### Planned Features
- [ ] Multiple bank support with templates
- [ ] Cloud backup (optional)
- [ ] AI-powered categorization improvements
- [ ] Custom category creation
- [ ] Transaction splitting
- [ ] Budget tracking
- [ ] Tax calculation assistance
- [ ] Monthly reports via email

---

**Made with ❤️ for Indian taxpayers**
