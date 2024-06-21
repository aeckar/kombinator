package kombinator.charstreams

import kombinator.util.MutableIntVector
import kombinator.util.debug

/**
 * Incrementally iterates through a set of characters with the ability to move the position of the iterator.
 */
internal sealed class AbstractCharStream(initialPosition: Int) : CharStream {
    var position = initialPosition
        protected set
    private val savedPositions = MutableIntVector()

    override fun next() = peek().also {
        debug { +"Position advanced by 1" }
        ++position
    }

    override fun advancePosition(places: Int) {
        debug { +"Position advanced by $places places" }
        position += places
    }

    override fun savePosition() {
        debug { +"Position saved at $position" }
        savedPositions += position
    }

    override fun revertPosition() {
        try {
            val oldPosition = position
            position = savedPositions.removeLast()
            debug { +"Position reverted from $oldPosition to $position" }
        } catch (e: NoSuchElementException) {
            throw IllegalStateException("No positions are currently marked", e)
        }
    }

    override fun removeSavedPosition() {
        debug { +"Saved position at ${savedPositions[savedPositions.size - 1]} removed" }
        savedPositions.removeLast()
    }

    protected inline fun <R> ensureBounds(block: () -> R): R {
        try {
            return block()
        } catch (_: IndexOutOfBoundsException) {
            throw StreamTerminator
        }
    }
}