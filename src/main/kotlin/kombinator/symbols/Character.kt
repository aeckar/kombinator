package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.io.Serialized
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent

/**
 * Symbol created by definition of a string of length 1.
 */
@Serialized
internal class Character(val acceptable: Char) : Symbol() {
    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        debugWithIndent {
            +"-> Character (${this@Character})"
            +"Attempting match to literal ${toDebugString()}..."
        }
        val result = tokenOrNothingAndCacheFail(lexerState, lexerState.peek() == acceptable, length = 1)
        lexerState.advancePosition(result.substring.length)
        return result.debugStatus()
    }

    override fun toDebugString() = "\"${acceptable.toEscapeString() }\""
}