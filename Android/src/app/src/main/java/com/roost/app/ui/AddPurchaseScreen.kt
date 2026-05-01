package com.roost.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roost.app.data.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseScreen(
    viewModel: AddPurchaseViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.acknowledgeSaved()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Back") }
        }

        Text(
            "Add purchase",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        OutlinedButton(
            onClick = { viewModel.onScanReceipt() },
            enabled = !state.scanning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.scanning) "Scanning…" else "📷 Scan receipt (mock)")
        }

        OutlinedTextField(
            value = state.merchant,
            onValueChange = viewModel::onMerchantChange,
            label = { Text("Merchant") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = { Text("Amount") },
            leadingIcon = { Text("$") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        var menuOpen by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = menuOpen,
            onExpandedChange = { menuOpen = it },
        ) {
            OutlinedTextField(
                value = state.category.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                Category.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.displayName) },
                        onClick = {
                            viewModel.onCategoryChange(cat)
                            menuOpen = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::onNoteChange,
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.onSave() },
            enabled = !state.saving && state.merchant.isNotBlank() && (state.amount.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.saving) "Saving…" else "Save")
        }
    }
}
