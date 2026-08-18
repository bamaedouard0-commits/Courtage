package com.ouagadousoft.filerescuelibre.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ouagadousoft.filerescuelibre.R
import com.ouagadousoft.filerescuelibre.data.thumbnail.ThumbnailLoader
import com.ouagadousoft.filerescuelibre.domain.model.FileCategory
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ReliabilityLevel
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

    var categoryFilter by remember { mutableStateOf<FileCategory?>(null) }
    var reliabilityFilter by remember { mutableStateOf<ReliabilityLevel?>(null) }

    val countsByCategory = remember(results) { results.groupingBy { it.category }.eachCount() }
    val countsByReliability = remember(results) { results.groupingBy { it.reliability }.eachCount() }

    val filteredResults = remember(results, categoryFilter, reliabilityFilter) {
        results.filter { file ->
            (categoryFilter == null || file.category == categoryFilter) &&
                (reliabilityFilter == null || file.reliability == reliabilityFilter)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = resultsHeaderText(filteredResults.size, results.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )

        if (countsByCategory.size > 1) {
            FilterChipRow(
                totalCount = results.size,
                options = listOf(
                    FileCategory.IMAGE to stringResource(R.string.filter_photos),
                    FileCategory.VIDEO to stringResource(R.string.filter_videos),
                    FileCategory.AUDIO to stringResource(R.string.filter_audio),
                    FileCategory.DOCUMENT to stringResource(R.string.filter_documents),
                ),
                counts = countsByCategory,
                selected = categoryFilter,
                onSelectedChange = { categoryFilter = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if ((countsByReliability[ReliabilityLevel.PARTIAL] ?: 0) > 0) {
            FilterChipRow(
                totalCount = results.size,
                options = listOf(
                    ReliabilityLevel.INTACT to stringResource(R.string.filter_intact),
                    ReliabilityLevel.PARTIAL to stringResource(R.string.filter_partial),
                ),
                counts = countsByReliability,
                selected = reliabilityFilter,
                onSelectedChange = { reliabilityFilter = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredResults.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (results.isEmpty()) {
                        stringResource(R.string.results_empty_quick)
                    } else {
                        stringResource(R.string.results_empty_filtered)
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(filteredResults, key = { it.path }) { file ->
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
            Text(stringResource(R.string.results_new_scan))
        }
    }
}

@Composable
private fun resultsHeaderText(filteredCount: Int, totalCount: Int): String =
    if (filteredCount == totalCount) {
        stringResource(R.string.results_count, totalCount)
    } else {
        stringResource(R.string.results_count_filtered, filteredCount, totalCount)
    }

/** Ligne de puces à sélection unique ("Toutes" + une option par entrée de [options] présente dans [counts]). */
@Composable
private fun <T> FilterChipRow(
    totalCount: Int,
    options: List<Pair<T, String>>,
    counts: Map<T, Int>,
    selected: T?,
    onSelectedChange: (T?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelectedChange(null) },
            label = { Text(stringResource(R.string.filter_all_count, totalCount)) },
        )
        options.forEach { (value, label) ->
            val count = counts[value] ?: 0
            if (count > 0) {
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelectedChange(if (selected == value) null else value) },
                    label = { Text(stringResource(R.string.filter_option_count, label, count)) },
                )
            }
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
            Thumbnail(file = file)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                val partialSuffix = if (file.reliability == ReliabilityLevel.PARTIAL) {
                    " · " + stringResource(R.string.reliability_partial)
                } else {
                    ""
                }
                Text(
                    text = "${file.category.label()} · ${formatSize(file.sizeBytes)}$partialSuffix",
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
private fun Thumbnail(file: RecoverableFile) {
    var bitmap by remember(file.path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.path) {
        bitmap = ThumbnailLoader.load(file)
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (file.category == FileCategory.VIDEO) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                )
            }
        } else {
            Icon(
                imageVector = when (file.category) {
                    FileCategory.IMAGE -> Icons.Filled.Photo
                    FileCategory.VIDEO -> Icons.Filled.Videocam
                    FileCategory.AUDIO -> Icons.Filled.AudioFile
                    FileCategory.DOCUMENT -> Icons.Filled.Description
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecoveryStatusLabel(status: RecoveryStatus?) {
    when (status) {
        is RecoveryStatus.Success -> Text(
            text = stringResource(R.string.recovery_saved, status.savedPath),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        is RecoveryStatus.Error -> Text(
            text = stringResource(R.string.recovery_failed, status.message),
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
            contentDescription = stringResource(R.string.recovery_recovered_description),
            tint = MaterialTheme.colorScheme.primary,
        )
        is RecoveryStatus.Error -> IconButton(onClick = onRecover) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.recovery_retry_description),
            )
        }
        null -> IconButton(onClick = onRecover) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = stringResource(R.string.recovery_recover_description),
            )
        }
    }
}

@Composable
private fun FileCategory.label(): String = when (this) {
    FileCategory.IMAGE -> stringResource(R.string.category_photo)
    FileCategory.VIDEO -> stringResource(R.string.category_video)
    FileCategory.AUDIO -> stringResource(R.string.category_audio)
    FileCategory.DOCUMENT -> stringResource(R.string.category_document)
}

@Composable
private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> stringResource(R.string.size_megabytes, mb)
        kb >= 1 -> stringResource(R.string.size_kilobytes, kb)
        else -> stringResource(R.string.size_bytes, bytes)
    }
}
