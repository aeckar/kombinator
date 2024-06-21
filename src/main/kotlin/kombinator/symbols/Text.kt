package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.utils.concatenate
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent

/**
 * Symbol created by definition of a string.
 */
internal class Text(val acceptable: String) : Symbol() {
    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        debugWithIndent {
            +"-> Text (${this@Text})"
            +"Attempting match to literal ${toDebugString()}..."
        }
        lexerState.savePosition()
        val result = tokenOrNothingAndCacheFail(lexerState, acceptable.all { it == lexerState.next() },
            length = acceptable.length)
        lexerState.revertPosition()
        lexerState.advancePosition(result.substring.length)
        return result.debugStatus()
    }

    override fun toDebugString() = "\"${acceptable.map { it.toEscapeString() }.concatenate()}\""
}