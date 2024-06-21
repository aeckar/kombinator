package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.io.Serialized
import kombinator.util.debug
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent

/**
 * Symbol created by use of the '|' operator.
 */
@Serialized
internal class Junction(override val members: List<Symbol>) : MultiMatchSymbol() {
    override fun needsParentheses() = members.size > 1

    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        debugWithIndent {
            +"-> Junction (${this@Junction})"
            +"members = ${members.map { it.id }}"
            +"recursions = ${lexerState.recursions}"
        }
        val nothing = ContextFreeToken.NOTHING
        val subMatch = members
            .filter { it.id !in lexerState.recursions }
            .map {
                debug { +"Attempting match to next un-recursive member in ${this@Junction} ($it)..." }
                it.match(lexerState)
            }
            .find { it != nothing } ?: nothing
        return tokenOrNothingAndCacheFail(lexerState, subMatch != ContextFreeToken.NOTHING,
            children = listOf(subMatch),
            ordinal = members.indexOfFirst { it.id == subMatch.origin.id }
        ).debugStatus()
    }

    override fun toDebugString() = members.joinToString(" | ", transform = { it.toIntermediateString() })
}