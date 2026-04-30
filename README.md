# Kinex — Real-Time AI Form Coach

**Kinex** is a real-time, privacy-first AI workout form coach built for the Qualcomm x Google Hackathon. It leverages on-device NPU processing to track your body mechanics in real-time and provide conversational, personalized coaching without ever sending your video to the cloud.

## 🏆 How It Works

Kinex combines state-of-the-art vision models with large language models to not only tell you *what* your body is doing, but *why* it matters and *how* to fix it.

1. **Real-Time Tracking (MoveNet/MediaPipe):** Set your phone up facing you. As you exercise, Kinex runs pose estimation at 30fps entirely on the Snapdragon NPU, overlaying your skeletal structure and calculating joint angles in real-time.
2. **Rep Detection State Machine:** Kinex automatically detects when you start and finish a repetition, analyzing form issues (e.g., knee cave, shallow depth, back inclination) during the movement.
3. **Conversational Coaching (Gemma 3n):** Between reps, Kinex pauses the vision model and spins up Gemma 3n. Gemma interprets the joint angle deviations and generates natural, encouraging, and specific coaching feedback (e.g., *"Your left knee is drifting inward. Try widening your stance by 2 inches"*).
4. **Form History (EmbeddingGemma):** *[WIP]* Your form history is stored as vector embeddings locally. Kinex recalls your historical patterns to give you progressive coaching over time.

## 🧠 Model Architecture

| Model | Role | Hardware |
|---|---|---|
| **MediaPipe Pose** | Real-time 33-point joint tracking and AR skeleton overlay. | NPU / CPU |
| **Gemma 3n (1B)** | Converts mathematical angle deviations into natural language coaching feedback between reps. | NPU (SM8750) |
| **FastVLM 0.5B** | Automatically identifies the exercise you are performing from the camera feed. | NPU (SM8750) |
| **EmbeddingGemma** | Encodes historical form errors to personalize future coaching sessions. | NPU / CPU |

## 🔒 Privacy First

Nobody wants their gym videos sent to a cloud server. Kinex is **100% offline**.
- The pose tracking happens on your device.
- The coaching text is generated on your device.
- Your form history never leaves your device. 

## 🚀 Building & Running

### Prerequisites
- Samsung Galaxy S25 Ultra (Snapdragon 8 Elite SM8750)
- Android Studio Ladybug or later
- ADB installed and device connected with USB Debugging enabled

### Model Setup
To run Kinex, you must push the pre-compiled LiteRT models to your device's `Download` folder.
Run the following via ADB:

```bash
# Push Gemma 3n
adb push Gemma3-1B-IT_q4_ekv1280_sm8750.litertlm /sdcard/Download/

# Push FastVLM
adb push FastVLM-0.5B.qualcomm.sm8750.litertlm /sdcard/Download/

# Push MediaPipe Pose Landmarker
adb push pose_landmarker_lite.task /sdcard/Download/
```

### Compile
1. Open the `Android` directory in Android Studio.
2. Sync Project with Gradle Files.
3. Hit **Run** to deploy to your connected device.

## 🛠 Project Structure

Kinex is built on top of the robust Android LiteRT framework to handle complex memory scheduling and NPU delegation.

- `KinexScreen.kt` - The Compose UI, CameraX preview, and AR canvas overlay.
- `KinexViewModel.kt` - The core engine handling the Rep Detection State Machine and model memory scheduling.
- `ExerciseDatabase.kt` - The knowledge base mapping joint angles to specific exercise form criteria.

---
*Built for the Google AI Edge x Qualcomm Hackathon 2026.*
