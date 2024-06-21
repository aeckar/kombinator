package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent
import kombinator.ranges.RangeVector

/**
 * Symbol created by definition of a character switch (e.g. \[a-zA-Z]).
 * For up-to ranges (e.g. \[-z]), the lower bound is [Char.MIN_VALUE].
 * For at-least ranges (e.g. \[a-]), the upper bound is [Char.MAX_VALUE], inclusive.
 * For single characters (e.g. \[ab-c]), the lower and upper bounds will be the same.
 * May be implicitly defined.
 */
internal class Switch(val ranges: RangeVector) : Symbol() {
    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        debugWithIndent {
            +"-> Switch (${this@Switch})"
            +"Attempting match to literal ${toDebugString()}..."
        }
        val result = tokenOrNothingAndCacheFail(lexerState, lexerState.peek().code in ranges, length = 1)
        lexerState.advancePosition(result.substring.length)
        return result.debugStatus()
    }

    override fun toDebugString(): String {
        val bounds = ranges.pairs()
        return bounds.joinToString("", "[","]") {
            if (it.first == it.second) {
                return@joinToString it.first.toChar().toEscapeString()
            }
            it.first.toChar().toEscapeString() + "-" + it.second.toChar().toEscapeString()
        }
    }
}