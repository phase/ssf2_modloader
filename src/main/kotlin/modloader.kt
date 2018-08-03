import java.io.File
import java.util.zip.InflaterInputStream
import java.io.ByteArrayInputStream
import java.util.zip.DeflaterInputStream

fun main(args: Array<String>) {
    println("ssf2 modloader for SSF2 v1.1.0.1 beta\n\tby phase\n")

    if (args.isEmpty()) {
        println("java -jar modloader.jar [compress|decompress]")
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
        println("Couldn't find FWS header in decompressed data file")
        return
    }

    val header = datDecompressed.toList().subList(0, offset)
    println("${file.name} Header size ${header.size}")
    val swf = datDecompressed.toList().subList(offset, datDecompressed.size)
    println("${file.name} SWF size ${swf.size}")

    file.withExtension("header").writeBytes(header.toByteArray())
    file.withExtension("swf").writeBytes(swf.toByteArray())
}

fun compress(file: File) {
    val swf = file.readBytes()
    println("${file.name} SWF size ${swf.size}")
    val header = file.withExtension("header").readBytes()
    println("${file.name} Header size ${header.size}")
    val combined = header.toMutableList()
    println("${file.name} Combined size ${combined.size}")
    combined.addAll(swf.toList())

    val stream = ByteArrayInputStream(combined.toByteArray())
    val zlibStream = DeflaterInputStream(stream)
    val bytes = zlibStream.readBytes()
    zlibStream.close()
    stream.close()

    val datFile = file.withExtension("ssf")
    if (datFile.exists()) {
        datFile.renameTo(datFile.withExtension("ssf.bak"))
        file.withExtension("ssf").writeBytes(bytes)
    }
}

fun File.withExtension(extension: String): File {
    return File(this.absolutePath.substringBeforeLast(".") + ".$extension")
}
