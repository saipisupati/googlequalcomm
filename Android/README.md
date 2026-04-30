# PawAI Android

Compose + CameraX + LiteRT-LM. See top-level `README.md` for project overview.

Source lives under `src/app/src/main/java/com/pawai/app/`:

- `PawAIScreen.kt` — Compose UI, scan flow, assessment card
- `PawAIViewModel.kt` — Pipeline orchestration with FastVLM/Gemma memory scheduling
- `PetProfileStore.kt` — Local pet profile + EmbeddingGemma-backed feeding history
- `NutritionPromptBuilder.kt` — Builds the Gemma prompt from FastVLM output + pet profile + history

The Gradle project (`Android/src/build.gradle.kts`, `settings.gradle.kts`, gradle wrapper) is not yet committed — bring it in alongside the first real model integration.
