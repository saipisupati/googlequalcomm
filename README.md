# PawAI — On-Device Pet Nutrition Assistant

PawAI is a privacy-first AI nutrition coach for your pets. Point your camera at your pet's food bowl and get an instant, personalized assessment of whether your pet is eating the right amount — entirely on-device, with no data ever leaving your phone.

## How It Works

1. **Visual Food Recognition (FastVLM):** Camera scans the bowl. FastVLM identifies the food type, brand, and estimated portion size running on the Snapdragon NPU in real time.

2. **Personalized Assessment (Gemma 4):** Gemma receives the food description alongside your pet's profile (breed, age, weight) and generates a plain-English nutrition assessment and feeding recommendation.

3. **Feeding History (EmbeddingGemma):** Every meal is stored as a vector embedding locally. PawAI detects patterns over time — *"Mochi has eaten 20% less than usual the past two days"* — and factors this into its recommendations.

## Model Architecture

| Model | Role | Hardware |
|---|---|---|
| **FastVLM 0.5B** | Food identification from camera | NPU SM8750 |
| **Gemma 4 E2B** via LiteRT-LM | Nutrition reasoning and assessment | NPU SM8750 |
| **EmbeddingGemma** | Local feeding history and pattern detection | NPU / CPU |

### Memory Scheduling

FastVLM and Gemma never run simultaneously. FastVLM runs during the camera scan, then unloads. Gemma loads, generates the assessment, then unloads. EmbeddingGemma is lightweight enough to stay resident throughout.

## Privacy First

Your pet's photos, health data, and feeding history never leave your device. Everything runs on the Snapdragon 8 Elite NPU.

## Setup

### Prerequisites
- Samsung Galaxy S25 Ultra (Snapdragon 8 Elite SM8750)
- Android Studio Ladybug or later
- ADB with USB debugging enabled

### Model Setup
Push the pre-compiled LiteRT models to your device's `Download` folder:

```bash
adb push FastVLM-0.5B.qualcomm.sm8750.litertlm /sdcard/Download/
adb push gemma-4-E2B-it.litertlm /sdcard/Download/
adb push embedding_gemma.tflite /sdcard/Download/
```

### Build
1. Open the `Android` folder in Android Studio
2. File → Sync Project with Gradle Files
3. Hit **Run**

## Project Structure

- `PawAIScreen.kt` — Compose UI, CameraX, result card
- `PawAIViewModel.kt` — Model memory scheduling, pipeline orchestration
- `PetProfileStore.kt` — Local pet profile and EmbeddingGemma history
- `NutritionPromptBuilder.kt` — Structures Gemma prompt from FastVLM output + pet profile

## App Flow

1. **Pet Profile Setup** *(first launch)* — name, species, breed, age, weight, dietary restrictions, daily calorie target
2. **Scan Screen** — live camera feed, FastVLM detects food as editable chips
3. **Assessment Card** — streamed Gemma output with portion rating (good / slightly over / significantly over / under), log meal button
4. **History** — timeline of meals, intake-vs-target trend line, flags for unusual eating patterns

---
*Built for the Google AI Edge x Qualcomm Hackathon 2026.*
