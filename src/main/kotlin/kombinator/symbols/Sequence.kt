package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.util.debug
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent

/**
 * Symbol created by definition of a symbol using multiple other symbols in sequence.
 *
 * Default payload: List of payloads for each matched symbol
 */
internal class Sequence(override val members: List<Symbol>) : MultiMatchSymbol() {
    init {
        assert(members.isNotEmpty())
    }

    override fun needsParentheses() =  members.size > 1

    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        val children = mutableListOf<ContextFreeToken>()
        var subMatch: ContextFreeToken
        val members = members.iterator()
        val member1 = members.next()
        debugWithIndent {
            +"-> Sequence (${this@Sequence})"
            +"member 1 = ${member1.id} ${if (member1.reference() is Junction) "(::Junction)" else ""}"
            +"recursions = ${lexerState.recursions}"
        }
        lexerState.savePosition()
        if (member1.reference() !is Junction && member1.id in lexerState.recursions) {
            debug { +"Recursion found for $member1" }
            lexerState.revertPosition()
            return nothingAndCacheFail(lexerState).debugStatus()
        }
        subMatch = member1.match(lexerState)
        if (subMatch === ContextFreeToken.NOTHING) {
            lexerState.revertPosition()
            return nothingAndCacheFail(lexerState).debugStatus()
        }
        children += subMatch
        if (subMatch !== ContextFreeToken.EMPTY) {
            lexerState.skip.consume(lexerState)
        }
        lexerState.failCache.clear()
        while (members.hasNext()) {
            val member = members.next()
            debug { +"Attempting match to next member in ${this@Sequence} ($member)..." }
            subMatch = member.match(lexerState)
            if (subMatch === ContextFreeToken.NOTHING) {
                lexerState.revertPosition()
                return nothingAndCacheFail(lexerState).debugStatus()
            }
            children += subMatch
            if (subMatch !== ContextFreeToken.EMPTY) {
                lexerState.skip.consume(lexerState)
            }
        }
        lexerState.removeSavedPosition()
        return token(lexerState, children).debugStatus()
    }

    override fun toDebugString() = members.joinToString(" ", transform = { it.toIntermediateString() })
}