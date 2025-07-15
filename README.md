# BetterMux

A modern terminal emulator for Android with enhanced features and secure authentication.

## Overview

BetterMux is an Android terminal application designed to provide a better command-line experience on mobile devices. It combines terminal functionality with modern authentication methods including Firebase authentication and biometric security.

## Features

- **Terminal Emulation**: Full-featured command-line interface
- **Secure Authentication**: Firebase authentication with sign-in and sign-up capabilities
- **Biometric Support**: Fingerprint authentication for enhanced security
- **Modern UI**: Built with Jetpack Compose for a fluid, responsive interface
- **Directory Navigation**: Visual file and directory browsing
- **Command History**: Track and reuse previous commands

## Technologies Used

- **Kotlin**: Modern programming language for Android development
- **Jetpack Compose**: UI toolkit for native interface development
- **Firebase Authentication**: Secure user authentication services
- **Hilt**: Dependency injection framework
- **MVVM Architecture**: Clean separation of concerns
- **Navigation Component**: Managing app navigation
- **Retrofit**: Type-safe HTTP client for API communication
- **Biometric Authentication**: Secure device-level authentication

## Requirements

- Android API level 28+ (Android 9.0 Pie or newer)
- Internet connectivity for authentication services

## Installation

1. Clone this repository
2. Open the project in Android Studio
3. Add the Gemini api key as "GEMINI_API_KEY" in your local.properties file
4. Sync Gradle dependencies
5. Run the app on an emulator or physical device

```bash
git clone https://github.com/yourusername/bettermux.git
cd bettermux
```

## Project Structure

- **app/src/main/java/com/ayaan/mongofsterminal/**
  - **mainActivity/**: Main application entry point
  - **navigation/**: App navigation components
  - **presentation/**: UI screens and components
    - **auth/**: Authentication screens
    - **splashscreen/**: activates the free backend service on render
    - **terminalscreen/**: Terminal UI and functionality
    - **fingerPrintScreen/**: Biometric authentication


Contributions are welcome! Please feel free to submit a Pull Request.

