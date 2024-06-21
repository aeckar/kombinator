package kombinator.charstreams

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Incrementally iterates through the characters in a text file.
 * Must be closed after use.
 */
internal class FileCharStream(path: String) : AbstractCharStream(0) {
    private val source: BufferedInputStream
    private val chars = StringBuilder()
    private var eofReached = false

     init {
         val sourceFile = File(path)
         val length = sourceFile.length()
         if (length == 0L) {
             throw IOException("File $path does not exist")
         }
         source = BufferedInputStream(FileInputStream(sourceFile))
     }

    override fun peek(): Char {
        if (position >= chars.length) {
            loadChars()
        }
        return ensureBounds { chars[position] }
    }

    private fun loadChars() {
        var b1: Int
        var b2: Int
        do {
            b1 = source.read()
            if (b1 == -1) {
                break
            }
            b2 = source.read()
            if (b2 == -1) {
                break
            }
            chars.append(((b1 shl Byte.SIZE_BITS) or b2).toChar())
            if (b1 == 0x0 && b2 == 0x0) {
                eofReached = true
            }
        } while (!(b1 == 0x0 && b2 == '\n'.code) && !eofReached)
    }

    override fun substring(size: Int): String = ensureBounds { chars.substring(position, position + size) }

    override fun hasNext() = eofReached

    override fun close() {
        source.close()
    }
}