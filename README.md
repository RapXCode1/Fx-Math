# Fx-Math: AI-Powered Smart Mathematics Platform

**Developed by RapXCode**  
**Version:** 1.0.0-Release  
**Target Platforms:** Android & iOS (Native Jetpack Compose Implementation optimized for high performance)

---

## 🌟 Executive Summary

**Fx-Math** is not just an ordinary calculator. It is a premium, high-fidelity, offline-first **Smart Mathematics Platform** crafted with absolute precision. Designed with a gorgeous, luxury dark space glassmorphic layout inspired by iOS, Fx-Math is optimized to provide custom standard and scientific calculations instantly and offline, while hiding a massive "superpower": an elegant, glowing AI float key that unfurls a multi-model conversational assistant.

Fx-Math integrates a **Symbolic Kotlin Math Engine**, an **Interactive Cartesian 2D Graph Plotter**, a **Handwritten Math Sketchpad with OCR Recognition**, local **SQLite Persistence (Room)**, and support for four state-of-the-art AI models:
1. **FYY-Llama 3.3** (Llama 3.3 - 70B parameters)
2. **FYY-Llama Scout** (Llama 4 Scout)
3. **FYY-GPT OSS** (GPT OSS 120B)
4. **FYY-Qwen** (Qwen 3 32B)

---

## 🛠️ Application Architecture Guide

Fx-Math strictly adheres to **Clean Architecture** principles and the **Model-View-ViewModel (MVVM)** design pattern. It separates business logic from views to ensure maximum scalability, testability, and maintenance.

### File Structure Map

```
/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt               <-- Main view container & tab navigator
│   │   │   │   ├── data/
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── DatabaseEntities.kt   <-- Room Entities (History, Sessions, Messages)
│   │   │   │   │   │   ├── DatabaseDaos.kt       <-- Database Queries
│   │   │   │   │   │   └── AppDatabase.kt        <-- SQLite database initialization
│   │   │   │   │   ├── network/
│   │   │   │   │   │   └── MathAiService.kt      <-- Gemini and Groq API Client
│   │   │   │   │   └── solver/
│   │   │   │   │       └── MathEngine.kt         <-- Recursive Descent Parser (Offline Solver)
│   │   │   │   └── ui/
│   │   │   │       ├── screens/
│   │   │   │       │   ├── GlassCard.kt          <-- Glassmorphic layout utilities & glows
│   │   │   │       │   ├── CalculatorScreen.kt   <-- Scientific calculator UI
│   │   │   │       │   ├── AiScreen.kt           <-- Multimodel chat UI
│   │   │   │       │   ├── ScannerScreen.kt      <-- Math Sketchpad & OCR solution card
│   │   │   │       │   ├── GraphScreen.kt        <-- 2D Canvas Graphing plotter
│   │   │   │       │   ├── HistoryScreen.kt      <-- Calc log lists & pinned chats
│   │   │   │       │   └── SettingsScreen.kt     <-- Decimals precision & Groq key secure input
│   │   │   │       └── theme/
│   │   │   │           ├── Theme.kt              <-- Premium dark-first scheme
│   │   │   │           ├── Color.kt              <-- Neon glows, custom space gradients
│   │   │   │           └── Type.kt               <-- Clean typography pairings
```

---

## 📓 Installation & Environment Setup Guide

### System Requirements
* **JDK:** Version 11 or 17
* **Android Studio:** Ladybug (2024.2.1) or higher
* **Android SDK:** Compile SDK 36, Min SDK 24 (Compatible with 98.5% of active Android devices)
* **Gradle:** Version 8.0 or higher with Kotlin DSL (`.gradle.kts`)

### Secret Key Configuration
Fx-Math uses a secure credential system to manage API keys.

1. **Gemini API:** Handled automatically in the AI Studio cloud workspace.
2. **Groq / OpenAI API Keys:** Configured dynamically. Users can enter their personal keys in the **Settings tab** which saves them locally in private SQLite databases. No hardcoded environment exposure!

---

## 🧠 Developer Guide: Engine Specifications

### 1. Symbolic Mathematics Solver (`MathEngine.kt`)
To eliminate LLM hallucinations on math operations, Fx-Math incorporates a custom-coded **Recursive Descent Parser**.
* **Precedence Grammars:** Formulated with operator precedence, resolving power indices (`^`), modulo (`%`), brackets, scientific trigonometry (`sin`, `acos`, etc.), log ranges, and factorial factor bounds.
* **Equation Solver:** Symbolically calculates the roots and discriminant of quadratic equations (`ax² + bx + c = 0`), compiling step-by-step mathematical proofs.

### 2. Cartesian 2D Graph Plotter (`GraphScreen.kt`)
Calculated dynamically on custom Compose canvas elements:
* **Grid Scaling:** Dynamically scales coordinate vectors in real time based on zoom and offset states.
* **Calculus Tangents:** Evaluates numerical derivatives at the user-selected point $x$ using the formula:
  $$\frac{df(x)}{dx} \approx \frac{f(x + h) - f(x)}{h}$$
  and draws the exact tangent line onto the coordinate graph automatically.

### 3. Handwritten Vector Sketchpad (`ScannerScreen.kt`)
Rather than depending on unreliable camera emulators, Fx-Math features a high-fidelity handwriting sketchpad:
1. Translates touch dragging events into high-density vector strokes.
2. Converts vector strokes programmatically into a program-backed `Bitmap` with black backgrounds and white lines.
3. Transmits the generated bitmap directly to the multimodal Gemini model for real-time math OCR transcribing and step-by-step solving.

---

## 🔌 API & Network Integration Guide (`MathAiService.kt`)

Fx-Math supports a dual API architecture:

```
                  ┌───────────────────────────────┐
                  │      User Prompts Math        │
                  └───────────────┬───────────────┘
                                  │
                    Is Groq API Key Set in Settings?
                     /                         \
                   YES                          NO
                   /                             \
    ┌─────────────────────────────┐       ┌─────────────────────────────┐
    │  Call Groq REST Endpoint    │       │ Call Gemini REST Endpoint   │
    │  - Model: Llama 70B / Qwen  │       │ - Model: gemini-3.5-flash   │
    │  - Header: Custom Bearer Key│       │ - Header: System Env Key    │
    └─────────────────────────────┘       └─────────────────────────────┘
```

---

## 🗄️ Database & Schema Guide (`DatabaseEntities.kt`)

Fx-Math leverages **Room Database** for high-efficiency, reactive data streams.

```
                    ┌─────────────────────────┐
                    │      AppDatabase        │
                    └────┬───────────────┬────┘
                         │               │
            ┌────────────┴───┐       ┌───┴────────────┐
            │ CalcHistoryDao │       │  AiSessionDao  │
            └────────────┬───┘       └───┬────────────┘
                         │               │
                 [calc_history]    [ai_sessions] ---> [ai_messages]
```

### Table Schemas:
1. `calc_history`:
   * `id` (INTEGER Primary Key, Auto-generate)
   * `expression` (TEXT)
   * `result` (TEXT)
   * `timestamp` (INTEGER)
   * `isFavorite` (INTEGER/BOOLEAN)
2. `ai_sessions`:
   * `id` (TEXT/UUID Primary Key)
   * `title` (TEXT)
   * `timestamp` (INTEGER)
   * `isPinned` (INTEGER/BOOLEAN)
   * `selectedModel` (TEXT)
3. `ai_messages`:
   * `id` (INTEGER Primary Key, Auto-generate)
   * `sessionId` (TEXT Foreign Key)
   * `sender` (TEXT: "user" or "ai")
   * `text` (TEXT)
   * `timestamp` (INTEGER)

---

## 🚀 Deployment & Security Guide

### Security Protocol
* **Sandbox Security:** Private local Room databases are stored in the device's protected internal storage.
* **Encrypted API inputs:** API keys are processed in-memory or written exclusively to localized preferences, preventing leakage.
* **Proguard Obfuscation:** Configured with optimization rules to prevent decompilation reverse-engineering.

---

## 🧪 Testing Guidelines

Fx-Math supports fully automated, rapid local testing suites without emulator delays using **Robolectric** and **Roborazzi**:

### 1. Execute JVM Core Unit Tests
Verifies calculation accuracy, mathematical parser grammar, and equation solvers:
```bash
gradle :app:testDebugUnitTest
```

### 2. Verify UI & Visual Screenshot Regression
Renders pixel-perfect visual reference screenshot tests:
```bash
gradle :app:verifyRoborazziDebug
```

### 3. Record Updated UI Reference Screenshots
```bash
gradle :app:recordRoborazziDebug
```
