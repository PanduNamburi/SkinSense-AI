# SkinSense AI 🩺🤖

SkinSense AI is a comprehensive, intelligent Android health-tech application designed to bridge the gap between AI and dermatology. Built entirely with Kotlin and modern **Jetpack Compose**, this app allows users to scan skin conditions using their smartphone camera and provides instant AI-powered dermatological disease classifications with over 90% accuracy.

Beyond AI analysis, SkinSense acts as a complete telehealth platform for both **Patients** and **Doctors**, backed by a secure **Firebase** infrastructure.

---

## ✨ Key Features

* **On-Device AI Classification:** Leverages a custom-trained EfficientNetB3 deep learning model to accurately predict various skin diseases in seconds.
* **Dual-Role Ecosystem:** Dedicated interfaces and workflows for both Patients and Dermatologists.
* **Telehealth Consultations:** Patients can maintain their scan history, find verified dermatologists, and book or initiate real-time chat consultations.
* **Integrated AI Chatbot:** A Groq-powered AI assistant available 24/7 to provide preliminary guidance and answer skin-related questions.
* **Modern & Secure:** Premium, responsive Jetpack Compose UI with biometric authentication and robust Firebase Firestore security rules ensuring total privacy.

---

## 🛠 Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Backend:** Firebase Authentication, Firestore Database, Firebase Storage
* **Machine Learning:** TensorFlow Lite / EfficientNetB3
* **AI Chatbot:** Groq API

---

## 📥 How to Install the APK

The fully compiled APK is available directly in this repository! To install the application onto your Android device:

1. **Download the APK:** Go to the `releases/` folder in this GitHub repository and download the `SkinSenseAI-debug.apk` file.
2. **Transfer to Device:** Transfer the downloaded APK file to your Android phone (e.g., via Google Drive, Email, or USB transfer).
3. **Locate the APK:** Open your device's **File Manager** and find the downloaded APK.
4. **Allow Unknown Sources:** Tap on the APK to install it. If prompted with a security warning, tap **Settings** and toggle on **"Allow from this source"**.
5. **Install:** Go back, tap **Install**, and wait for the installation to finish.
6. **Open:** Once installed, tap **Open** to launch SkinSense AI!

---

## 📖 How to Use the App

### For Patients 👤
1. **Sign Up / Login:** Open the app and sign up as a **Patient**. You can enable biometric login for faster future access.
2. **Scan a Skin Condition:** Tap the central floating action button (Scan icon) on the navigation bar. Follow the on-screen photo guidelines and upload a picture of the concerned skin area. The AI will provide an immediate prediction report.
3. **Consult a Dermatologist:** Go to the **Consult** tab to browse verified dermatologists. You can view their profiles, check their credentials, and book an appointment or send them your AI scan result for a second opinion.
4. **AI Assistant:** Need quick advice? Tap the **Chatbot** icon to ask general skin care queries.

### For Doctors 👨‍⚕️
1. **Apply & Verify:** Sign up as a **Doctor**. Note: Doctor accounts may require administrative approval before they are fully active to ensure quality care.
2. **Manage Consultations:** Once approved, your home screen acts as a dashboard. You can review connection requests from patients.
3. **Review Scans:** Doctors can securely review the AI-assisted scan results sent by their connected patients to offer verified medical advice.
4. **Chat & Telehealth:** Use the **Chats** tab to message patients securely in real-time regarding their conditions.

---

## 🔒 Permissions Required
* **Camera:** Required to take pictures of skin conditions.
* **Storage/Gallery:** Required to upload existing pictures for analysis.
* **Biometrics (Optional):** Used for secure app login.
