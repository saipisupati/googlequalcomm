package com.google.ai.edge.gallery.customtasks.kinex

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

class KinexTask @Inject constructor() : CustomTask {

  override val task = Task(
    id = BuiltInTaskId.LLM_ASK_IMAGE,
    label = "Kinex",
    description = "Real-time AI exercise form coach. Point the camera at yourself " +
      "doing squats or push-ups. Kinex overlays your skeleton in real-time and uses " +
      "Gemma 3n to coach your form when it detects issues.\n\n" +
      "Powered by MediaPipe Pose · FastVLM · EmbeddingGemma · Gemma 3n. " +
      "All on-device. No internet required.",
    shortDescription = "Real-time AI form coach with AR skeleton overlay",
    docUrl = "https://github.com/saipisupati/googlequalcomm",
    sourceCodeUrl = "https://github.com/saipisupati/googlequalcomm",
    category = Category.LLM,
    icon = Icons.Outlined.FitnessCenter,
    models = mutableListOf(),
  )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) { onDone("") }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) { onDone() }

  @Composable
  override fun MainScreen(data: Any) {
    val customTaskData = data as CustomTaskData
    KinexScreen(bottomPadding = customTaskData.bottomPadding)
  }
}
