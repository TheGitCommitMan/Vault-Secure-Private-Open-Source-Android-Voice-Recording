# 🔐 Vault: Secure Voice Recording

**Professional-grade, encrypted audio capture for Android.**

Vault is a privacy-first application designed for users who require absolute security for their voice data. Utilizing modern Android security standards and a zero-trust architecture, Vault ensures your recordings are protected from the moment they are captured.

---

## 🛡️ Security Architecture

- **Military-Grade Encryption**: Every recording is encrypted at rest using **AES-256-GCM** with keys managed in the **Android Keystore System**.
- **Biometric Authentication**: Secure-entry UI requiring Fingerprint or Face Unlock before accessing the recording library.
- **Encrypted File System**: Audio data is never stored in cleartext; it remains in a private, encrypted internal storage partition.
- **Zero-Cloud Policy**: Your voice data stays on your device. No cloud syncing, no external uploads.

---

## 🚀 Features

- **High-Fidelity Capture**: Lossless audio recording with real-time waveform visualization.
- **Secure Library**: Metadata-only indexing for rapid search without compromising file security.
- **Auto-Lock**: Immediate session termination when the application is backgrounded.

---

## 🛠️ Setup & Audit

1.  **Clone**: `git clone https://github.com/TheGitCommitMan/Vault-Secure-Private-Open-Source-Android-Voice-Recording.git`
2.  **Dependencies**: Uses the **Jetpack Security** library for crypto-management.
3.  **Run**: Deploy via Android Studio. Ensure Biometric Hardware is available on the target device.
