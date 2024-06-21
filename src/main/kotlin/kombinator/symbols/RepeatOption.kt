package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent

/**
 * Symbol created by use of the '*' operator.
 * Because of the possibility that nothing is captured, this symbol cannot be given an ID or listener.
 */
internal class RepeatOption(inner: Symbol) : Symbol() {
    val equivalent = Option(Repetition(inner))

    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        debugWithIndent {
            +"-> RepeatOption (${this@RepeatOption})"
            +"Attempting match to equivalent..."
        }
        val result = equivalent.match(lexerState)
        return if (result.isNotPresent()) {
            ContextFreeToken.EMPTY
        } else {
            result.children[0].apply { origin = this@RepeatOption }
        }.debugStatus() // cacheFail() never needed
    }

    override fun toDebugString() = "${(equivalent.inner as Repetition).inner.toIntermediateString()}*"
}