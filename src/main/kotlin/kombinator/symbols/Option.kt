package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent

/**
 * Symbol created by use of the '?' operator.
 * Because of the possibility that nothing is captured, this symbol cannot be given an ID or listener.
 * TODO check ^
 */
internal class Option(val inner: Symbol) : Symbol() {
    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        debugWithIndent {
            +"-> Option (${this@Option})"
            +"Attempting match to inner..."
        }
        val result = inner.match(lexerState)
        return if (result !== ContextFreeToken.NOTHING) {
            token(lexerState, listOf(result))
        } else {
            ContextFreeToken.EMPTY  // .additionalInfo == 0
        }.debugStatus() // cacheFail() never needed
    }

    override fun toDebugString() = "${inner.toIntermediateString()}?"
}