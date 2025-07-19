# BetterMux

A modern terminal emulator for Android with enhanced features and secure authentication.

---

## Overview

**BetterMux** is a powerful Android terminal application designed to enhance the command-line experience on mobile devices. It combines terminal emulation with modern authentication, security, and intuitive UI using Jetpack Compose.

---

## Features

- Terminal Emulation – Full-featured interactive command-line interface
- Secure Authentication – Google OAuth, GitHub OAuth, and Firebase Authentication
- Biometric Support – Device-level fingerprint unlock
- Modern UI – Built using Jetpack Compose for a fluid, reactive experience
- Directory Navigation – Visual file and folder browser
- Command History – View and reuse previous terminal commands
- Performance Optimized – Faster builds with Gradle caching and parallel execution

---

## Technologies Used

- Kotlin – Modern Android programming language
- Jetpack Compose – Declarative UI toolkit
- Firebase Authentication – Secure sign-in/sign-up flows
- GitHub OAuth – Alternate login option
- Hilt – Dependency injection
- MVVM – Modular, testable architecture
- Navigation Component – Declarative in-app navigation
- Retrofit – REST API communication
- BiometricPrompt API – Native fingerprint authentication

---

## Requirements

- Android 9.0+ (API Level 28 or newer)
- Internet Connectivity – Required for authentication services
- Android Studio – With bundled JDK recommended

---

## Installation

1. Clone the repository:
    ```bash
    git clone https://github.com/yourusername/bettermux.git
    cd bettermux
    ```

2. Open the project in Android Studio

3. Set the required keys in your `local.properties` file:
    ```properties
    sdk.dir=/Users/ayaan/Library/Android/sdk

    GEMINI_API_KEY=your_gemini_api_key_here

    oauth_client_id=your_google_oauth_client_id
    github_client_id=your_github_client_id
    github_client_secret=your_github_client_secret

    ```

4. Set the required keys in your `gradle.properties` file:
    ```properties
    speedchecker.username=<get this from speedchecker's official github repo>
    speedchecker.password=<get this from speedchecker's official github repo>
    ```  

5. Sync Gradle and run the app on an emulator or device

---

## Project Structure

```
app/
└── src/
    └── main/
        └── java/
            └── com/ayaan/mongofsterminal/
                ├── mainActivity/          # App entry point
                ├── navigation/            # Navigation logic
                └── presentation/
                    ├── auth/              # Auth screens (Google, GitHub, Firebase)
                    ├── splashscreen/      # Triggers backend setup (Render.com)
                    ├── terminalscreen/    # Terminal interface
                    └── fingerPrintScreen/ # Biometric unlock
```

---

## Gradle Performance Tweaks (enabled)

- `org.gradle.caching=true`
- `org.gradle.parallel=true`
- `org.gradle.configuration-cache=true`
- `org.gradle.daemon=true`
- `kotlin.incremental=true`
- `android.nonTransitiveRClass=true`
- `android.enableR8.fullMode=true`

---

## Contributing

Contributions are welcome!

1. Fork the repository
2. Create a new branch
3. Make your changes
4. Submit a Pull Request  
