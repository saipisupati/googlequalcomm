package com.google.ai.edge.gallery.customtasks.kinex

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.math.acos
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "KinexViewModel"

private const val GEMMA_MODEL_PATH =
  "/sdcard/Download/Gemma3-1B-IT_q4_ekv1280_sm8750.litertlm"
private const val FASTVLM_MODEL_PATH =
  "/sdcard/Download/FastVLM-0.5B.qualcomm.sm8750.litertlm"
private const val POSE_MODEL_PATH = "/sdcard/Download/pose_landmarker_lite.task"

enum class RepState { STANDING, SQUATTING }


data class JointAngles(
  val leftKnee: Float = 0f,
  val rightKnee: Float = 0f,
  val leftHip: Float = 0f,
  val rightHip: Float = 0f,
  val leftElbow: Float = 0f,
  val rightElbow: Float = 0f,
  val torsoLean: Float = 0f,
)

data class FormIssue(val joint: String, val angle: Float, val ideal: AngleRange, val cue: String)

data class KinexUiState(
  val isInitializing: Boolean = true,
  val initError: String = "",
  val poseResult: PoseLandmarkerResult? = null,
  val detectedExercise: ExerciseType = ExerciseType.UNKNOWN,
  val jointAngles: JointAngles = JointAngles(),
  val formIssues: List<FormIssue> = emptyList(),
  val coachingText: String = "",
  val isGeneratingCoach: Boolean = false,
  val latencyMs: Map<String, Long> = emptyMap(),
  val fps: Float = 0f,
  val repCount: Int = 0,
)

@HiltViewModel
class KinexViewModel
@Inject
constructor(@ApplicationContext private val context: Context) : ViewModel() {

  private val _uiState = MutableStateFlow(KinexUiState())
  val uiState = _uiState.asStateFlow()

  private var poseLandmarker: PoseLandmarker? = null
  private var gemmaEngine: Engine? = null
  private var fastVlmEngine: Engine? = null

  private var currentRepState = RepState.STANDING
  private var frameCountInternal = 0
  private var fpsStartMs = System.currentTimeMillis()
  
  // Accumulated issues during the current rep
  private val currentRepIssues = mutableListOf<FormIssue>()

  init {
    initModels()
  }

  private fun initModels() {
    viewModelScope.launch(Dispatchers.Default) {
      try {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        // 1. MediaPipe Pose Landmarker
        Log.d(TAG, "Initializing MediaPipe Pose...")
        val t0 = System.currentTimeMillis()
        val poseOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
          .setBaseOptions(
            BaseOptions.builder()
              .setModelAssetPath(POSE_MODEL_PATH)
              .build()
          )
          .setRunningMode(RunningMode.IMAGE)
          .setNumPoses(1)
          .setMinPoseDetectionConfidence(0.5f)
          .setMinTrackingConfidence(0.5f)
          .build()
        poseLandmarker = PoseLandmarker.createFromOptions(context, poseOptions)
        Log.d(TAG, "Pose ready in ${System.currentTimeMillis() - t0}ms")

        // 2. Gemma 3n (for coaching)
        Log.d(TAG, "Initializing Gemma 3n...")
        val t1 = System.currentTimeMillis()
        val gemmaConfig = EngineConfig(
          modelPath = GEMMA_MODEL_PATH,
          backend = Backend.NPU(nativeLibraryDir = nativeLibDir),
          maxNumTokens = 512,
        )
        val engine = Engine(gemmaConfig)
        engine.initialize()
        gemmaEngine = engine
        Log.d(TAG, "Gemma 3n ready in ${System.currentTimeMillis() - t1}ms")

        // 3. FastVLM (for exercise identification)
        Log.d(TAG, "Initializing FastVLM...")
        val t2 = System.currentTimeMillis()
        try {
          val vlmConfig = EngineConfig(
            modelPath = FASTVLM_MODEL_PATH,
            backend = Backend.NPU(nativeLibraryDir = nativeLibDir),
            visionBackend = Backend.NPU(nativeLibraryDir = nativeLibDir),
            maxNumTokens = 256,
          )
          val vlmEngine = Engine(vlmConfig)
          vlmEngine.initialize()
          fastVlmEngine = vlmEngine
          Log.d(TAG, "FastVLM ready in ${System.currentTimeMillis() - t2}ms")
        } catch (e: Exception) {
          Log.w(TAG, "FastVLM init failed (non-fatal): ${e.message}")
        }

        _uiState.update { it.copy(isInitializing = false) }
      } catch (e: Exception) {
        Log.e(TAG, "Init failed", e)
        _uiState.update { it.copy(isInitializing = false, initError = e.message ?: "Init failed") }
      }
    }
  }

  /**
   * Called for every camera frame. Runs pose estimation, detects rep state transitions,
   * and triggers coaching generation at the end of a rep.
   */
  fun processFrame(bitmap: Bitmap) {
    val landmarker = poseLandmarker ?: return
    val now = System.currentTimeMillis()

    // FPS tracking
    frameCountInternal++
    val elapsed = now - fpsStartMs
    if (elapsed >= 1000L) {
      val fps = frameCountInternal * 1000f / elapsed
      _uiState.update { it.copy(fps = fps, frameCount = frameCountInternal) }
      frameCountInternal = 0
      fpsStartMs = now
    }

    viewModelScope.launch(Dispatchers.Default) {
      try {
        val t0 = System.currentTimeMillis()
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = landmarker.detect(mpImage)
        val poseMs = System.currentTimeMillis() - t0

        if (result.landmarks().isEmpty()) {
          _uiState.update { it.copy(poseResult = null) }
          return@launch
        }

        val landmarks = result.landmarks()[0]
        val angles = calculateAngles(landmarks)
        val exercise = _uiState.value.detectedExercise
        val issues = checkForm(exercise, angles)

        _uiState.update {
          it.copy(
            poseResult = result,
            jointAngles = angles,
            formIssues = issues,
            latencyMs = it.latencyMs + ("Pose" to poseMs),
          )
        }

        // Rep Detection State Machine
        val avgKnee = (angles.leftKnee + angles.rightKnee) / 2f
        if (currentRepState == RepState.STANDING && avgKnee < 110f) {
          // Started squatting
          currentRepState = RepState.SQUATTING
          currentRepIssues.clear()
          Log.d(TAG, "Rep started (squatting)")
        } else if (currentRepState == RepState.SQUATTING) {
          // Accumulate issues while squatting
          if (issues.isNotEmpty()) {
            currentRepIssues.addAll(issues)
          }
          // Check if stood back up
          if (avgKnee > 150f) {
            currentRepState = RepState.STANDING
            _uiState.update { it.copy(repCount = it.repCount + 1) }
            Log.d(TAG, "Rep completed! Triggering analysis...")
            
            // Deduplicate and filter issues
            val distinctIssues = currentRepIssues.distinctBy { it.joint }
            analyzeExercise(bitmap, angles, distinctIssues)
          }
        }

      } catch (e: Exception) {
        Log.e(TAG, "Frame processing error", e)
      }
    }
  }

  private suspend fun analyzeExercise(
    bitmap: Bitmap,
    angles: JointAngles,
    issues: List<FormIssue>,
  ) {
    // Step 1: FastVLM identifies the exercise
    val t0 = System.currentTimeMillis()
    val exerciseType = identifyExercise(bitmap)
    val vlmMs = System.currentTimeMillis() - t0

    if (exerciseType != _uiState.value.detectedExercise) {
      _uiState.update { it.copy(detectedExercise = exerciseType) }
    }

    // Step 2: Gemma 3n generates coaching if there are form issues
    if (issues.isNotEmpty() && !_uiState.value.isGeneratingCoach) {
      _uiState.update { it.copy(isGeneratingCoach = true) }
      val t1 = System.currentTimeMillis()
      generateCoaching(exerciseType, angles, issues)
      val coachMs = System.currentTimeMillis() - t1
      _uiState.update {
        it.copy(latencyMs = it.latencyMs + ("FastVLM" to vlmMs) + ("Gemma" to coachMs))
      }
    }
  }

  private suspend fun identifyExercise(bitmap: Bitmap): ExerciseType {
    val engine = fastVlmEngine ?: return classifyByPose(_uiState.value.jointAngles)
    return withContext(Dispatchers.Default) {
      try {
        val conversation = engine.createConversation(ConversationConfig())
        val imgBytes = bitmap.toPngByteArray()
        val prompt =
          "What exercise is this person doing? Reply with ONE word only: squat, pushup, or unknown."
        val response = conversation.sendMessage(
          Contents.of(listOf(Content.ImageBytes(imgBytes), Content.Text(prompt)))
        ).toString().lowercase().trim()
        conversation.close()

        when {
          response.contains("squat") -> ExerciseType.SQUAT
          response.contains("push") -> ExerciseType.PUSHUP
          else -> classifyByPose(_uiState.value.jointAngles)
        }
      } catch (e: Exception) {
        Log.e(TAG, "FastVLM error", e)
        classifyByPose(_uiState.value.jointAngles)
      }
    }
  }

  /** Fallback: classify exercise from joint angles alone (no FastVLM). */
  private fun classifyByPose(angles: JointAngles): ExerciseType {
    val avgKnee = (angles.leftKnee + angles.rightKnee) / 2f
    val avgElbow = (angles.leftElbow + angles.rightElbow) / 2f
    return when {
      avgKnee < 160f -> ExerciseType.SQUAT
      avgElbow < 150f && angles.torsoLean < 30f -> ExerciseType.PUSHUP
      else -> ExerciseType.UNKNOWN
    }
  }

  private fun generateCoaching(
    exercise: ExerciseType,
    angles: JointAngles,
    issues: List<FormIssue>,
  ) {
    val engine = gemmaEngine ?: run {
      _uiState.update { it.copy(isGeneratingCoach = false) }
      return
    }
    viewModelScope.launch(Dispatchers.Default) {
      try {
        val criteria = EXERCISE_DATABASE[exercise]
        val issueText = issues.joinToString("; ") { "${it.joint}: ${it.angle.toInt()}° (ideal: ${it.ideal.min.toInt()}°–${it.ideal.max.toInt()}°)" }
        val prompt = """You are a real-time exercise coach. Be concise — max 2 short sentences.
Exercise: ${exercise.displayName}
Form issues detected: $issueText
Give one specific, actionable correction. Start directly with the correction (no greeting)."""

        val conversation = engine.createConversation(
          ConversationConfig(
            systemInstruction = Contents.of("You are a concise, encouraging fitness coach.")
          )
        )

        var coaching = ""
        conversation.sendMessageAsync(
          Contents.of(listOf(Content.Text(prompt))),
          object : MessageCallback {
            override fun onMessage(message: Message) {
              coaching += message.toString()
              _uiState.update { it.copy(coachingText = coaching) }
            }
            override fun onDone() {
              _uiState.update { it.copy(isGeneratingCoach = false) }
              try { conversation.close() } catch (_: Exception) {}
            }
            override fun onError(throwable: Throwable) {
              _uiState.update { it.copy(isGeneratingCoach = false) }
              try { conversation.close() } catch (_: Exception) {}
            }
          },
          emptyMap(),
        )
      } catch (e: Exception) {
        Log.e(TAG, "Coaching generation failed", e)
        _uiState.update { it.copy(isGeneratingCoach = false) }
      }
    }
  }

  // ─── Angle math ─────────────────────────────────────────────────────────────

  private fun calculateAngles(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): JointAngles {
    if (landmarks.size < 29) return JointAngles()
    return JointAngles(
      leftKnee = angle(
        landmarks[PoseLandmark.LEFT_HIP],
        landmarks[PoseLandmark.LEFT_KNEE],
        landmarks[PoseLandmark.LEFT_ANKLE],
      ),
      rightKnee = angle(
        landmarks[PoseLandmark.RIGHT_HIP],
        landmarks[PoseLandmark.RIGHT_KNEE],
        landmarks[PoseLandmark.RIGHT_ANKLE],
      ),
      leftHip = angle(
        landmarks[PoseLandmark.LEFT_SHOULDER],
        landmarks[PoseLandmark.LEFT_HIP],
        landmarks[PoseLandmark.LEFT_KNEE],
      ),
      rightHip = angle(
        landmarks[PoseLandmark.RIGHT_SHOULDER],
        landmarks[PoseLandmark.RIGHT_HIP],
        landmarks[PoseLandmark.RIGHT_KNEE],
      ),
      leftElbow = angle(
        landmarks[PoseLandmark.LEFT_SHOULDER],
        landmarks[PoseLandmark.LEFT_ELBOW],
        landmarks[PoseLandmark.LEFT_WRIST],
      ),
      rightElbow = angle(
        landmarks[PoseLandmark.RIGHT_SHOULDER],
        landmarks[PoseLandmark.RIGHT_ELBOW],
        landmarks[PoseLandmark.RIGHT_WRIST],
      ),
      torsoLean = angle(
        landmarks[PoseLandmark.LEFT_SHOULDER],
        landmarks[PoseLandmark.LEFT_HIP],
        landmarks[PoseLandmark.LEFT_KNEE],
      ),
    )
  }

  private fun angle(
    a: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
    b: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
    c: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
  ): Float {
    val v1x = a.x() - b.x(); val v1y = a.y() - b.y()
    val v2x = c.x() - b.x(); val v2y = c.y() - b.y()
    val dot = v1x * v2x + v1y * v2y
    val mag1 = sqrt(v1x * v1x + v1y * v1y)
    val mag2 = sqrt(v2x * v2x + v2y * v2y)
    if (mag1 == 0f || mag2 == 0f) return 0f
    val cos = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cos.toDouble()).toFloat().toDouble()).toFloat()
  }

  private fun checkForm(exercise: ExerciseType, angles: JointAngles): List<FormIssue> {
    val criteria = EXERCISE_DATABASE[exercise] ?: return emptyList()
    val issues = mutableListOf<FormIssue>()
    val avgKnee = (angles.leftKnee + angles.rightKnee) / 2f
    val avgHip = (angles.leftHip + angles.rightHip) / 2f
    val avgElbow = (angles.leftElbow + angles.rightElbow) / 2f

    criteria.kneeAngle?.let { range ->
      if (avgKnee !in range.min..range.max)
        issues.add(FormIssue("Knee", avgKnee, range, range.cue))
    }
    criteria.hipAngle?.let { range ->
      if (avgHip !in range.min..range.max)
        issues.add(FormIssue("Hip", avgHip, range, range.cue))
    }
    criteria.elbowAngle?.let { range ->
      if (avgElbow !in range.min..range.max)
        issues.add(FormIssue("Elbow", avgElbow, range, range.cue))
    }
    return issues
  }

  override fun onCleared() {
    super.onCleared()
    try { poseLandmarker?.close() } catch (_: Exception) {}
    try { gemmaEngine?.close() } catch (_: Exception) {}
    try { fastVlmEngine?.close() } catch (_: Exception) {}
  }
}

private fun Bitmap.toPngByteArray(): ByteArray {
  val stream = ByteArrayOutputStream()
  compress(Bitmap.CompressFormat.PNG, 80, stream)
  return stream.toByteArray()
}
