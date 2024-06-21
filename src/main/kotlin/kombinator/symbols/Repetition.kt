package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.util.debug
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent

/**
 * Symbol created by use of the '+' operator.
 */
internal class Repetition(val inner: Symbol) : Symbol() {
    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        debugWithIndent {
            +"-> Repetition (${this@Repetition})"
            +"Attempting match to inner... [x1]"
        }
        val children = mutableListOf<ContextFreeToken>()
        var subMatch = inner.match(lexerState)
        while (subMatch !== ContextFreeToken.NOTHING) {
            children += subMatch
            if (subMatch.substring.isEmpty()) {
                break
            }
            debug { +"Attempting inner.match()... [x${children.size + 1}]" }
            subMatch = inner.match(lexerState)
        }
        return tokenOrNothingAndCacheFail(lexerState, children.isNotEmpty(), children).debugStatus()
    }

    override fun toDebugString() = "${inner.toIntermediateString()}+"
}