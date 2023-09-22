
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.jvm.AudioPlayer
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import classes.CButton
import classes.CSlider
import demos.classes.Animation
import kotlinx.coroutines.DelicateCoroutinesApi
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.noise.random
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.IntVector2
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.scale
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.findShapes
import org.openrndr.svg.loadSVG
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.math.sqrt

@Volatile
var isRunning = true


@OptIn(DelicateCoroutinesApi::class)
fun main() = application {
    configure {
        width = 608
        height = 342
        hideWindowDecorations = true
        windowAlwaysOnTop = true
        position = IntVector2(1285,110)
        windowTransparent = true
        multisample = WindowMultisample.SampleCount(4)
    }
    oliveProgram {
// MOUSE STUFF //////
        var mouseClick = false
        var mouseState = "up"
        mouse.dragged.listen { mouseState = "drag" }
        mouse.exited.listen { mouseState = "up" }
        mouse.buttonUp.listen { mouseState = "up"; mouseClick = true }
        mouse.buttonDown.listen { mouseState = "down" }
        mouse.moved.listen { mouseState = "move" }
// END //////////////
        val columnCount = 3
        val rowCount = 3
        val marginX = 10.0
        val marginY = 10.0
        val gutterX = 3.0
        val gutterY = 3.0
        var grid = drawer.bounds.grid(columnCount, rowCount, marginX, marginY, gutterX, gutterY)
        val flatGrid = grid.flatten()

        val incremCheck = onceObj()
        var palette = listOf(ColorRGBa.fromHex(0xF1934B), ColorRGBa.fromHex(0x0E8847), ColorRGBa.fromHex(0xD73E1C), ColorRGBa.fromHex(0xF4ECDF), ColorRGBa.fromHex(0x552F20))
        val white = ColorRGBa.WHITE
        val black = ColorRGBa.BLACK
        val animation = Animation()
        val loopDelay = 3.0
        val message = "hello"
        animation.loadFromJson(File("data/keyframes/keyframes-0.json"))
        val svgA = loadSVG(File("data/fonts/a.svg"))
        val firstShape = svgA.root.findShapes()[0]
        val firstContour = firstShape.shape.contours[0]

        val image = loadImage("data/images/cheeta.jpg")
        val scale: DoubleArray = typeScale(3, 100.0, 3)
        val typeFace: Pair<List<FontMap>, List<FontImageMap>> = defaultTypeSetup(scale, listOf("reg", "reg", "bold"))
        val animArr = mutableListOf<Animation>()
        val randNums = mutableListOf<Double>()
        val charArr = message.toCharArray()
        charArr.forEach { e ->
            animArr.add(Animation())
            randNums.add(random(0.0, 1.0))
        }
        animArr.forEach { a ->
            a.loadFromJson(File("data/keyframes/keyframes-0.json"))
        }
        val globalSpeed = 0.01



//        val stSR = 44100.0f
        val stSR = 48000.0f

        val format = AudioFormat(
            48000.0f, // Sample Rate
            16,       // Sample Size In Bits
            2,        // Channels
            true,     // Signed
            false     // Little Endian
        )

        val tarsosFormat = createTarsosFormat(format)
        val player = AudioPlayer(format)
        val dataLineInfo = DataLine.Info(TargetDataLine::class.java, format)
        val line = getTargetDataLine( dataLineInfo )
        val audioByteBuffer = ByteArray(line!!.bufferSize)
        animation.loadFromJson(File("data/keyframes/keyframes-0.json"))

        val myOnsetHandler = { time: Double, salience: Double ->
            println("Onset detected at time: $time with salience: $salience")
        }

        val detectors = listOf(
            PercussionOnsetDetector(48000f, 1024, myOnsetHandler, 0.05, 0.05),
            PercussionOnsetDetector(48000f, 512, myOnsetHandler, 0.1, 0.1),
            PercussionOnsetDetector(48000f, 2048, myOnsetHandler, 0.02, 0.02),
            PercussionOnsetDetector(48000f, 1024, myOnsetHandler, 0.2, 0.1),
            PercussionOnsetDetector(44100f, 1024, myOnsetHandler, 0.05, 0.05),
            PercussionOnsetDetector(48000f, 256, myOnsetHandler, 0.2, 0.2),
            PercussionOnsetDetector(48000f, 4096, myOnsetHandler, 0.01, 0.01),
            PercussionOnsetDetector(44100f, 512, myOnsetHandler, 0.1, 0.2),
            PercussionOnsetDetector(48000f, 1024, myOnsetHandler, 0.3, 0.05),
            PercussionOnsetDetector(48000f, 768, myOnsetHandler, 0.1, 0.05)
        )



//        myOnsetHandler.handleOnset(0.0, 0.0)


        val percDetect = detectors[0]

        line.open(format)
        line.start()
        val stream = AudioInputStream(line)

        Thread {
            while(isRunning) {
                while (true) {
                    val bytesRead = stream.read(audioByteBuffer)
                    if (bytesRead > 0) {
                        val audioEvent = AudioEvent( tarsosFormat )
                        audioEvent.floatBuffer = FloatArray(
                            bytesRead / 2
                        ) { i ->
                            (audioByteBuffer[i * 2].toInt() shl 8 or (audioByteBuffer[i * 2 + 1].toInt() and 0xFF)) / 32768.0f
                        }

//                        println("Sending buffer to detector: ${audioEvent.floatBuffer.take(5)}")
                        percDetect.process( audioEvent )
//                        println("Detector has processed the buffer.")

                        val rms = sqrt(
                            audioByteBuffer.map { it * it }.average()
                        )
                        if (rms < 0.01) {
                            println("Captured silence")
                        } else {
//                            println("Captured audio")
                        }
                    }
                }
            }
        }.start()
        println("doign styuff")



        extend {
            animArr.forEachIndexed { i, a ->
                a((randNums[i] * 0.3 + frameCount * globalSpeed) % loopDelay)
            }
            drawer.clear(ColorRGBa.TRANSPARENT)
            drawer.circle(drawer.bounds.center, 10.0)
            drawer.stroke = white

            // THIS NEEDS TO STAY AT THE END //
            if (mouseClick) mouseClick = false
            // END END ////////////////////////
        }
    }
}