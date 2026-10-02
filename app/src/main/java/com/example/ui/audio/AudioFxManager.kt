package com.example.ui.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.example.data.model.EqualizerPreset

/**
 * Manages native Android AudioFx (Equalizer, BassBoost, Virtualizer) on the global audio session
 * in coordination with WebAudio parametric DSP in the playback engine.
 */
object AudioFxManager {
    private const val TAG = "AudioFxManager"

    private var nativeEqualizer: Equalizer? = null
    private var nativeBassBoost: BassBoost? = null
    private var nativeVirtualizer: Virtualizer? = null

    fun initAudioEffects(audioSessionId: Int = 0) {
        try {
            release()
            nativeEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            nativeBassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            nativeVirtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
            }
            Log.d(TAG, "Native AudioFx successfully initialized on session $audioSessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Native AudioFx not supported on this device/session: ${e.message}")
        }
    }

    /**
     * Applies band gains (-12dB to +12dB or normalized 0.0f to 1.0f where 0.5f is 0dB).
     */
    fun applyBands(
        band60: Float,
        band230: Float,
        band910: Float,
        band3k: Float,
        band14k: Float,
        bassBoost: Boolean,
        virtualizer: Boolean
    ) {
        try {
            val eq = nativeEqualizer ?: return
            if (!eq.enabled) eq.enabled = true

            val minLevel = eq.bandLevelRange?.get(0) ?: -1000 // millibels
            val maxLevel = eq.bandLevelRange?.get(1) ?: 1000
            val range = (maxLevel - minLevel).toFloat()

            val numBands = eq.numberOfBands.toInt()
            val gains = floatArrayOf(band60, band230, band910, band3k, band14k)

            for (i in 0 until minOf(numBands, gains.size)) {
                val normalized = gains[i].coerceIn(0.0f, 1.0f)
                val millibels = (minLevel + normalized * range).toInt().toShort()
                eq.setBandLevel(i.toShort(), millibels)
            }

            nativeBassBoost?.let { bb ->
                bb.enabled = bassBoost
                if (bassBoost) {
                    bb.setStrength(1000.toShort()) // max strength
                } else {
                    bb.setStrength(0.toShort())
                }
            }

            nativeVirtualizer?.let { virt ->
                virt.enabled = virtualizer
                if (virtualizer) {
                    virt.setStrength(1000.toShort())
                } else {
                    virt.setStrength(0.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying native AudioFx: ${e.message}")
        }
    }

    fun release() {
        try {
            nativeEqualizer?.release()
            nativeBassBoost?.release()
            nativeVirtualizer?.release()
        } catch (_: Exception) {}
        nativeEqualizer = null
        nativeBassBoost = null
        nativeVirtualizer = null
    }
}
