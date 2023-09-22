

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.jvm.AudioPlayer
import javax.sound.sampled.*


fun createTarsosFormat(format: AudioFormat): TarsosDSPAudioFormat {
    val tarsosEncoding: TarsosDSPAudioFormat.Encoding = when (format.encoding) {
        AudioFormat.Encoding.PCM_SIGNED -> TarsosDSPAudioFormat.Encoding.PCM_SIGNED
        AudioFormat.Encoding.PCM_UNSIGNED -> TarsosDSPAudioFormat.Encoding.PCM_UNSIGNED
        else -> throw IllegalArgumentException("Unsupported encoding")
    }

    return TarsosDSPAudioFormat(
        tarsosEncoding,
        format.sampleRate,
        format.sampleSizeInBits,
        format.channels,
        format.frameSize,
        format.frameRate,
        format.isBigEndian
    )
}

fun getTargetDataLine(dataLineInfo: DataLine.Info): TargetDataLine? {
    val mixerInfos = AudioSystem.getMixerInfo()
    var line: TargetDataLine? = null

    for (info in mixerInfos) {
        val mixer = AudioSystem.getMixer(info)
        if (mixer.isLineSupported(dataLineInfo) && info.name.contains("BlackHole")) {
            line = mixer.getLine(dataLineInfo) as TargetDataLine
            break
        }
    }
    return line
}
