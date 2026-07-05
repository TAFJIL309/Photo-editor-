# AI Creative Studio

> **Premium AI-powered Image-to-Image Editor and Photo Generator**

AI Creative Studio is an advanced, offline-capable mobile application designed for next-generation image editing and generation on Android 15. Powered by Google's state-of-the-art **Gemini API**, this application provides a sophisticated playground for mask-based brush editing, generative fill, and intelligent picture transformation.

---

## 🚀 Key Features

*   **Intelligent Gemini Editing**: Seamlessly interact with Gemini models for precise image-to-image transformations, contextual edits, and creative styling.
*   **Precision Mask Canvas**: A robust, touch-friendly canvas featuring fully adjustable brush sizes, undo/redo capabilities, and high-performance mask drawing.
*   **Full Android 15 Support**: Fully compiled and optimized against Android SDK 36, utilizing modern Edge-to-Edge layouts, adaptive spacing, and premium Material Design 3 components.
*   **Offline Testing Ready**: Bundled with pre-built, ready-to-test APKs directly inside the repository for instant deployment on physical devices.

---

## 📦 Download Pre-built APKs

For quick testing and review, you can find the compiled Android package files directly in the root of this repository:
1.  **[AICreativeStudio_Android15.apk](./AICreativeStudio_Android15.apk)**: Optimized build tailored for Android 15 devices.
2.  **[AICreativeStudio.apk](./AICreativeStudio.apk)**: Standard release build for broad device compatibility.
3.  **[LuminaStudio.apk](./LuminaStudio.apk)**: Alternative studio edition package.

---

## 🛠️ Architecture & Tech Stack

AI Creative Studio is built from the ground up using modern Android development practices:

*   **UI Framework**: Jetpack Compose with strict adherence to **Material Design 3 (M3)** spacing, typography, and dynamic color systems.
*   **Asynchronous Engine**: Built entirely with Kotlin Coroutines and asynchronous state-handling via `StateFlow`.
*   **Core API Integration**: Native integration with Google's server-side Gemini SDKs for lightning-fast image analysis and editing.
*   **Responsive Layouts**: Designed using window size classes to render elegantly on mobile, tablet, and foldable form factors.

---

## 💻 Local Setup & Development

To compile and run the project locally:

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/<your-username>/ai-creative-studio.git
    cd ai-creative-studio
    ```

2.  **Configure Environment Secrets**:
    Enter your Gemini API Key in the **Secrets panel in AI Studio** or create a local `.env` file based on `.env.example`:
    ```env
    GEMINI_API_KEY=your_api_key_here
    ```

3.  **Build the Project**:
    Compile the debug package using Gradle:
    ```bash
    gradle :app:assembleDebug
    ```

4.  **Install the App**:
    The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.
