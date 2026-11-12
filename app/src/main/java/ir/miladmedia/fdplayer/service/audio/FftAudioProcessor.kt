package ir.miladmedia.fdplayer.service.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Latest smoothed band levels (0..1) published by [FftAudioProcessor]. Read by the UI. */
object AudioLevels {
    const val BANDS = 32
    val bands = MutableStateFlow(FloatArray(BANDS))

    fun reset() {
        bands.value = FloatArray(BANDS)
    }
}

/**
 * Pass-through processor: mono-mixes 16-bit or float PCM, runs a Hann-windowed
 * 1024-point FFT, groups bins into log-spaced bands and publishes them to [AudioLevels].
 */
class FftAudioProcessor : BaseAudioProcessor() {

    private val size = 1024
    private val bins = size / 2
    private val hann = FloatArray(size) { (0.5 - 0.5 * cos(2.0 * PI * it / (size - 1))).toFloat() }
    private val frame = FloatArray(size)
    private val re = FloatArray(size)
    private val im = FloatArray(size)
    private val smoothed = FloatArray(AudioLevels.BANDS)

    private var filled = 0
    private var channels = 1
    private var isFloat = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        channels = inputAudioFormat.channelCount
        isFloat = enc == C.ENCODING_PCM_FLOAT
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        analyze(inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN))
        out.put(inputBuffer)
        out.flip()
    }

    override fun onFlush() {
        filled = 0
    }

    override fun onReset() {
        filled = 0
        smoothed.fill(0f)
    }

    private fun analyze(view: ByteBuffer) {
        val bytesPerSample = if (isFloat) 4 else 2
        val frameBytes = channels * bytesPerSample
        while (view.remaining() >= frameBytes) {
            var sum = 0f
            for (c in 0 until channels) {
                sum += if (isFloat) view.getFloat() else view.getShort().toFloat() / 32768f
            }
            frame[filled++] = sum / channels
            if (filled == size) {
                process()
                filled = 0
            }
        }
    }

    private fun process() {
        for (i in 0 until size) {
            re[i] = frame[i] * hann[i]
            im[i] = 0f
        }
        fft(re, im)

        for (b in 0 until AudioLevels.BANDS) {
            val lo = max(1, bins.toDouble().pow(b / AudioLevels.BANDS.toDouble()).toInt())
            val hi = max(lo + 1, bins.toDouble().pow((b + 1) / AudioLevels.BANDS.toDouble()).toInt())
            var acc = 0.0
            var count = 0
            for (k in lo until minOf(hi, bins)) {
                acc += sqrt((re[k] * re[k] + im[k] * im[k]).toDouble())
                count++
            }
            val mag = if (count == 0) 0.0 else acc / count / bins
            val db = 20.0 * log10(mag + 1e-9)
            val norm = ((db + 60.0) / 60.0).toFloat().coerceIn(0f, 1f)
            smoothed[b] = max(norm, smoothed[b] * 0.85f)
        }
        AudioLevels.bands.value = smoothed.copyOf()
    }

    /** In-place iterative radix-2 Cooley-Tukey FFT. Length must be a power of two. */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang)
            val wIm = sin(ang)
            val half = len / 2
            var i = 0
            while (i < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (k in 0 until half) {
                    val p = i + k
                    val q = p + half
                    val vr = (re[q] * curRe - im[q] * curIm).toFloat()
                    val vi = (re[q] * curIm + im[q] * curRe).toFloat()
                    re[q] = re[p] - vr
                    im[q] = im[p] - vi
                    re[p] = re[p] + vr
                    im[p] = im[p] + vi
                    val nr = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nr
                }
                i += len
            }
            len = len shl 1
        }
    }
}
