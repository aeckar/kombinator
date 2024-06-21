package kombinator.ranges

import io.github.aeckar.kanary.ObjectDeserializer
import io.github.aeckar.kanary.Serializer
import io.github.aeckar.kanary.TypedReadOperation
import io.github.aeckar.kanary.TypedWriteOperation
import kombinator.util.MutableIntVector
import java.io.Serial

internal sealed interface RangeVector {
    operator fun contains(n: Int): Boolean
    fun pairs(): List<Pair<Int,Int>>

    companion object Protocol : TypedReadOperation<RangeVector> {
        override fun ObjectDeserializer.readOperation(): RangeVector {
            val lower = read<MutableIntVector>()
            val upper = read<MutableIntVector>()
            val inverted = readBoolean()
            return MutableRangeVector(lower, upper, inverted)
        }

        @Serial
        private fun readResolve(): Any = Protocol
    }
}

internal class MutableRangeVector(
    private val lower: MutableIntVector,
    private val upper: MutableIntVector,
    private var inverted: Boolean
) : RangeVector {
    constructor(initialSize: Int = MutableIntVector.DEFAULT_SIZE)
            : this(MutableIntVector(initialSize), MutableIntVector(initialSize), false)
    private val indices inline get() = lower.indices

    fun add(range: CharRange) = add(range.first, range.last)

    fun add(min: Char, endInclusive: Char) {
        lower += min.code
        upper += endInclusive.code
    }

    fun invertRanges() {
        inverted = !inverted
    }

    override operator fun contains(n: Int): Boolean {
        return if (inverted) {
            indices.all { n < lower[it] || n > upper[it] }
        } else {
            indices.any { n >= lower[it] && n <= upper[it] }
        }
    }

    override fun pairs() = indices.map { lower[it] to upper[it] }

    companion object Protocol : TypedWriteOperation<MutableRangeVector> {
        override fun Serializer.writeOperation(obj: MutableRangeVector) {
            write(obj.lower)
            write(obj.upper)
            writeBoolean(obj.inverted)
        }

        @Serial
        private fun readResolve(): Any = Protocol
    }
}

