package kombinator.internal

import kombinator.ContextFreeToken
import kombinator.internal.MetaGrammar.characterEscapes

/**
 * [Matches][attemptMatch] tokens in a [stream][CharStream].
 * @property id a unique identifier. Used by grammars to call listeners of the name.
 * Not needed for implicitly defined symbols or [OptionSymbol]s.
 * Those starting in an underscore or digit are reserved for the compiler.
 * @see ContextFreeToken
 * @see kombinator.Grammar
 */
internal sealed class Symbol(var id: String) {
    /**
     * @return this, or [ImplicitSymbol.reference] if implicit
     */
    open fun reference() = this

    /**
     * [attemptMatch] with protection from infinite recursion.
     */
    fun match(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        recursions.add(id)
        return try {
            attemptMatch(input, skip, recursions)
        } catch (_: StreamTerminator) {
            ContextFreeToken.NOTHING
        } finally {
            recursions.removeLast()
        }
    }

    /**
     * Consumes the next characters in stream which match this symbol.
     * Used for skip symbols.
     */
    fun consume(input: CharStream, recursions: MutableList<String>) {
        match(input, ContextFreeToken.EMPTY.origin, recursions)
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
        return when (this) {
            is SequenceSymbol, is MultipleSymbol, is StarSymbol -> true
            is ImplicitSymbol -> reference.isMultiChild()
            else -> false
        }
    }

    fun isTerminal(): Boolean {
        return when (this) {
            is SwitchSymbol, is StringSymbol, is CharSymbol, is AnyCharacterSymbol, is ZeroLengthSymbol -> true
            is ImplicitSymbol -> reference.isTerminal()
            else -> false
        }
    }

    abstract fun toDebugString(): String

    /**
     * If the current in [input] contains an expression that agrees with the rules defined by this object, a token
     * is created, which describes:
     * - The [id] of the matching symbol
     * - The chiildren which this symbol comprises (not applicable to all symbol types)
     * - The length of the character string which this symbol matches
     * @param input the target of the lexical analysis
     * @param skip the symbol matching any ignored expressions
     * @return a unique [ContextFreeToken], or [ContextFreeToken.NOTHING], signifying failure
     * @see consume
     */
    protected abstract fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken

    /**
     * @return a unique [ContextFreeToken] with the [id] of this symbol
     * @see tokenOrNothing
     */
    protected fun token(
        input: CharStream,
        children: List<ContextFreeToken> = listOf(),
        length: Int = children.sumOf { it.substring.length },
        ordinal: Int = 0
    ): ContextFreeToken {
        return ContextFreeToken(this, input.substring(length), children, ordinal)
    }

    /**
     * @return If predicate is true: a unique [ContextFreeToken] with the [id] of this symbol;
     * If false: [ContextFreeToken.NOTHING]
     * @see token
     */
    protected fun tokenOrNothing(
        input: CharStream,
        predicate: Boolean,
        children: List<ContextFreeToken> = listOf(),
        length: Int = children.sumOf { it.substring.length },
        ordinal: Int = 0
    ): ContextFreeToken {
        return if (!predicate) ContextFreeToken.NOTHING else token(input, children, length, ordinal)
    }

    final override fun toString() = debugStringOrID()

    private fun debugStringOrID() = id.takeIf { it[0] == '$' }?.let { toDebugString() } ?: id
}

/**
 * Symbol created by definition of a symbol using multiple other symbols in sequence.
 *
 * Default payload: List of payloads for each matched symbol
 */
internal class SequenceSymbol(id: String = generateID("Sequence"), private val members: List<Symbol>) : Symbol(id) {
    constructor(id: String, vararg members: Symbol) : this(id, members.toList())
    constructor(vararg members: Symbol) : this(generateID("SequenceSymbol"), members.toList())

    init {
        assert(members.isNotEmpty())
    }

    override fun needsParentheses() =  members.size > 1

    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        val children = mutableListOf<ContextFreeToken>()
        var subMatch: ContextFreeToken

        input.savePosition()
        for (member in members) {
            subMatch = member.match(input, skip, recursions)
            if (subMatch === ContextFreeToken.NOTHING) {
                input.revertPosition()
                return ContextFreeToken.NOTHING
            }
            children += subMatch
            skip.consume(input, recursions)
        }
        input.removeSavedPosition()
        return token(input, children)
    }

    companion object {
        val ID = SequenceSymbol("ID",
            SwitchSymbol(vectorOf('a', 'A'), vectorOf('z', 'Z')),
            StarSymbol(SwitchSymbol(vectorOf('a', 'A', '0', '_'), vectorOf('z', 'Z', '9', '_')))
        )
    }

    override fun toDebugString() = members.joinToString(" ", transform = { it.toIntermediateString() })
}

/**
 * Symbol created by use of the '|' operator.
 */
internal class JunctionSymbol(id: String = generateID("Junction"), private val members: List<Symbol>) : Symbol(id) {
    constructor(id: String, vararg members: Symbol) : this(id, members.toList())
    constructor(vararg members: Symbol) : this(generateID("JunctionSymbol"), members.toList())

    override fun needsParentheses() = members.size > 1

    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        val nothing = ContextFreeToken.NOTHING
        val subMatch = members
            .asSequence()
            .filter { it.id !in recursions }
            .map { it.match(input, skip, recursions) }
            .find { it != nothing } ?: nothing
        return tokenOrNothing(input, subMatch != ContextFreeToken.NOTHING,
            children = listOf(subMatch),
            ordinal = members.indexOfFirst { it.id == subMatch.origin.id }
        )
    }

    override fun toDebugString() = members.joinToString(" | ", transform = { it.toIntermediateString() })
}

/**
 * Symbol created by use of the '+' operator.
 */
internal class MultipleSymbol(id: String = generateID("Multiple"), val inner: Symbol) : Symbol(id) {
    constructor(inner: Symbol) : this(generateID("MultipleSymbol"), inner)

    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
         val children = mutableListOf<ContextFreeToken>()
        var subMatch = inner.match(input, skip, recursions)
        while (subMatch !== ContextFreeToken.NOTHING) {
            children += subMatch
            if (subMatch.substring.isEmpty()) {
                break
            }
            subMatch = inner.match(input, skip, recursions)
        }
        return tokenOrNothing(input, children.isNotEmpty(), children)
    }

    override fun toDebugString() = "${inner.toIntermediateString()}+"
}

/**
 * Symbol created by use of the '?' operator.
 * Because of the possibility that nothing is captured, this symbol cannot be given an ID or listener.
 */
internal class OptionSymbol(val inner: Symbol) : Symbol(generateID("Option")) {
    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        val result = inner.match(input, skip, recursions)
        return if (result !== ContextFreeToken.NOTHING) {
            token(input, listOf(result))
        } else {
            ContextFreeToken.EMPTY  // .additionalInfo == 0
        }
    }

    override fun toDebugString() = "${inner.toIntermediateString()}?"
}

/**
 * Symbol created by use of the '*' operator.
 * Because of the possibility that nothing is captured, this symbol cannot be given an ID or listener.
 */
internal class StarSymbol(inner: Symbol) : Symbol(generateID("Star")) {
    private val equivalent = OptionSymbol(MultipleSymbol(inner))

    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        val result = equivalent.match(input, skip, recursions)
        return if (result.isNotPresent()) ContextFreeToken.EMPTY else result.children[0].apply { origin = this@StarSymbol }
    }

    override fun toDebugString() = "${(equivalent.inner as MultipleSymbol).inner.toIntermediateString()}*"
}

/**
 * Symbol created by definition of a character switch (e.g. \[a-zA-Z]).
 * For up-to ranges (e.g. \[-z]), [lowerBounds] will store [Char.MIN_VALUE].
 * For at-least ranges (e.g. \[a-]), [upperBounds] will store [Char.MAX_VALUE].
 * For single characters (e.g. \[ab-c]), the lower and upper bounds will be the same.
 * May be implicitly defined.
 */
internal class SwitchSymbol(
    id: String = generateID("SwitchSymbol"),
    private val lowerBounds: IntVector,
    private val upperBounds: IntVector
) : Symbol(id) {
    constructor(lowerBounds: IntVector, upperBounds: IntVector) : this(generateID("Switch"), lowerBounds, upperBounds)

    init {
        assert(lowerBounds.size == upperBounds.size)
    }

    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        val test = lowerBounds.indices.indexOfFirst { input.peek().code in lowerBounds[it]..upperBounds[it] }
        val result = tokenOrNothing(input, test != -1, length = 1, ordinal = test)
        input.advancePosition(result.substring.length)
        return result
    }

    override fun toDebugString(): String {
        val bounds = mutableListOf<Pair<Int,Int>>() // Auto-boxing not a concern
        lowerBounds.indices.forEach {
            bounds += lowerBounds[it] to upperBounds[it]
        }
        return bounds.joinToString("", "[","]") {
            it.first.toChar().toEscapeString() + "-" + it.second.toChar().toEscapeString()
        }
    }

    companion object {
        val DIGIT = SwitchSymbol("DIGIT", vectorOf('0'), vectorOf('9'))

        fun excluding(c: Char) = SwitchSymbol(vectorOf(Char.MIN_VALUE, c + 1), vectorOf(c - 1, Char.MAX_VALUE))

        fun including(vararg c: Char): SwitchSymbol {
            val lowerBounds = MutableIntVector(c.size)
            val upperBounds = MutableIntVector(c.size)
            for (char in c) {
                lowerBounds += char.code
                upperBounds += char.code
            }
            return SwitchSymbol(lowerBounds, upperBounds)
        }
    }
}

/**
 * Symbol created by definition of a string.
 */
internal class StringSymbol(id: String = generateID("Text"), private val acceptable: String) : Symbol(id) {
    constructor(acceptable: String) : this(generateID("StringSymbol"), acceptable)

    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        input.savePosition()
        val result = tokenOrNothing(input, acceptable.all { it == input.next() }, length = acceptable.length)
        input.revertPosition()
        input.advancePosition(result.substring.length)
        return result
    }

    override fun toDebugString() = "\"${acceptable.toEscapeString()}\""
}

/**
 * Symbol created by defintition of a string of length 1.
 */
internal class CharSymbol(id: String = generateID("Character"), val acceptable: Char) : Symbol(id) {
    constructor(acceptable: Char) : this(generateID("CharSymbol"), acceptable)

    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        val result = tokenOrNothing(input, input.peek() == acceptable, length = 1)
        input.advancePosition(result.substring.length)
        return result
    }

    override fun toDebugString() = "\"${acceptable.toEscapeString() }\""

    companion object {
        val DASH = CharSymbol("DASH", '-')
        val QUOTE = CharSymbol("QUOTE", '"')
    }
}


/**
 * A catch-all switch literal (e.g. \[-]).
 */
internal class AnyCharacterSymbol(id: String = generateID("AnyCharacter")) : Symbol(id) {
    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        try {
            input.peek()    // throws if end is reached
            return token(input, length = 1, ordinal = 1)
        } catch (e: IndexOutOfBoundsException) {
            return ContextFreeToken.NOTHING
        }
    }

    override fun toDebugString() = "[-]"
}

internal class ImplicitSymbol(id: String = generateID("Implicit")) : Symbol(id) {
    lateinit var reference: Symbol

    override fun reference() = reference

    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>): ContextFreeToken {
        return reference.match(input, skip, recursions)
    }

    override fun toDebugString() = "::${reference.toIntermediateString()}"
}

/**
 * Special rule for internal API usage.
 */
internal class ZeroLengthSymbol(id: String) : Symbol(id) {
    override fun attemptMatch(input: CharStream, skip: Symbol, recursions: MutableList<String>) = ContextFreeToken.EMPTY

    override fun toDebugString() = id
}

private fun Any?.toEscapeString(): String {
    return toString().map { c ->
        when  {
            c.isWhitespace() || c.isISOControl() -> "\\u${c.code.toString(8).padStart(4, '0')}"
            c in characterEscapes.values -> "\\${characterEscapes.filterValues { it == c }.keys.single()}"
            else -> c
        }
    }.joinToString(separator = "")
}

private var idCounter = 0   // Synchronization not necessary

private fun generateID(shortName: String) = "$$shortName:$idCounter".also { ++idCounter }