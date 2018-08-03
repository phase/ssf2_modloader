import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.DeflaterInputStream
import java.util.zip.InflaterInputStream
import javax.swing.*

fun main(args: Array<String>) {
    println("ssf2 modloader for SSF2 v1.1.0.1 beta\n\tby phase\n")

    if (args.isEmpty()) {
//        println("java -jar modloader.jar [compress|decompress]")
        try {
            UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // gui
        val frame = JFrame("phase's ssf2 modloader")
        val container = JPanel()

        val instructions = JTextPane()
        instructions.text = "phase's ssf2 modloader\n" +
                "\nmake sure this jar file is placed within" +
                "\nthe same directory as your SSF2 installation." +
                "\n\nSSF2/" +
                "\n  - data/" +
                "\n  - SSF2.swf" +
                "\n  - SSF2.exe" +
                "\n  - modloader.jar" +
                "\n\nDecompress will convert all of the" +
                "\n  `ssf` files in data/ to `swf`" +
                "\n\nCompress will convert all of the" +
                "\n  `swf` files in data/ to `ssf`"
        instructions.isEditable = false

        val decompressButton = JButton("Decompress")
        decompressButton.addActionListener {
            val dataFolder = File("data")
            if (dataFolder.exists() && dataFolder.isDirectory) {
                dataFolder.listFiles().forEach {
                    if (it.isFile && it.extension == "ssf") {
                        decompress(it)
                    }
                }
            }
            JOptionPane.showMessageDialog(container, "Decompression Complete", "phase's ssf2 modloader", JOptionPane.INFORMATION_MESSAGE)
        }

        val compressButton = JButton("Compress")
        compressButton.addActionListener {
            val dataFolder = File("data")
            if (dataFolder.exists() && dataFolder.isDirectory) {
                dataFolder.listFiles().forEach {
                    if (it.isFile && it.extension == "swf") {
                        compress(it)
                    }
                }
            }
            JOptionPane.showMessageDialog(container, "Compression Complete", "phase's ssf2 modloader", JOptionPane.INFORMATION_MESSAGE)
        }

        container.add(instructions)
        container.add(decompressButton)
        container.add(compressButton)

        container.layout = BoxLayout(container, BoxLayout.Y_AXIS)
        frame.add(container)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isResizable = false
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true

    } else if (args[0] == "decompress" || args[0] == "d") {
        val dataFolder = File("data")
        if (dataFolder.exists()) {
            dataFolder.listFiles().forEach {
                if (it.isFile && it.extension == "ssf") {
                    decompress(it)
                }
            }
        }
    } else if (args[0] == "compress" || args[0] == "c") {
        val dataFolder = File("data")
        if (dataFolder.exists()) {
            dataFolder.listFiles().forEach {
                if (it.isFile && it.extension == "swf") {
                    compress(it)
                }
            }
        }
    }
}

fun decompress(file: File) {
    val datBytes = file.readBytes()
    val stream = ByteArrayInputStream(datBytes)
    val zlibStream = InflaterInputStream(stream)

    val datDecompressed = zlibStream.readBytes()
    zlibStream.close()
    stream.close()

    var maybeOffset = -1
    var offset = -1
    val buffer = mutableListOf<Byte>()

    for ((index, byte) in datDecompressed.withIndex()) {

        if (byte == 0x46.toByte()) { // F
            buffer.add(byte)
            maybeOffset = index
        } else if (byte == 0x57.toByte() && buffer.size == 1) { // W
            buffer.add(byte)
        } else if (byte == 0x53.toByte() && buffer.size == 2) { // S
            buffer.add(byte)
            offset = maybeOffset
            break
        }
    }

    if (offset == -1) {
        println("ERROR(${file.name}): Couldn't find FWS header in decompressed data file")
        return
    }

    val header = datDecompressed.toList().subList(0, offset)
    println("${file.name} Header size ${header.size}")
    val swf = datDecompressed.toList().subList(offset, datDecompressed.size)
    println("${file.name} SWF size ${swf.size}")

    file.withExtension("swf").writeBytes(swf.toByteArray())
}

fun compress(file: File) {
    val swf = file.readBytes()
    println("${file.name} SWF size ${swf.size}")
    val combined = ByteBuffer.allocate(4).putInt(swf.size).array().toMutableList()
    combined.addAll(listOf(0, 0, 0, 0))
    combined.addAll(swf.toList())
    println("${file.name} Combined size ${combined.size}")

    val stream = ByteArrayInputStream(combined.toByteArray())
    val zlibStream = DeflaterInputStream(stream)
    val bytes = zlibStream.readBytes()
    zlibStream.close()
    stream.close()

    val datFile = file.withExtension("ssf")
    if (datFile.exists()) {
//        datFile.renameTo(datFile.withExtension("ssf.bak"))
        file.withExtension("ssf").writeBytes(bytes)
    }
}

fun File.withExtension(extension: String): File {
    return File(this.absolutePath.substringBeforeLast(".") + ".$extension")
}
