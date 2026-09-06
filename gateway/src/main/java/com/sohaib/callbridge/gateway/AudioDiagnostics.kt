package com.sohaib.callbridge.gateway

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import kotlin.math.abs

object AudioDiagnostics {
    private const val SAMPLE_RATE = 16000

    fun run(context: Context): String {
        val lines = mutableListOf<String>()
        lines += "Stage 3 Audio Diagnostic"
        lines += "Device: ${Build.MANUFACTURER} ${Build.MODEL}"
        lines += "Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        lines += "Root shell: ${rootStatus()}"
        lines += "RECORD_AUDIO: ${permission(context, Manifest.permission.RECORD_AUDIO)}"
        lines += "CAPTURE_AUDIO_OUTPUT: ${permission(context, "android.permission.CAPTURE_AUDIO_OUTPUT")}"
        lines += ""

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        lines += "Audio mode: ${audioModeName(am.mode)}"
        if (Build.VERSION.SDK_INT >= 23) {
            val inputs = am.getDevices(AudioManager.GET_DEVICES_INPUTS)
                .joinToString { "${it.productName}/${deviceTypeName(it.type)}" }
            val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .joinToString { "${it.productName}/${deviceTypeName(it.type)}" }
            lines += "Inputs: ${if (inputs.isBlank()) "none" else inputs}"
            lines += "Outputs: ${if (outputs.isBlank()) "none" else outputs}"
        }
        lines += ""

        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            lines += "Audio-source tests skipped: grant Microphone permission."
            return lines.joinToString("\n")
        }

        val sources = listOf(
            "MIC" to MediaRecorder.AudioSource.MIC,
            "VOICE_COMMUNICATION" to MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            "VOICE_RECOGNITION" to MediaRecorder.AudioSource.VOICE_RECOGNITION,
            "VOICE_CALL" to MediaRecorder.AudioSource.VOICE_CALL,
            "VOICE_UPLINK" to MediaRecorder.AudioSource.VOICE_UPLINK,
            "VOICE_DOWNLINK" to MediaRecorder.AudioSource.VOICE_DOWNLINK
        )

        lines += "Audio source probes (~0.5 s each):"
        for ((name, source) in sources) {
            lines += "$name: ${probe(source)}"
        }
        lines += ""
        lines += "Interpretation: VOICE_CALL/UPLINK/DOWNLINK normally require privileged system permission. A successful MIC or VOICE_COMMUNICATION probe only proves microphone access, not cellular downlink capture."
        return lines.joinToString("\n")
    }

    private fun probe(source: Int): String {
        var record: AudioRecord? = null
        return try {
            val min = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (min <= 0) return "UNSUPPORTED buffer=$min"
            record = AudioRecord(
                source,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(min * 2, 4096)
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) return "NOT_INITIALIZED"
            record.startRecording()
            val buf = ShortArray(1024)
            var maxAbs = 0
            var totalRead = 0
            val end = System.currentTimeMillis() + 500
            while (System.currentTimeMillis() < end) {
                val n = record.read(buf, 0, buf.size)
                if (n > 0) {
                    totalRead += n
                    for (i in 0 until n) maxAbs = maxOf(maxAbs, abs(buf[i].toInt()))
                } else if (n < 0) {
                    return "READ_ERROR $n"
                }
            }
            "OK samples=$totalRead peak=$maxAbs"
        } catch (se: SecurityException) {
            "BLOCKED ${se.message ?: "SecurityException"}"
        } catch (t: Throwable) {
            "ERROR ${t.javaClass.simpleName}: ${t.message ?: "unknown"}"
        } finally {
            try { record?.stop() } catch (_: Throwable) {}
            try { record?.release() } catch (_: Throwable) {}
        }
    }

    private fun permission(context: Context, name: String): String =
        if (context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"

    private fun rootStatus(): String = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val out = p.inputStream.bufferedReader().readText().trim()
        val code = p.waitFor()
        if (code == 0 && out.contains("uid=0")) "AVAILABLE ($out)" else "NOT_AVAILABLE"
    } catch (_: Throwable) {
        "NOT_AVAILABLE"
    }

    private fun audioModeName(mode: Int): String = when (mode) {
        AudioManager.MODE_NORMAL -> "NORMAL"
        AudioManager.MODE_RINGTONE -> "RINGTONE"
        AudioManager.MODE_IN_CALL -> "IN_CALL"
        AudioManager.MODE_IN_COMMUNICATION -> "IN_COMMUNICATION"
        else -> mode.toString()
    }

    private fun deviceTypeName(type: Int): String = when (type) {
        1 -> "EARPIECE"
        2 -> "SPEAKER"
        3 -> "WIRED_HEADSET"
        4 -> "WIRED_HEADPHONES"
        7 -> "BLUETOOTH_SCO"
        8 -> "BLUETOOTH_A2DP"
        11 -> "USB_DEVICE"
        12 -> "USB_ACCESSORY"
        15 -> "BUILTIN_MIC"
        18 -> "TELEPHONY"
        else -> "TYPE_$type"
    }
}
