  ├── settings/
    └── navigation/
        ├── AppNavGraph.kt
        └── NavigationRoutes.kt

---

## 4. Technology Stack & Dependencies

### Core
```kotlin
// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.8.2")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.6")

// Lifecycle & ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Hilt
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
```

### Firebase
```kotlin
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
```

### Cryptography
```kotlin
// Argon2 key derivation
implementation("com.lambdapioneer.argon2kt:argon2kt:1.4.0")

// BouncyCastle for X25519 / Ed25519
implementation("org.bouncycastle:bcprov-jdk18on:1.77")
```

### Storage & Data
```kotlin
// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// EncryptedSharedPreferences
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Room (contacts DB)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
```

### Media & UI
```kotlin
// Coil (image loading)
implementation("io.coil-kt:coil-compose:2.5.0")

// ExoPlayer
implementation("androidx.media3:media3-exoplayer:1.2.1")
implementation("androidx.media3:media3-ui:1.2.1")

// Lottie
implementation("com.airbnb.android:lottie-compose:6.3.0")

// Splash screen
implementation("androidx.core:core-splashscreen:1.0.1")
```

---

## 5. Cryptographic Architecture — Critical Rules

This is a **zero-knowledge** system. These rules are absolute and must never be violated.

### 5.1 Key Hierarchy

```
User Password
     │
     ▼  (Argon2id: iterations=3, memory=65536, parallelism=2, hashLength=32)
Derived Key (256-bit)  ← NEVER stored, exists only in memory during unlock
     │
     ▼  (AES-256-GCM encrypt)
Encrypted Master Key  ← stored in Firestore (ciphertext + IV only)
     │
     ▼  (AES-256-GCM encrypt)
File Encryption Keys  ← stored in VaultMetadata (each file has unique key)
     │
     ▼  (AES-256-GCM streaming)
Encrypted Files       ← stored in local vault folder
```

### 5.2 Asymmetric Keys

```
X25519 Key Pair  → encryption + ECDH key exchange for file sharing
Ed25519 Key Pair → digital signatures for share authenticity

Public keys  → stored PLAINTEXT in Firestore (discoverable by other users)
Private keys → encrypted with Master Key, stored in Firestore
```

### 5.3 Rules That Must Never Be Broken

1. **The raw password is never stored anywhere** — not in memory longer than needed, not in logs, not in preferences.
2. **The derived key (from Argon2) is never stored anywhere** — computed on demand, used to decrypt master key, then discarded.
3. **The master key lives only in `SessionManager` in memory** — cleared on vault lock, app background, or timeout.
4. **Private keys live only in `SessionManager` in memory** — same rules as master key.
5. **Every file uses a unique, randomly generated file encryption key** — never reuse keys across files.
6. **Every AES-GCM encryption uses a unique random 12-byte IV** — never reuse IVs.
7. **Firebase never receives plaintext** — files, metadata, keys are always encrypted client-side before upload.
8. **The `crypto/` package has zero Firebase imports** — cryptographic code is completely isolated from networking.

### 5.4 AES-GCM File Format

```
[12 bytes: IV][N bytes: AES-256-GCM encrypted content]
```

Encryption is **streaming** — files are read/written in 8192-byte chunks. Never load an entire file into memory.

### 5.5 X25519 Key Exchange (File Sharing)

```
sharedSecret = X25519(senderPrivateKey, recipientPublicKey)
encryptionKey = HKDF-SHA256(sharedSecret, salt, info, 32 bytes)
encryptedFileKey = AES-256-GCM(fileKey, encryptionKey)
```

---

## 6. Firebase Data Structure

```
Firestore:
├── users/{userId}/
│   ├── keys/
│   │   ├── masterKey          → { ciphertext: Base64, iv: Base64 }
│   │   ├── x25519PrivateKey   → { ciphertext: Base64, iv: Base64 }
│   │   └── ed25519PrivateKey  → { ciphertext: Base64, iv: Base64 }
│   ├── publicKeys/
│   │   ├── x25519PublicKey    → Base64 string (PLAINTEXT — discoverable)
│   │   └── ed25519PublicKey   → Base64 string (PLAINTEXT — discoverable)
│   └── metadataBackup/
│       └── latest             → { ciphertext: Base64, iv: Base64, timestamp: Long }
│
└── shares/{shareId}/
    ├── senderId               → String
    ├── recipientId            → String
    ├── fileName               → String
    ├── fileSize               → Long
    ├── mimeType               → String
    ├── encryptedFileKey       → { ciphertext: Base64, iv: Base64 }
    ├── senderEphemeralPublic  → Base64
    ├── senderSignature        → Base64
    ├── timestamp              → Long
    └── status                 → "PENDING" | "ACCEPTED" | "REJECTED"

Firebase Storage:
└── shares/{shareId}/{fileId}.enc   ← encrypted file (AES-256-GCM)
```

---

## 7. Local Storage Structure

```
Android External Files Dir (getExternalFilesDir(null)):
└── SecureMe_Vault/
    ├── .nomedia                     ← prevents gallery indexing
    ├── vault_metadata.enc           ← AES-256-GCM encrypted VaultMetadata JSON
    └── {uuid}.enc                   ← individually encrypted vault files

Android Cache Dir (getCacheDir()):
└── temp_view/
    └── {uuid}.tmp                   ← decrypted temp files (cleared on screen exit)

EncryptedSharedPreferences:
└── argon2_salt                      ← 32-byte random salt (Base64)

DataStore:
├── vault_initialized                ← Boolean
├── auto_lock_timeout_minutes        ← Int
└── biometric_enabled                ← Boolean

Room Database:
└── trusted_contacts table           ← TrustedContact entities
```

---

## 8. UI Design System

### Color Palette (Dark Theme Only)

```kotlin
object SecureMeColors {
    val Background    = Color(0xFF0A0E1A)  // deep navy — app background
    val Surface       = Color(0xFF1A1E2E)  // slightly lighter — cards, sheets
    val SurfaceVariant = Color(0xFF252840) // elevated surfaces
    val Primary       = Color(0xFF4FC3F7)  // light blue — primary actions
    val PrimaryVariant = Color(0xFF0288D1) // darker blue — pressed states
    val Secondary     = Color(0xFF80CBC4)  // teal — secondary actions
    val Accent        = Color(0xFF7C4DFF)  // purple — highlights
    val Error         = Color(0xFFCF6679)  // muted red — errors
    val OnBackground  = Color(0xFFE8EAF6)  // near-white — primary text
    val OnSurface     = Color(0xFFB0BEC5)  // grey — secondary text
    val Success       = Color(0xFF66BB6A)  // green — success states
}
```

### Typography

```kotlin
// Use MaterialTheme.typography — customize:
// displayLarge: 57sp, displayMedium: 45sp
// headlineLarge: 32sp bold, headlineMedium: 28sp bold
// titleLarge: 22sp medium, titleMedium: 16sp medium
// bodyLarge: 16sp, bodyMedium: 14sp
// labelLarge: 14sp medium (buttons), labelSmall: 11sp
```

### Design Principles

- Dark theme only — no light theme
- Rounded corners: `RoundedCornerShape(12.dp)` for cards, `16.dp` for bottom sheets, `8.dp` for buttons
- Elevation via background color difference — avoid hard shadows
- Animations: use `AnimatedVisibility`, `animateContentSize()`, `animateFloatAsState()` — keep under 300ms
- Always show loading skeleton shimmer instead of blank screens
- Empty states: Lottie animation + descriptive text + action button

---

## 9. Security Hardening Checklist

Every screen and component must comply:

- [ ] `FLAG_SECURE` set on all windows — no screenshots, no app switcher previews
- [ ] No sensitive data in logs — use `SecureLogger` which is a no-op in release
- [ ] No sensitive data in `Bundle` or `SavedStateHandle` that persists across process death
- [ ] Clipboard auto-cleared 60 seconds after any sensitive copy operation
- [ ] Temp decrypted files deleted immediately when viewer screen exits
- [ ] `SessionManager` cleared on: vault lock, app background (configurable), timeout
- [ ] All crypto uses `SecureRandom` — never `Random` or `Math.random()`
- [ ] BouncyCastle explicitly initialized: `Security.addProvider(BouncyCastleProvider())`
- [ ] Root detection warning shown (non-blocking) on rooted devices
- [ ] Debug logs and DevTools screen only compiled in `debug` buildType

---

## 10. Naming Conventions

| Type | Convention | Example |
|---|---|---|
| Composable functions | PascalCase | `HomeScreen`, `VaultFileCard` |
| ViewModels | PascalCase + ViewModel | `HomeViewModel` |
| Use cases | PascalCase + UseCase | `ImportFileUseCase` |
| Repositories (interface) | PascalCase + Repository | `VaultRepository` |
| Repository implementations | Firebase/Local prefix + Impl | `FirebaseAuthRepositoryImpl` |
| UiState | ScreenName + UiState | `HomeUiState` |
| UiIntent | ScreenName + UiIntent | `HomeUiIntent` |
| UiEffect | ScreenName + UiEffect | `HomeUiEffect` |
| Hilt modules | Feature + Module | `AuthModule`, `VaultModule` |
| Data models | Plain noun | `VaultFileEntry`, `ShareRecord` |
| Encrypted data wrapper | `EncryptedData` | always this class for ciphertext+IV |
| Navigation routes | SCREAMING_SNAKE_CASE const | `NavigationRoutes.HOME` |

---

## 11. Domain Models Reference

```kotlin
data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray
)

data class VaultFileEntry(
    val id: String,                        // UUID
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: Long,                   // epoch millis
    val storagePath: String,               // absolute path in vault folder
    val encryptedFileKey: EncryptedData,   // file key encrypted with master key
    val ownerId: String,                   // Firebase UID
    val isShared: Boolean = false          // true if received from another user
)

data class VaultMetadata(
    val version: Int = 1,
    val ownerId: String,
    val entries: List<VaultFileEntry>
)

data class ShareRecord(
    val shareId: String,
    val senderId: String,
    val recipientId: String,
    val fileId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val encryptedFileKey: EncryptedData,
    val senderEphemeralPublicKey: ByteArray,  // sender's X25519 ephemeral public key
    val senderSignature: ByteArray,            // Ed25519 signature over share data
    val timestamp: Long,
    val status: ShareStatus
)

enum class ShareStatus { PENDING, ACCEPTED, REJECTED }

data class TrustedContact(
    val userId: String,
    val displayName: String,
    val email: String,
    val trustedFingerprint: String,       // SHA-256(x25519Public + ed25519Public), hex grouped
    val verifiedAt: Long,
    val isTrusted: Boolean
)

data class UserKeyBundle(
    val userId: String,
    val x25519PublicKey: ByteArray,
    val ed25519PublicKey: ByteArray
)
```

---

## 12. Current Build Progress

> **Update this section yourself as you complete each stage.**

- [x] Stage 1: Project Setup & Empty App Shell
- [x] Stage 2: Splash Screen
- [x] Stage 3: Navigation Graph & Screen Skeletons
- [x] Stage 4: Authentication UI
- [x] Stage 5: Firebase Authentication Integration
- [x] Stage 6: Onboarding — Password Setup & Key Derivation
- [x] Stage 7: Cryptographic Core — Master Key
- [x] Stage 8: Asymmetric Key Pairs (X25519 & Ed25519)
- [x] Stage 9: Local Vault Storage Setup
- [x] Stage 10: File Encryption & Import
- [x] Stage 11: Home Screen File Grid & Tabs
- [x] Stage 12: File Decryption & Viewer
- [x] Stage 13: File Deletion & Vault Lock
- [ ] Stage 14: Encrypted File Sharing — Send
- [ ] Stage 15: Encrypted File Sharing — Receive
- [ ] Stage 16: Metadata Cloud Backup & Restore
- [ ] Stage 17: Contact Identity Verification
- [ ] Stage 18: Security Hardening & Final Polish

---

## 13. How to Give Prompts to Gemini

### For a new stage (start of session):
```
[Paste this entire file first]

---

I am now working on Stage X: [Stage Title].

Here are the relevant existing files from my project:
[Paste the files Gemini needs to be aware of]

Here is the implementation prompt for this stage:
[Paste the Stage prompt from the Dev Plan document]
```

### For a bug fix:
```
[Paste this entire file first]

---

I have a bug in [ClassName / feature].

Here is the relevant code:
[paste code]

The problem is: [describe what happens vs what should happen]

Fix it following the project architecture and conventions in the rules above.
```

### For adding something not in the plan:
```
[Paste this entire file first]

---

I want to add [feature description] to SecureMe.

It should follow all the architecture rules above. It touches these existing files:
[list files]

Implement it.
```

### For a code review:
```
[Paste this entire file first]

---

Review this code for SecureMe. Check for:
1. MVI pattern compliance
2. Clean Architecture violations (wrong layer dependencies)
3. Cryptographic security issues
4. Memory leaks or coroutine misuse
5. Missing error handling

[paste code]
```

---

## 14. Things Gemini Must Never Do

When generating code for SecureMe, these are hard restrictions:

1. **Never store the vault password, derived key, or master key to disk** — they are in-memory only.
2. **Never add `Log.d/e/v` with sensitive data** — always use `SecureLogger`.
3. **Never use `Random` for crypto** — always `SecureRandom`.
4. **Never load entire encrypted files into a `ByteArray`** — always stream in chunks.
5. **Never add `android:allowBackup="true"`** — already set to false in manifest.
6. **Never import Firebase classes in `domain/` or `crypto/` packages.**
7. **Never import `data/` layer classes in `presentation/` layer.**
8. **Never create a Singleton outside of Hilt** — no companion object `getInstance()`.
9. **Never write a ViewModel that takes a `Context` parameter** — use `@ApplicationContext` injected into repositories instead.
10. **Never skip the `Result<T>` wrapper on operations that can fail.**
11. **Never hardcode encryption keys, passwords, or any secrets** — all keys are generated at runtime.
12. **Never reuse an AES-GCM IV** — always generate a fresh `SecureRandom` IV per encryption.

---

*End of SecureMe Project Rules — paste everything above this line into Gemini before each session.*
