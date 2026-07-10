# Vault: Encrypted Voice Architecture

Vault is a privacy-focused, open-source Android application designed for secure audio capture and storage. Built on a foundation of zero-trust principles, Vault utilizes industry-standard AES-256 encryption to ensure that voice recordings remain private and accessible only to the authorized user.

Vault is more than a recording tool; it is a commitment to mobile security, featuring an encrypted file system and a secure-entry UI.

## 🔐 Technical Setup

Instructions for developers looking to contribute to or audit the Vault security architecture.

### Prerequisites

- [Android Studio](https://developer.android.com/studio)
- Basic understanding of Android Keystore and Encryption APIs

### Installation Guide

1. **Open Source Core**
   Import the project into Android Studio via the **Open** menu.

2. **Environment Synchronization**
   Wait for the project to index and for Gradle to download the necessary security libraries.

3. **Secrets Management**
   The application's intelligent metadata indexing requires a secure API bridge.
   - Create a `.env` file in the root directory.
   - Add: `GEMINI_API_KEY=your_api_key_string`
   - See `.env.example` for reference.

4. **Build Permissions**
   In the `build.gradle.kts` file, ensure the `signingConfig` is optimized for your local development environment to facilitate seamless debugging and iterative testing.

5. **Deploy and Audit**
   Deploy the application to a secure emulator or test device. Once running, you can explore the encryption workflow and recording pipeline.
