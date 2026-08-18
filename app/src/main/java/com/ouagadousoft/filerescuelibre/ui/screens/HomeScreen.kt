package com.ouagadousoft.filerescuelibre.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ouagadousoft.filerescuelibre.R
import com.ouagadousoft.filerescuelibre.domain.model.ScanZone

@Composable
fun HomeScreen(
    onQuickScan: (ScanZone) -> Unit = {},
    onDeepScan: () -> Unit = {},
) {
    var selectedZone by remember { mutableStateOf(ScanZone.FULL_STORAGE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        ZoneSelector(
            selected = selectedZone,
            onSelect = { selectedZone = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))

        ScanOptionCard(
            title = stringResource(R.string.scan_quick),
            description = stringResource(R.string.scan_quick_desc),
            onClick = { onQuickScan(selectedZone) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        ScanOptionCard(
            title = stringResource(R.string.scan_deep),
            description = stringResource(R.string.scan_deep_desc),
            onClick = onDeepScan,
        )
    }
}

@Composable
private fun ZoneSelector(
    selected: ScanZone,
    onSelect: (ScanZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.zone_selector_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selected.displayLabel())
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                ScanZone.entries.forEach { zone ->
                    DropdownMenuItem(
                        text = { Text(zone.displayLabel()) },
                        onClick = {
                            onSelect(zone)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanZone.displayLabel(): String = when (this) {
    ScanZone.FULL_STORAGE -> stringResource(R.string.zone_full_storage)
    ScanZone.DCIM -> stringResource(R.string.zone_dcim)
    ScanZone.PICTURES -> stringResource(R.string.zone_pictures)
    ScanZone.MOVIES -> stringResource(R.string.zone_movies)
    ScanZone.DOWNLOAD -> stringResource(R.string.zone_download)
    ScanZone.WHATSAPP -> stringResource(R.string.zone_whatsapp)
}

@Composable
private fun ScanOptionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
