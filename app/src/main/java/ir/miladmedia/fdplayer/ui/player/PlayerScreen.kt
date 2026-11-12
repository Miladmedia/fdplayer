package ir.miladmedia.fdplayer.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.compose.runtime.collectAsState
import ir.miladmedia.fdplayer.R
import ir.miladmedia.fdplayer.ui.common.formatDuration

@Composable
fun PlayerRoute(viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val bands by viewModel.bands.collectAsState()
    val driving by viewModel.driving.collectAsState()
    PlayerScreen(
        state = state,
        bands = bands,
        onTogglePlay = viewModel::togglePlay,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onSeek = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeat,
        driving = driving,
        onToggleDriving = viewModel::toggleDriving
    )
}

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    bands: FloatArray,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    driving: Boolean,
    onToggleDriving: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        FilterChip(
            selected = driving,
            onClick = onToggleDriving,
            label = { Text(stringResource(R.string.driving_mode)) },
            leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) }
        )
        if (!state.hasMedia) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.player_empty), textAlign = TextAlign.Center)
            }
            return@Column
        }

        Spacer(Modifier.height(8.dp))
        Visualizer(
            bands = bands,
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = state.artist,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        SeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeek = onSeek
        )

        PlayerControls(
            state = state,
            driving = driving,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onPrevious = onPrevious,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat
        )
    }
}

/** Playback controls; driving mode enlarges every button and its icon. */
@Composable
private fun PlayerControls(
    state: PlayerUiState,
    driving: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    val buttonSize = if (driving) 64.dp else 48.dp
    val iconSize = if (driving) 34.dp else 24.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                modifier = Modifier.size(iconSize),
                tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
                modifier = Modifier.size(iconSize)
            )
        }
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                modifier = Modifier.size(iconSize)
            )
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.next),
                modifier = Modifier.size(iconSize)
            )
        }
        IconButton(
            onClick = onCycleRepeat,
            modifier = Modifier.size(buttonSize)
        ) {
            val active = state.repeatMode != Player.REPEAT_MODE_OFF
            Icon(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = stringResource(R.string.repeat),
                modifier = Modifier.size(iconSize),
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val live = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val shown = if (dragging) dragFraction else live

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = shown.coerceIn(0f, 1f),
            onValueChange = {
                dragging = true
                dragFraction = it
            },
            onValueChangeFinished = {
                if (durationMs > 0) onSeek((dragFraction * durationMs).toLong())
                dragging = false
            }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionMs), style = MaterialTheme.typography.bodySmall)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Draws the FFT bands as rounded vertical bars. Band values are 0..1. */
@Composable
private fun Visualizer(bands: FloatArray, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val count = bands.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (count - 1)) / count
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        for (i in 0 until count) {
            val level = bands[i].coerceIn(0f, 1f)
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = radius
            )
            val h = (size.height * level).coerceAtLeast(barWidth)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = radius
            )
        }
    }
}
