# Roost — Grow Your Dragon By Spending Wisely

**Roost** is a fully offline, on-device budgeting game for the Google AI Edge × Qualcomm LiteRT Hackathon. You care for **SnapDragon**, a Snapdragon-inspired baby dragon whose health, mood, and level reflect how responsibly you've been spending. Log purchases, scan receipts, and ask the dragon for advice — every byte of financial data stays on your phone.

## Why On-Device

Personal financial data is exactly the kind of thing people refuse to send to a cloud LLM. Roost never asks for it: there is no internet permission in the manifest, no Firebase, no Plaid, no OpenAI. The app uses LiteRT-LM compiled models running on the Snapdragon NPU to generate budgeting advice locally, and a deterministic rule engine to make the actual game-state decisions.

## Track 1 — LLM Based Consumer Use Journeys

| Judging criterion | How Roost addresses it |
|---|---|
| **Must use LiteRT/LiteRT-LM compiled model API** | Gemma 4 E2B (`.litertlm`) answers user questions; FastVLM 0.5B (`.litertlm`) extracts purchase details from a receipt photo |
| **Technological implementation** | Inference is gated behind explicit user actions (ask the dragon / scan a receipt) — no continuous polling, no background battery drain. Rule engine is pure Kotlin and runs in microseconds, so the LLM is invoked only when natural-language output is genuinely needed |
| **Use case + innovation** | Gamified budgeting that's fully offline. Existing budget apps (Mint, YNAB, Rocket Money) require cloud accounts. Roost shows what the local-LLM era enables: a financial coach that's literally yours |
| **Deployment + accessibility** | Single APK. No login, no signup, no internet. Open the app and you have a dragon |
| **Presentation + documentation** | This README, in-code comments at every TODO marker for real model integration, a 90-second demo script |

## How It Works

1. User logs a purchase (manual entry or "scan receipt" — vision model returns merchant + amount + category).
2. **Rule engine** updates SnapDragon's health, XP, mood, and level based on how the purchase affects the relevant weekly budget.
3. **Gemma 4 E2B** is invoked only when the user asks a question (*"Why is my dragon tired?"*, *"Can I afford food today?"*) — it generates a short, friendly explanation grounded in the user's actual spending.
4. Everything is persisted in a local Room database. No network calls, ever.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  UI (Jetpack Compose)                                        │
│  HomeScreen · AddPurchaseScreen · AskDragonScreen            │
└────────┬────────────────────────────────────────────────────┘
         │ ViewModel (HomeVM / AddPurchaseVM / AskDragonVM)
         ▼
┌─────────────────────────┬───────────────────────────────────┐
│ Domain (deterministic)  │ Runtime (probabilistic)           │
│  BudgetEngine           │  LocalLLMEngine ── LiteRtGemmaEng. │
│  DragonStateEngine      │  ReceiptVisionEngine ── LiteRtVis. │
│                         │  PromptBuilder                    │
└─────────────────────────┴───────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│  Data (Room + Repositories)                                  │
│  Purchase · BudgetCategory · DragonState · AiAdvice          │
└─────────────────────────────────────────────────────────────┘
```

The split between *deterministic* domain logic and *probabilistic* runtime is load-bearing: **the LLM never makes financial decisions, it only explains them.** This means dragon health is always computed from auditable rules, and Gemma's job is purely to translate those rules into friendly language.

## Model Stack

| Model | Role | Hardware | File |
|---|---|---|---|
| **Gemma 4 E2B** via LiteRT-LM | Short budgeting explanations | NPU SM8750 | `gemma-4-E2B-it_qualcomm_sm8750.litertlm` |
| **FastVLM 0.5B** via LiteRT-LM | Receipt → merchant + amount | NPU SM8750 | `FastVLM-0.5B.qualcomm.sm8750.litertlm` |
| **EmbeddingGemma 300M** *(future)* | Search past spending semantically | NPU/CPU | `embeddinggemma-300M_seq512_mixed-precision.qualcomm.sm8750.tflite` |

## Setup

### Prerequisites
- Samsung Galaxy S25 Ultra (Snapdragon 8 Elite SM8750)
- Android Studio Ladybug or later
- JDK 17

### 1. Build the app
```bash
# Open Android/src in Android Studio
# Sync Gradle, hit Run
```

The app launches in **DemoMode** by default (`runtime/DemoMode.kt`), where the LLM and vision models return canned responses. This lets the entire app run on any device without the model files present.

### 2. Wire real LiteRT-LM models (optional)
```bash
# Download the three model files (~4.1 GB total, requires HuggingFace login)
./scripts/download-models.sh

# Push them to the connected phone
./scripts/push-models-to-device.sh

# Then in code:
#   - Add: implementation("com.google.ai.edge.litertlm:litertlm-android:0.10.0")
#     to Android/src/app/build.gradle.kts
#   - Replace the TODO body in runtime/LiteRtGemmaEngine.kt
#   - Set DemoMode.ENABLED = false
```

## Project Structure

```
Android/src/app/src/main/java/com/roost/app/
├── MainActivity.kt              # Compose entry, simple state-based nav
├── RoostApplication.kt   # Holds the AppContainer
├── AppContainer.kt              # Manual DI: repos, engines, DB
├── data/
│   ├── Entities.kt              # Room @Entity classes
│   ├── Daos.kt                  # Room @Dao interfaces
│   ├── RoostDatabase.kt  # Room database
│   ├── Repositories.kt          # Thin DAO wrappers
│   ├── Category.kt              # The 8 spending categories
│   └── Seeder.kt                # Default budgets + initial dragon
├── domain/
│   ├── BudgetEngine.kt          # Pure budget math, week boundaries
│   └── DragonStateEngine.kt     # Health/XP/mood rules
├── runtime/
│   ├── DemoMode.kt              # Single boolean: canned vs real models
│   ├── ModelPaths.kt            # Filenames + /sdcard/Download paths
│   ├── LocalLLMEngine.kt        # interface
│   ├── MockLocalLLMEngine.kt    # demo-mode advice
│   ├── LiteRtGemmaEngine.kt     # real Gemma 4 E2B (TODO body)
│   ├── ReceiptVisionEngine.kt   # interface + mock + LiteRT placeholder
│   └── PromptBuilder.kt         # Gemma prompt grounded in user data
├── ui/
│   ├── HomeScreen.kt            # Dragon card, budget summary, recent
│   ├── AddPurchaseScreen.kt     # Form + scan-receipt button
│   ├── AskDragonScreen.kt       # 3 preset questions + free text
│   ├── HomeViewModel.kt
│   ├── AddPurchaseViewModel.kt
│   ├── AskDragonViewModel.kt
│   └── theme/Theme.kt           # Snapdragon dark palette
```

## 90-Second Demo Script

1. **0:00 – 0:15** Open app. Dragon card: *SnapDragon, mood Stable, level 2, 82/100 health, $124.50 left this week.*
2. **0:15 – 0:35** Tap **+ Add Purchase**. Tap **Scan receipt**. *"Chipotle, $14.25, Food"* fills in. Tap **Save**. Return to home — XP bumps up, dragon's mood holds at Stable.
3. **0:35 – 0:55** Add another: tap Scan receipt twice more — *"AMC, $22, Entertainment"* — after the second, mood flips to **Worried**, health drops 5.
4. **0:55 – 1:15** Tap **Ask Dragon** → tap *"Why is my dragon tired?"*. Gemma streams: *"You spent past your Entertainment budget this week — try a no-spend night to recover energy."*
5. **1:15 – 1:30** Show **airplane mode toggle** in the notification shade. Open Ask Dragon again, ask another question — same response speed. *"All on this phone. No internet. Your money data stays in your pocket."*

## Privacy & Network Posture

- **No `<uses-permission android:name="android.permission.INTERNET" />`** in the manifest. The app cannot make network calls.
- All persistence is in a single Room database file: `roost.db` in app-private storage.
- Model files live in `/sdcard/Download/` and are loaded via LiteRT-LM only when an inference is requested.

---

*Built for the Google AI Edge × Qualcomm LiteRT Hackathon 2026.*
