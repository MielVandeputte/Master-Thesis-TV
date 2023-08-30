package com.masterproef.model

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

private const val FREQUENCY = 20000
private const val SAMPLE_RATE = 44100

class UltrasonicGenerator {

    private var countDown = -1

    /* If not playing yet, start playing 30 patterns
    *  If already playing, reset countdown to 30, extending it so multiple devices can be connected
    *  at once and it still works
    * */
    fun startPlaying() {
        if (countDown < 0) {
            countDown = 30
            playAudio()
        } else {
            countDown = 30
        }
    }

    // Prematurely stops playing and discards any data that hasn't played yet
    fun stopPlaying() {
        countDown = -1
        audioTrack.flush()
    }

    private val audioData: ShortArray = generateAudioSignal()

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(SAMPLE_RATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    private val bufferSizeInBytes = audioData.size * 2 // 2 bytes per sample (value in ShortArray) as defined in setEncoding above

    private val audioTrack: AudioTrack = AudioTrack(
        audioAttributes,
        audioFormat,
        bufferSizeInBytes,
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    )

    // Generate audio pattern: 1 second on, 1 second off
    private fun generateAudioSignal(): ShortArray {
        val angularFrequency = 2 * PI * FREQUENCY

        // Pattern is 2 seconds long, so 2 * SAMPLE_RATE samples are needed
        val length = 2 * SAMPLE_RATE
        val audioData = ShortArray(length)

        var enabled = false

        // For every sample
        for (i in audioData.indices) {

            // Switch if the sample-index is a multiple of SAMPLE_RATE, so switch every 44100 samples, or every second
            if (i % SAMPLE_RATE == 0) {
                enabled = !enabled
            }

            // Calculate the position in the sin-wave of a particular frequency for a specific sample
            val sample = (sin(angularFrequency * i / SAMPLE_RATE) * Short.MAX_VALUE * if (enabled) 1 else 0).toInt().toShort()
            audioData[i] = sample
        }

        return audioData
    }

    private fun playAudio() {
        audioTrack.play()

        Thread {
            while (countDown >= 0) {
                audioTrack.write(audioData, 0, audioData.size)
                countDown -= 1
            }
        }.start()
    }
}