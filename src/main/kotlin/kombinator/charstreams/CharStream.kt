package kombinator.charstreams

import java.io.Closeable

/**
 * Incrementally iterates through a sequence of characters.
 * May or may not need to be closed, depending on implementing class.
 */
internal sealed interface CharStream : Closeable {
    fun next(): Char
    fun advancePosition(places: Int)
    fun savePosition()
    fun revertPosition()
    fun removeSavedPosition()
    fun peek(): Char
    fun substring(size: Int): String
    fun hasNext(): Boolean
}