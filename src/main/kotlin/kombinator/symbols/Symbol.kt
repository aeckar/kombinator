package kombinator.symbols

import io.github.aeckar.kanary.ObjectDeserializer
import io.github.aeckar.kanary.ProtocolBuilder
import kombinator.ContextFreeToken
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.charstreams.StreamTerminator
import kombinator.util.debug
import kombinator.util.debugIgnore
import kombinator.util.literalEscapes
import kombinator.util.switchEscapes

internal fun <T : Symbol> T.name(id: String) = this.also { it.id = id }

internal fun <T : Symbol> ProtocolBuilder<T>.namedRead(read: ObjectDeserializer.() -> T) {
    read { read(this).name(superclass.read()) }
}

internal fun Char.toEscapeString(): String {
    return when  {
        this.isWhitespace() || this.isISOControl() -> "\\u${this.code.toString(16).padStart(4, '0')}"
        this in literalEscapes -> "\\${literalEscapes[this]}"
        this in switchEscapes -> "\\${switchEscapes[this]}"
        else -> this.toString()
    }
}

/**
 * [Matches][attemptMatch] tokens in a [stream][kombinator.charstreams.CharStream].
 * Conducts the equivalent of the lexical analysis phase in an ordinary parser-generator.
 * @property id a unique identifier. Used by grammars to call listeners of the name.
 * Those starting with '$' are reserved for internal API use
 * @see ContextFreeToken
 * @see kombinator.Grammar
 */
internal sealed class Symbol {
    var id = ""

    /**
     * @return this, or [ImplicitSymbol.reference] if implicit
     */
    open fun reference() = this

    protected fun nothingAndCacheFail(lexerState: LexicalAnalyzerState): ContextFreeToken {
        lexerState.failCache += id
        return ContextFreeToken.NOTHING
    }

    /**
     * Matches the next token in the stream with protection from infinite recursion and caching of failed matches.
     * @return result of [attemptMatch], or [ContextFreeToken.NOTHING] if end of stream reached
     */
    fun match(lexerState: LexicalAnalyzerState): ContextFreeToken {
        if (id in lexerState.failCache) {
            debug { +"Symbol $id is in fail cache, match failed..." }
            return ContextFreeToken.NOTHING
        }
        lexerState.recursions += id
        return try {
             attemptMatch(lexerState)
        } catch (_: StreamTerminator) {
            ContextFreeToken.NOTHING
        } finally {
            lexerState.recursions.removeLast()
        }
    }

    /**
     * Consumes the next characters in stream which match this symbol.
     * Used for skip symbols.
     */
    fun consume(lexerState: LexicalAnalyzerState) {
        println(lexerState)
        debugIgnore {
            match(lexerState)
        }
        println(lexerState)
    }

    /**
     * @return true if debug string needs parentheses to prevent ambiguity
     */
    open fun needsParentheses() = false

    /**
     * @return debug string or ID without ambiguity
     */
    fun toIntermediateString(): String {
        return if (needsParentheses()) {
            "(${debugStringOrID()})"
        } else {
            debugStringOrID()
        }
    }

    fun isMultiChild(): Boolean {
        return when (reference()) {
            is Sequence, is Repetition, is RepeatOption -> true
            else -> false
        }
    }

    fun isTerminal(): Boolean {
        return when (reference()) {
            is Switch, is Text, is Character, is CatchAllSymbol, is ZeroLengthSymbol -> true
            else -> false
        }
    }

    abstract fun toDebugString(): String

    /**
     * If the current in [input][LexicalAnalyzerState.input] contains an expression that agrees with the rules
     * defined by this object, a token is created, which describes:
     * - The [id] of the matching symbol
     * - The children which this symbol comprises (not applicable to all symbol types)
     * - The length of the character string which this symbol matches
     * @param lexerState the mutable state of the lexical analysis
     * @return a unique [ContextFreeToken], or [ContextFreeToken.NOTHING], signifying failure
     * @see consume
     */
    abstract fun attemptMatch(lexerState: LexicalAnalyzerState): ContextFreeToken

    /**
     * @return a unique [ContextFreeToken] with the [id] of this symbol
     * @see tokenOrNothingAndCacheFail
     */
    protected fun token(
        lexerState: LexicalAnalyzerState,
        children: List<ContextFreeToken> = listOf(),
        length: Int = children.sumOf { it.substring.length },
        ordinal: Int = 0
    ): ContextFreeToken {
        return ContextFreeToken(this, lexerState.substring(length), children, ordinal)
    }

    /**
     * @return If predicate is true: a unique [ContextFreeToken] with the [id] of this symbol;
     * If false: [ContextFreeToken.NOTHING]
     * @see token
     */
    protected fun tokenOrNothingAndCacheFail(
        lexerState: LexicalAnalyzerState,
        predicate: Boolean,
        children: List<ContextFreeToken> = listOf(),
        length: Int = children.sumOf { it.substring.length },
        ordinal: Int = 0
    ): ContextFreeToken {
        return if (!predicate) {
            lexerState.failCache += this@Symbol.id
            ContextFreeToken.NOTHING
        } else {
            token(lexerState, children, length, ordinal)
        }
    }

    final override fun toString() = debugStringOrID()

    private fun debugStringOrID() = id.takeIf { it[0] == '$' }?.let { toDebugString() } ?: id

    // TODO maybe remove
    fun generateName() = apply {
        id = "$${this::class.simpleName}:$idCounter"
        ++idCounter
    }

    fun hasGeneratedName(): Boolean = id[0] == '$'

    private companion object {
        var idCounter = 0   // Synchronization not necessary
    }
}