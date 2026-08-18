package com.ouagadousoft.filerescuelibre.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ouagadousoft.filerescuelibre.viewmodel.ScanUiState
import com.ouagadousoft.filerescuelibre.viewmodel.ScanViewModel

@Composable
fun ScanProgressScreen(
    viewModel: ScanViewModel,
    onScanCompleted: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ScanUiState.Completed) {
            onScanCompleted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = uiState) {
            is ScanUiState.Error -> {
                Text(
                    text = "Le scan a échoué : ${state.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                val scanning = state as? ScanUiState.Scanning
                val fraction = scanning?.progressFraction
                if (fraction != null) {
                    LinearProgressIndicator(progress = { fraction })
                } else {
                    CircularProgressIndicator()
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Recherche en cours…", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${scanning?.foundCount ?: 0} fichier(s) trouvé(s)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (fraction != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(fraction * 100).toInt()} %",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
