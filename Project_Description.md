# SkinSense AI

SkinSense AI is a comprehensive Android application designed to bridge the gap between users and dermatological care. It leverages advanced Artificial Intelligence (Machine Learning and Large Language Models) to empower users to proactively monitor their skin health, receive preliminary assessments, and connect with certified dermatologists.

## Core Problem Statement & Real-World Approach

Skin conditions are incredibly common, but access to dermatologists can be hindered by long wait times, geographical distances, or high costs. Many individuals ignore early signs of skin anomalies, leading to severe complications later.

**How SkinSense AI solves this:**
1. **Accessibility**: Users can instantly analyze a photo of their skin anomaly from the comfort of their home using an on-device Machine Learning model.
2. **Pre-screening & Guidance**: It doesn't replace doctors but acts as a triage tool. It informs users about potential conditions and provides AI-driven follow-up advice (via conversational AI) to ease anxiety and provide home-care tips while they seek professional help.
3. **Connecting with Experts**: Based on the severity and location, the application dynamically suggests nearby dermatologists on a structured, interactive map.
4. **Data Portability**: Users can maintain a persistent history of their scans in the cloud to show doctors how a condition has progressed over time.

---

## Key Features

- **Multi-Role Systems**: 
  - **Patients**: Can upload photos, receive AI-powered health analyses, chat with a smart health-bot, and search for doctors.
  - **Doctors**: A dedicated portal to join the network, manage their profile, and receive patients.
  - **Admin Dashboard**: A centralized console to verify doctor credentials and manage users.

- **On-Device Image Validation (Gatekeeper)**:
  - Uses MediaPipe to strictly validate that uploaded photos actually contain human skin (faces, hands, or a significant body mask). This prevents the AI from processing irrelevant images (like furniture or pets) and avoids false diagnoses.

- **Skin Disease Classification (TFLite Inference)**:
  - A lightweight, on-device TensorFlow Lite model that analyzes the skin image and predicts the likelihood of various skin disorders in real-time without needing a constant internet connection.

- **Conversational Health AI (Gemini Bot)**:
  - An integrated chatbot fueled by Google's Gemini LLM. It can discuss the initial scan results, answer general dermatology questions in a friendly manner, and summarize advice.

- **Geospatial Dermatologist Finder**:
  - A real-time, interactive map built inside the app. It uses the user's browser geolocation to plot nearby hospitals and specialized clinics, providing direct routing capabilities.

- **Persistent Cloud History**:
  - Scan results, including condition severity and uploaded image metadata, are saved to a global cloud database allowing users to view their medical timeline across multiple devices.

---

## Technologies Used

### Frontend & UI
- **Kotlin**: The primary programming language.
- **Jetpack Compose**: Used entirely for building modern, declarative, and responsive user interfaces.
- **Material Design 3 (M3)**: Used for modern Android aesthetics and components.
- **Coil**: For asynchronous image rendering and caching.

### Artificial Intelligence & Machine Learning
- **TensorFlow Lite (TFLite)**: Powers the Custom Image Classification model for outputting accurate disease predictions on mobile hardware.
- **MediaPipe Tasks Vision SDK**: Utilized for the `SkinImageValidator` (Face Detector, Hand Landmarker, and Image Segmenter) to verify that an uploaded image is indeed a human body part.
- **Google Generative AI (Gemini SDK)**: Powers the conversational AI chatbot interface.

### Backend, Database & Authentication
- **Firebase Authentication**: Manages secure email/password sign-ups and role-based login credentials.
- **Cloud Firestore**: A NoSQL cloud database used to store cross-device "Scan History" and "User Profiles".

### Mapping & Geolocation
- **Leaflet.js**: An open-source JavaScript library utilized inside an Android WebView to render dynamic, interactive maps.
- **HTML5 Geolocation API**: Intercepted in Android to allow the Leaflet map to request and plot the user's live physical location.

### Architecture & Async
- **MVVM Architecture**: (Model-View-ViewModel) ensures a clean separation of concerns and robust UI state management.
- **Kotlin Coroutines & Flow**: For seamless background asynchronous operations, managing API requests, and database queries without blocking the main UI thread.
