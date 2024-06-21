package kombinator.charstreams

/**
 * Incrementally iterates through a [String].
 * Does not need to be closed.
 */
internal class StringCharStream(private val chars: String) : AbstractCharStream(0) {
    override fun peek() = ensureBounds { chars[position] }
    override fun substring(size: Int) = ensureBounds { chars.substring(position, position + size) }
    override fun hasNext() = position < chars.length
    override fun close() {}

    override fun toString(): String {
        val lowerBound = (position - 20).coerceAtLeast(0)
        val upperBound = (position + 20).coerceAtMost(chars.length)
        return buildString {
            if (lowerBound != 0) {
                append("...")
            }
            append(chars.substring(lowerBound, position))
            append("{ ")
            try {
                append(chars[position.coerceIn(lowerBound, upperBound)])
                append(" }")
                append(chars.substring(position + 1, upperBound))
                if (upperBound != chars.length) {
                    append("...")
                }
            } catch (_: StringIndexOutOfBoundsException) {
                append(" EOF }")
            }
        }
    }
}