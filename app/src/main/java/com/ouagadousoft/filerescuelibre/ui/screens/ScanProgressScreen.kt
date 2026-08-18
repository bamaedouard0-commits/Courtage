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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ouagadousoft.filerescuelibre.R
import com.ouagadousoft.filerescuelibre.viewmodel.ScanUiState
import com.ouagadousoft.filerescuelibre.viewmodel.ScanViewModel

@Composable
fun ScanProgressScreen(
    viewModel: ScanViewModel,
    onScanCompleted: () -> Unit,
    onPause: () -> Unit = {},
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
                    text = stringResource(R.string.scan_progress_failed, state.message),
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
                Text(text = stringResource(R.string.scan_progress_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.scan_progress_found_count, scanning?.foundCount ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (fraction != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.scan_progress_percent, (fraction * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Reprenable ensuite (F9) : uniquement le scan approfondi mesure une fraction.
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = onPause) {
                        Text(stringResource(R.string.scan_progress_pause))
                    }
                }
            }
        }
    }
}
