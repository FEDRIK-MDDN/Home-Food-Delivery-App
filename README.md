# HomeChef Connect 🍲

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2024%20--%2036-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12.01-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**A Modern Multi-Sided Marketplace App Connecting Home Chefs, Food Lovers, Delivery Partners, and Administrators.**

[Features](#-key-features-by-role) • [Tech Stack](#-technology-stack) • [Architecture](#-architecture) • [Getting Started](#-getting-started) • [Demo Credentials](#-demo-accounts--credentials) • [Project Structure](#-project-structure)

</div>

---

## 📖 Overview

**HomeChef Connect** is a food delivery application designed to empower local home chefs and provide food lovers with fresh, authentic, homemade meals. 

Unlike conventional fast-food delivery services, HomeChef Connect is built around a **4-Role Ecosystem**:
- **Customers** discover authentic culinary dishes, apply kitchen deals, track orders in real-time, and get smart recommendations from an AI meal advisor.
- **Home Cooks** run their micro-kitchen, manage dish menus with photo uploads, launch custom promo codes, and progress orders through kitchen prep stages.
- **Delivery Partners** claim ready-to-pickup orders, follow simulated interactive delivery routes on custom maps, and log field delivery issues.
- **Platform Administrators** oversee operations, approve newly registered cooks, manage users, and inspect real-time platform revenue.

---

## ✨ Key Features by Role

### 🛒 1. Customer Experience
- **Culinary Discovery & Categories**: Explore authentic homemade dishes across 8 curated categories (*Offers, Asian, Biryani, Indian, Gotyou, Box, Burger, All In*).
- **Nutritional & Dietary Transparency**: Detailed item views including prep time, calories, ingredients, allergens, and cook ratings.
- **Real-Time Cart & Coupon Engine**: Live subtotal calculations, dynamic 5% tax, $2.00 delivery fee, and promo code discounts with minimum order validations.
- **Secure Multi-Payment Checkout**: Delivery instructions for cooks, schedule options, and simulated card checkout.
- **8-Stage Order Progress Tracker**: Visual stepper tracking orders from *Pending* &rarr; *Accepted* &rarr; *Preparing* &rarr; *Ready* &rarr; *Picked Up* &rarr; *Out for Delivery* &rarr; *Delivered* &rarr; *Completed*.
- **Chef AI Assistant**: Smart meal recommendation chat engine answering natural queries based on budget (e.g. *"Meals under $10"*), dietary preferences, or cravings.

### 👨‍🍳 2. Home Cook / Kitchen Portal
- **Admin Verification Gate**: Protects customer food safety by gating incoming orders until an administrator approves the kitchen (`isApprovedCook`).
- **Kitchen Orders Queue**: Real-time incoming orders filtered strictly by `cookId` with one-tap status updates (*Accept Order* &rarr; *Start Cooking* &rarr; *Mark Ready for Pickup*).
- **Dish Menu Management (CRUD)**: Add dishes, edit descriptions/pricing, toggle availability switches in real time, and upload photos directly from the gallery (compressed to optimized Base64).
- **Kitchen Deals & Promos (CRUD)**: Create custom coupon vouchers with percentage or fixed discounts, minimum cart values, usage caps, and validity windows.
- **Earnings Metrics**: Live statistics row displaying today's gross revenue, total kitchen orders, and active menu items.

### 🛵 3. Delivery Express (Driver)
- **Available Pickups Filter**: Displays orders that are strictly in `READY` status with no assigned driver.
- **One-Tap Trip Claiming**: Driver claims the delivery, locking the order to their driver ID and name.
- **Animated Route Simulation**: Custom Jetpack Compose Canvas delivery map showing simulated transit progression (0% to 100%) between the cook's kitchen and customer location.
- **Delivery Issues Log (CRUD)**: Drivers can log field delivery bottlenecks with severity levels (*Low, Medium, High, Urgent*) and categories (*Customer not answering, Wrong address, Vehicle breakdown, Spilled package*).
- **Trip Cancellation**: Ability to safely release an unserviceable order back into the available pool.

### 🛡️ 4. Administrator Control Portal
- **Platform Analytics**: Aggregate gross platform GMV/revenue, registered cook/customer/driver metrics.
- **Cook Approvals**: Review and approve newly registered home cooks with a single tap.
- **User Governance**: Create users directly, update roles, toggle account suspensions, trigger Firebase password reset emails, or delete accounts.

---

## 🛠️ Technology Stack

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.2.10 | Target JVM 11, modern coroutines & flows |
| **UI Toolkit** | Jetpack Compose (BOM 2024.12.01) | Material 3, Material Icons Extended, Custom Theming |
| **Architecture** | MVVM + Unidirectional Data Flow | Reactive `StateFlow` observables and coroutine dispatchers |
| **Cloud Database** | Firebase Cloud Firestore | Real-time listeners for instant multi-device state synchronization |
| **Cloud Auth** | Firebase Authentication | Email/Password, Android Credential Manager, Google Sign-In |
| **Local Database** | Android Room Database 2.7.0 | SQLite caching, offline fallback, SHA-256 credential hashing |
| **Image Loading** | Coil Compose 2.7.0 | Supports Base64 data URLs, content URIs, and remote HTTPS assets |
| **Network & Parsing** | Retrofit 2.12.0 + Moshi + OkHttp 4 | Modern REST client and JSON serialization |
| **Build & Tooling** | Gradle 8.13.0 + KSP 2.3.5 | Gradle Version Catalog (`libs.versions.toml`) |

---

## 📐 Architecture

HomeChef Connect follows Google's recommended Android architecture with a clean separation of concerns:

```
┌────────────────────────────────────────────────────────┐
│               Jetpack Compose UI Layer                 │
│  (AuthScreen, CustomerHome, CookDashboard, Admin, etc.)│
└──────────────────────────▲─────────────────────────────┘
                           │ UI Events & StateFlow Observers
┌──────────────────────────┴─────────────────────────────┐
│                   HomeChefViewModel                    │
│      (StateFlow: currentUser, foods, orders, promos)   │
└──────────────────────────▲─────────────────────────────┘
                           │ Coroutines Dispatch
┌──────────────────────────┴─────────────────────────────┐
│                  HomeChefRepository                    │
│       (Unified Data Orchestrator & Live Listeners)     │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
    ┌──────────▼──────────┐    ┌──────────▼──────────┐
    │  Firebase Services  │    │   Local Database    │
    │  - Cloud Firestore  │    │   - Room SQLite     │
    │  - Firebase Auth    │    │   - DataStore Prefs │
    └─────────────────────┘    └─────────────────────┘
```

---

## 📁 Project Structure

```
Home-Food-Delivery-App/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt               # Root Activity & role router
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   └── AppDatabase.kt        # Room DB, DAOs, UserEntity
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Food.kt               # Dish specification model
│   │   │   │   │   │   ├── Order.kt              # Order tracking schema
│   │   │   │   │   │   ├── OrderStatus.kt        # 8-stage order lifecycle enum
│   │   │   │   │   │   ├── PromoCode.kt          # Kitchen promo voucher schema
│   │   │   │   │   │   ├── DeliveryIssue.kt      # Delivery problem reporting model
│   │   │   │   │   │   └── User.kt / UserRole.kt # Domain user & 4 roles
│   │   │   │   │   └── repository/
│   │   │   │   │       └── HomeChefRepository.kt # Firestore listeners & Cloud CRUD
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/           # Reusable UI cards, headers, dialogs
│   │   │   │   │   ├── screens/              # 16 Compose role screens
│   │   │   │   │   ├── theme/                # Culinary green design theme
│   │   │   │   │   └── viewmodel/
│   │   │   │   │       └── HomeChefViewModel.kt  # StateFlow coordinator
│   │   │   │   └── util/
│   │   │   │       ├── GoogleAuthHelper.kt   # Android Credential Manager & Fallback
│   │   │   │       └── ImageUtils.kt         # Base64 image compression & URI resolver
│   │   │   └── res/                          # Drawables, mipmaps, strings
│   │   └── test/                             # Unit tests & Roborazzi screenshot tests
│   ├── build.gradle.kts                      # Module dependencies & build config
│   └── google-services.json                  # Firebase app configuration
├── gradle/
│   └── libs.versions.toml                    # Version Catalog
├── build.gradle.kts                          # Root build script
├── settings.gradle.kts                       # Settings and repositories
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **Java Development Kit (JDK)**: JDK 17 or JDK 21
- **Android SDK**: API Level 36 (target/compile SDK) with minimum API Level 24 (Android 7.0)

### 1. Clone the Repository
```bash
git clone https://github.com/FEDRIK-MDDN/Home-Food-Delivery-App.git
cd Home-Food-Delivery-App
```

### 2. Configure Firebase
1. The project is pre-configured with Firebase in `app/google-services.json`.
2. Ensure **Authentication** (Email/Password & Google Sign-In) and **Cloud Firestore** are active in your Firebase Console.
3. If using Google Sign-In with your own debug keystore, add your machine's **SHA-1 fingerprint** to your Firebase project's Android app settings.

### 3. Setup Environment Variables
Create a `.env` file in the project root (or copy `.env.example`):
```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
```

### 4. Build and Run
1. Open the project in **Android Studio**.
2. Let Gradle sync and resolve all dependencies from `libs.versions.toml`.
3. Select an emulator or physical device running Android 7.0+ (API 24+).
4. Click **Run** (`Shift + F10`) or run from terminal:
```bash
# Debug compile check
./gradlew compileDebugKotlin

# Run on connected device
./gradlew installDebug
```

---

## 🔑 Demo Accounts & Credentials

The app automatically seeds a default administrative account on first startup:

| Role | Email | Password | Access / Scope |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin@homechef.com` | `Admin@1234` | Full access to Admin Portal, cook approvals, user management |
| **Customer** | *(Self-register via app)* | *(Min 6 chars)* | Full customer ordering & tracking portal |
| **Home Cook** | *(Self-register via app)* | *(Min 6 chars)* | Accesses Kitchen Dashboard *(Requires Admin approval)* |
| **Delivery Driver**| *(Self-register via app)* | *(Min 6 chars)* | Accesses Trip Dispatch & Map Tracking |

> 💡 **Tip:** You can also tap **"Continue with Google"** on the login screen to sign in instantly using any Google Account.

---

## 🧪 Testing

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run dry-run build validation
./gradlew compileDebugKotlin --dry-run
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE). Contributions and pull requests are welcome!
