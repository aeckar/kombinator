package kombinator.symbols

import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.charstreams.StreamTerminator
import kombinator.util.debugStatus
import kombinator.util.debugWithIndent

/**
 * A symbol used within a definition before it is given a proper definition.
 * After it has been defined, the [reference] value holds the proper symbol for producing token of the given ID.
 */
internal class ImplicitSymbol : Symbol() {
    var reference: Symbol? = null
        set(value) {
            assert(value !is ZeroLengthSymbol)
            field = value
        }

    override fun reference() = reference!!

    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        return reference().attemptMatch(lexerState)
    }

    override fun toDebugString() = "::${reference().toIntermediateString()}"
}

/**
 * A catch-all switch literal (e.g. \[-]).
 */
internal class CatchAllSymbol : Symbol() {
    override fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken {
        debugWithIndent {
            +"-> AnyCharacter (${this@CatchAllSymbol})"
            +"Attempting match to literal [-]..."
        }
        return try {
            lexerState.apply {
                peek()  // Ensures EOF not reached
                advancePosition(1)
            }
            token(lexerState, length = 1, ordinal = 1)
        } catch (_: StreamTerminator) {
            ContextFreeToken.NOTHING
        }.debugStatus()
    }

    override fun toDebugString() = "[-]"
}

/**
 * Special rule for internal API usage.
 */
internal class ZeroLengthSymbol : Symbol() {
    override fun attemptMatch(lexerState: LexicalAnalyzerState) = ContextFreeToken.EMPTY

    override fun toDebugString() = id
}