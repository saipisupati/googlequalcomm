package com.google.ai.edge.gallery.customtasks.kinex

/**
 * Landmark indices from MediaPipe PoseLandmarker (33 keypoints).
 * See: https://developers.google.com/mediapipe/solutions/vision/pose_landmarker
 */
object PoseLandmark {
  const val NOSE = 0
  const val LEFT_SHOULDER = 11
  const val RIGHT_SHOULDER = 12
  const val LEFT_ELBOW = 13
  const val RIGHT_ELBOW = 14
  const val LEFT_WRIST = 15
  const val RIGHT_WRIST = 16
  const val LEFT_HIP = 23
  const val RIGHT_HIP = 24
  const val LEFT_KNEE = 25
  const val RIGHT_KNEE = 26
  const val LEFT_ANKLE = 27
  const val RIGHT_ANKLE = 28
}

/** Skeleton connections to draw as lines between landmarks. */
val SKELETON_CONNECTIONS = listOf(
  // Torso
  Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER),
  Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
  Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
  Pair(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),
  // Left arm
  Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
  Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
  // Right arm
  Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW),
  Pair(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
  // Left leg
  Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
  Pair(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
  // Right leg
  Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
  Pair(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
)

enum class ExerciseType(val displayName: String, val keywords: List<String>) {
  SQUAT("Squat", listOf("squat", "squatting", "squat position", "legs bent", "sitting position")),
  PUSHUP("Push-up", listOf("push-up", "pushup", "push up", "plank position", "arms extended floor")),
  UNKNOWN("Unknown", listOf()),
}

/** Ideal angle ranges for each exercise. */
data class AngleRange(val min: Float, val max: Float, val cue: String)

data class ExerciseFormCriteria(
  val exercise: ExerciseType,
  val kneeAngle: AngleRange?,          // hip-knee-ankle angle
  val hipAngle: AngleRange?,           // shoulder-hip-knee angle
  val elbowAngle: AngleRange?,         // shoulder-elbow-wrist angle
  val torsoLean: AngleRange?,          // vertical tilt of torso
  val description: String,
)

val EXERCISE_DATABASE = mapOf(
  ExerciseType.SQUAT to ExerciseFormCriteria(
    exercise = ExerciseType.SQUAT,
    kneeAngle = AngleRange(80f, 105f, "Bend knees to 90°"),
    hipAngle = AngleRange(70f, 110f, "Keep hips level with knees"),
    elbowAngle = null,
    torsoLean = AngleRange(160f, 180f, "Keep chest up, back straight"),
    description = "Feet shoulder-width apart. Knees track over toes. " +
      "Hip crease below parallel. Chest up. Weight in heels.",
  ),
  ExerciseType.PUSHUP to ExerciseFormCriteria(
    exercise = ExerciseType.PUSHUP,
    kneeAngle = null,
    hipAngle = AngleRange(165f, 180f, "Keep body in straight line"),
    elbowAngle = AngleRange(80f, 110f, "Lower chest to floor"),
    torsoLean = null,
    description = "Hands shoulder-width apart. Body in straight line from head to heels. " +
      "Elbows at 45° to body. Lower chest to floor.",
  ),
)

/** Text description of the exercise used for EmbeddingGemma retrieval. */
fun getExerciseContext(exercise: ExerciseType): String {
  val criteria = EXERCISE_DATABASE[exercise] ?: return "Unknown exercise."
  return buildString {
    appendLine("Exercise: ${exercise.displayName}")
    appendLine("Description: ${criteria.description}")
    criteria.kneeAngle?.let { appendLine("Knee angle: ${it.min}°–${it.max}°. Cue: ${it.cue}") }
    criteria.hipAngle?.let { appendLine("Hip angle: ${it.min}°–${it.max}°. Cue: ${it.cue}") }
    criteria.elbowAngle?.let { appendLine("Elbow angle: ${it.min}°–${it.max}°. Cue: ${it.cue}") }
    criteria.torsoLean?.let { appendLine("Torso: ${it.min}°–${it.max}°. Cue: ${it.cue}") }
  }
}
