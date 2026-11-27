package ir.miladmedia.fdplayer.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

object MicAmplitudeMonitor {

    fun observeNormalizedAmplitude(context: Context): Flow<Float> = flow {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            emit(0f)
            return@flow
        }

        val sampleRate = 16000
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize <= 0) {
            emit(0f)
            return@flow
        }

        var recorder: AudioRecord? = null
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                emit(0f)
                return@flow
            }
            recorder.startRecording()
            val buffer = ShortArray(minBufferSize / 2)

            while (currentCoroutineContext().isActive) {
                val readCount = recorder.read(buffer, 0, buffer.size)
                if (readCount > 0) {
                    var sumSquares = 0.0
                    for (i in 0 until readCount) {
                        val sample = buffer[i].toDouble()
                        sumSquares += sample * sample
                    }
                    val rms = sqrt(sumSquares / readCount)
                    val normalized = (rms / 6500.0).toFloat().coerceIn(0f, 1f)
                    emit(normalized)
                }
                delay(60L)
            }
        } catch (_: Exception) {
            emit(0f)
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {
            }
        }
    }.flowOn(Dispatchers.IO)
}
