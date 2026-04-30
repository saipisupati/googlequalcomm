package com.pawai.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Main scan screen: live camera, FastVLM bowl detection, Gemma assessment card.
 *
 * State machine:
 *   Idle -> Scanning (FastVLM) -> Confirming (editable chips) -> Assessing (Gemma stream) -> Logged
 */
@Composable
fun PawAIScreen(viewModel: PawAIViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // TODO: CameraX preview goes here.
        Box(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Camera preview placeholder", style = MaterialTheme.typography.bodyMedium)
        }

        when (val s = state) {
            is PawAIUiState.Idle -> {
                Button(onClick = { viewModel.startScan() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan Bowl")
                }
            }
            is PawAIUiState.Scanning -> Text("Identifying food…")
            is PawAIUiState.Confirming -> {
                Text("Detected: ${s.detectedFood}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.confirmAndAssess() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Confirm")
                }
            }
            is PawAIUiState.Assessing -> AssessmentCard(streamingText = s.streamingText)
            is PawAIUiState.Logged -> {
                AssessmentCard(streamingText = s.finalAssessment)
                Button(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan again")
                }
            }
        }
    }
}

@Composable
private fun AssessmentCard(streamingText: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = streamingText,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
