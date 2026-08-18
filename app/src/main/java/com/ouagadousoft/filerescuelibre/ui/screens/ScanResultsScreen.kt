package com.ouagadousoft.filerescuelibre.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ouagadousoft.filerescuelibre.domain.model.FileCategory
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.viewmodel.RecoveryStatus
import com.ouagadousoft.filerescuelibre.viewmodel.ScanUiState
import com.ouagadousoft.filerescuelibre.viewmodel.ScanViewModel

@Composable
fun ScanResultsScreen(
    viewModel: ScanViewModel,
    onNewScan: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val recoveryStatuses by viewModel.recoveryStatuses.collectAsState()
    val results = (uiState as? ScanUiState.Completed)?.results.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${results.size} fichier(s) récupérable(s)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )

        if (results.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Aucun fichier trouvé lors du scan rapide. Essayez le scan approfondi.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(results, key = { it.path }) { file ->
                    RecoverableFileRow(
                        file = file,
                        status = recoveryStatuses[file.path],
                        onRecover = { viewModel.recoverFile(file) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Button(
            onClick = onNewScan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text("Nouveau scan")
        }
    }
}

@Composable
private fun RecoverableFileRow(
    file: RecoverableFile,
    status: RecoveryStatus?,
    onRecover: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${file.category.label()} · ${formatSize(file.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RecoveryStatusLabel(status)
            }
            Spacer(modifier = Modifier.width(8.dp))
            RecoveryAction(status = status, onRecover = onRecover)
        }
    }
}

@Composable
private fun RecoveryStatusLabel(status: RecoveryStatus?) {
    when (status) {
        is RecoveryStatus.Success -> Text(
            text = "Enregistré : ${status.savedPath}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        is RecoveryStatus.Error -> Text(
            text = "Échec : ${status.message}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        RecoveryStatus.InProgress, null -> Unit
    }
}

@Composable
private fun RecoveryAction(status: RecoveryStatus?, onRecover: () -> Unit) {
    when (status) {
        RecoveryStatus.InProgress -> CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.dp,
        )
        is RecoveryStatus.Success -> Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Récupéré",
            tint = MaterialTheme.colorScheme.primary,
        )
        is RecoveryStatus.Error -> IconButton(onClick = onRecover) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Réessayer")
        }
        null -> IconButton(onClick = onRecover) {
            Icon(imageVector = Icons.Filled.Download, contentDescription = "Récupérer")
        }
    }
}

private fun FileCategory.label(): String = when (this) {
    FileCategory.IMAGE -> "Photo"
    FileCategory.VIDEO -> "Vidéo"
}

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> "%.1f Mo".format(mb)
        kb >= 1 -> "%.0f Ko".format(kb)
        else -> "$bytes o"
    }
}
