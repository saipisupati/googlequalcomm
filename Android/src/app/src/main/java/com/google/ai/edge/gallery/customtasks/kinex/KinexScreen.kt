package com.google.ai.edge.gallery.customtasks.kinex

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.Executors

private const val TAG = "KinexScreen"

// Palette
private val BgDark = Color(0xFF070710)
private val GoodGreen = Color(0xFF00E676)
private val BadRed = Color(0xFFFF1744)
private val JointColor = Color(0xFFFFFFFF)
private val AccentViolet = Color(0xFF7C5CFC)
private val AccentCyan = Color(0xFF00E5FF)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextSecondary = Color(0xFF9090B0)
private val CardBg = Color(0xCC13131F)

@Composable
fun KinexScreen(bottomPadding: Dp = 0.dp) {
  val viewModel: KinexViewModel = hiltViewModel()
  val state by viewModel.uiState.collectAsState()

  Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
    if (state.isInitializing) {
      InitializingOverlay()
    } else if (state.initError.isNotEmpty()) {
      ErrorOverlay(state.initError)
    } else {
      // Camera + skeleton overlay
      CameraWithOverlay(
        poseResult = state.poseResult,
        formIssues = state.formIssues,
        onFrame = { viewModel.processFrame(it) },
      )

      // UI overlays
      Column(modifier = Modifier.fillMaxSize()) {
        // Top HUD
        TopHud(state)
        Spacer(Modifier.weight(1f))
        // Bottom coaching panel
        CoachingPanel(state, bottomPadding)
      }
    }
  }
}

// ─── Camera + skeleton ───────────────────────────────────────────────────────

@Composable
private fun CameraWithOverlay(
  poseResult: PoseLandmarkerResult?,
  formIssues: List<FormIssue>,
  onFrame: (Bitmap) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val executor = remember { Executors.newSingleThreadExecutor() }

  Box(modifier = Modifier.fillMaxSize()) {
    // Camera preview
    AndroidView(
      factory = { ctx ->
        val previewView = PreviewView(ctx)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
          val cameraProvider = cameraProviderFuture.get()
          val preview = Preview.Builder().build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
          val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
          imageAnalysis.setAnalyzer(executor) { imageProxy ->
            val bitmap = imageProxy.toBitmap()
            onFrame(bitmap)
            imageProxy.close()
          }
          try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
              lifecycleOwner,
              CameraSelector.DEFAULT_BACK_CAMERA,
              preview,
              imageAnalysis,
            )
          } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed", e)
          }
        }, ContextCompat.getMainExecutor(ctx))
        previewView
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Skeleton overlay
    poseResult?.let { result ->
      if (result.landmarks().isNotEmpty()) {
        SkeletonOverlay(
          landmarks = result.landmarks()[0],
          formIssues = formIssues,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

@Composable
private fun SkeletonOverlay(
  landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
  formIssues: List<FormIssue>,
  modifier: Modifier = Modifier,
) {
  val badJoints = formIssues.map { it.joint.lowercase() }.toSet()

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Draw skeleton connections
    SKELETON_CONNECTIONS.forEach { (startIdx, endIdx) ->
      if (startIdx < landmarks.size && endIdx < landmarks.size) {
        val start = landmarks[startIdx]
        val end = landmarks[endIdx]
        val isProblematic = isBadConnection(startIdx, endIdx, badJoints)
        drawLine(
          color = if (isProblematic) BadRed else GoodGreen,
          start = Offset(start.x() * w, start.y() * h),
          end = Offset(end.x() * w, end.y() * h),
          strokeWidth = if (isProblematic) 6f else 4f,
        )
      }
    }

    // Draw joint dots
    landmarks.forEachIndexed { idx, landmark ->
      drawCircle(
        color = JointColor,
        radius = 6f,
        center = Offset(landmark.x() * w, landmark.y() * h),
      )
    }
  }
}

private fun isBadConnection(startIdx: Int, endIdx: Int, badJoints: Set<String>): Boolean {
  val kneeIndices = setOf(PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE)
  val hipIndices = setOf(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
  val elbowIndices = setOf(PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW)
  return when {
    badJoints.contains("knee") && (startIdx in kneeIndices || endIdx in kneeIndices) -> true
    badJoints.contains("hip") && (startIdx in hipIndices || endIdx in hipIndices) -> true
    badJoints.contains("elbow") && (startIdx in elbowIndices || endIdx in elbowIndices) -> true
    else -> false
  }
}

// ─── Top HUD ─────────────────────────────────────────────────────────────────

@Composable
private fun TopHud(state: KinexUiState) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xBB000010))
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      // Exercise badge
      Card(
        colors = CardDefaults.cardColors(containerColor = AccentViolet),
        shape = RoundedCornerShape(8.dp),
      ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.FitnessCenter, null, tint = Color.White, modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(6.dp))
          Text(
            state.detectedExercise.displayName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Spacer(Modifier.width(10.dp))

      // Form status
      val hasIssues = state.formIssues.isNotEmpty()
      Card(
        colors = CardDefaults.cardColors(
          containerColor = if (hasIssues) BadRed.copy(alpha = 0.85f) else GoodGreen.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(8.dp),
      ) {
        Text(
          if (hasIssues) "⚠ Fix Form" else "✓ Good Form",
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }

      Spacer(Modifier.width(10.dp))

      // Rep Count
      Card(
        colors = CardDefaults.cardColors(containerColor = AccentViolet.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
      ) {
        Text(
          "Reps: ${state.repCount}",
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }

      Spacer(Modifier.weight(1f))

      // FPS + latency
      Column(horizontalAlignment = Alignment.End) {
        Text(
          "${state.fps.toInt()} fps",
          color = AccentCyan,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
        )
        state.latencyMs["Pose"]?.let {
          Text("Pose: ${it}ms", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
      }
    }
  }
}

// ─── Coaching panel ──────────────────────────────────────────────────────────

@Composable
private fun CoachingPanel(state: KinexUiState, bottomPadding: Dp) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CardBg)
      .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp + bottomPadding)
  ) {
    // Coaching text
    if (state.coachingText.isNotBlank() || state.isGeneratingCoach) {
      Row(verticalAlignment = Alignment.Top) {
        Text("🤖", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column {
          Text(
            "KINEX COACH",
            color = AccentViolet,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
          )
          if (state.isGeneratingCoach && state.coachingText.isBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(
                color = AccentViolet,
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
              )
              Spacer(Modifier.width(6.dp))
              Text("Analyzing form…", color = TextSecondary, fontSize = 13.sp)
            }
          } else {
            Text(
              state.coachingText,
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Medium,
              lineHeight = 22.sp,
            )
          }
        }
      }
    } else {
      Text(
        "Get into position — Kinex will start coaching when form issues are detected.",
        color = TextSecondary,
        fontSize = 13.sp,
      )
    }

    // Form issue chips
    if (state.formIssues.isNotEmpty()) {
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        state.formIssues.forEach { issue ->
          Card(
            colors = CardDefaults.cardColors(containerColor = BadRed.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(20.dp),
          ) {
            Text(
              "${issue.joint}: ${issue.angle.toInt()}°",
              color = BadRed,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }
    }

    // Model latency row
    if (state.latencyMs.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        state.latencyMs.entries.take(3).forEach { (label, ms) ->
          Text(
            "$label ${ms}ms",
            color = AccentCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

// ─── Utility overlays ────────────────────────────────────────────────────────

@Composable
private fun InitializingOverlay() {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Icon(Icons.Outlined.FitnessCenter, null, tint = AccentViolet, modifier = Modifier.size(48.dp))
      Text("KINEX", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
      CircularProgressIndicator(color = AccentViolet)
      Text("Loading models onto NPU…", color = TextSecondary, fontSize = 14.sp)
      Text(
        "MediaPipe Pose · FastVLM · Gemma 3n",
        color = AccentCyan,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}

@Composable
private fun ErrorOverlay(error: String) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Model Load Error", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      Spacer(Modifier.height(8.dp))
      Text(error, color = TextSecondary, fontSize = 13.sp)
      Spacer(Modifier.height(12.dp))
      Text(
        "Push models to /sdcard/Download/ via ADB",
        color = AccentCyan,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}
