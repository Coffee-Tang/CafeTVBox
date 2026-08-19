package dev.anilbeesetti.nextplayer.feature.live.screens.addsource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.NextSegmentedListItem
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun AddLiveSourceScreenRoute(
    onNavigateUp: () -> Unit,
    viewModel: AddLiveSourceViewModel,
) {
    val existingSource by viewModel.existingSource.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var isPrefilled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(existingSource) {
        val source = existingSource
        if (source != null && !isPrefilled) {
            name = source.name
            url = source.url
            isPrefilled = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.savedEvents.collect { onNavigateUp() }
    }

    AddLiveSourceScreen(
        isEdit = viewModel.isEdit,
        name = name,
        url = url,
        saveState = saveState,
        onNameChange = { name = it },
        onUrlChange = { url = it },
        onPresetSelected = { presetName, presetUrl ->
            name = presetName
            url = presetUrl
        },
        onNavigateUp = onNavigateUp,
        onSave = { viewModel.verifyAndSave(name, url) },
        onDismissError = viewModel::clearError,
    )
}

@Composable
internal fun AddLiveSourceScreen(
    isEdit: Boolean,
    name: String,
    url: String,
    saveState: SaveState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onPresetSelected: (name: String, url: String) -> Unit,
    onNavigateUp: () -> Unit,
    onSave: () -> Unit,
    onDismissError: () -> Unit,
) {
    val isVerifying = saveState is SaveState.Verifying
    val canSave = name.isNotBlank() && url.isNotBlank() && !isVerifying

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(
                    if (isEdit) R.string.edit_playlist_source else R.string.add_playlist_source,
                ),
                fontWeight = FontWeight.Bold,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp, modifier = Modifier.tvFocusRing()) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                enabled = !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusRing(shape = RoundedCornerShape(8.dp)),
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text(stringResource(R.string.playlist_url)) },
                placeholder = { Text("https://example.com/tv.m3u") },
                singleLine = true,
                enabled = !isVerifying,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusRing(shape = RoundedCornerShape(8.dp)),
            )
            Text(
                text = stringResource(R.string.playlist_url_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!isEdit) {
                PresetSection(enabled = !isVerifying, onPresetSelected = onPresetSelected)
            }
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusRing(shape = RoundedCornerShape(20.dp)),
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.verifying_playlist))
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    (saveState as? SaveState.Error)?.let { error ->
        NextDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.playlist_source_error)) },
            content = {
                Text(error.message ?: stringResource(R.string.playlist_source_error_description))
            },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PresetSection(
    enabled: Boolean,
    onPresetSelected: (name: String, url: String) -> Unit,
) {
    Text(
        text = stringResource(R.string.preset_sources),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
    livePresets.forEachIndexed { index, preset ->
        val presetName = stringResource(preset.nameRes)
        NextSegmentedListItem(
            enabled = enabled,
            isFirstItem = index == 0,
            isLastItem = index == livePresets.lastIndex,
            onClick = { onPresetSelected(presetName, preset.url) },
            leadingContent = {
                Icon(
                    imageVector = NextIcons.Live,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
            content = {
                Text(text = presetName, style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Text(
                    text = stringResource(preset.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}
