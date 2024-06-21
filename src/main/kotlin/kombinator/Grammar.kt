@file:Suppress("UNUSED")
package kombinator

import kombinator.charstreams.AbstractCharStream
import kombinator.charstreams.FileCharStream
import kombinator.charstreams.LexicalAnalyzerState
import kombinator.charstreams.StringCharStream
import kombinator.util.*
import kombinator.symbols.*
import kombinator.symbols.Character
import kombinator.symbols.Junction
import kombinator.symbols.Text
import kombinator.symbols.Switch
import kombinator.symbols.Symbol
import java.io.*

// TODO test
// TODO improve error messages (similar to a compiler, particularly the Rust compiler which gives good insights)

/**
 * Provides the primary functionality of the API.
 * @return a context-free grammar with the given definition and specifications
 */
@Suppress("UNCHECKED_CAST")
fun <R,M : MutableState> grammar(
    builder: Grammar<R,M>.BuilderScope.() -> Unit
): Grammar<R,M> {
    val needsCache = cachePath?.let {
        val file = File(cachePath)
        if (file.exists()) {
            ObjectInputStream(FileInputStream(file)).use {
                try {
                    return it.readObject() as Grammar<R, M>
                } catch (_: TypeCastException) {
                    // ...fallthrough
                }
            }
        }
    }
    return Grammar<R,M>().apply {
        rules = metaGrammar.parse(formalGrammar, MetaGrammarState())
        builder(BuilderScope())
        needsCache?.let {
            ObjectOutputStream(FileOutputStream(cachePath)).use {
                it.writeUnshared(this)
                it.flush()
            }
        }
    }
}

/**
 * A context-free grammar used to parse complex expressions in a string.
 * @param R the type of the
 */
class Grammar<R,M : MutableState> internal constructor() : Serializable {
    private val listeners = mutableMapOf<String, Token.(M) -> Any?>()
    internal var rules: MutableMap<String, Symbol> by AssignOnce()
    private var startID: String by AssignOnce()
    private var skipID: String by AssignOnce()

    /**
     * Parses the input while modifying the given mutable state.
     * @return the payload of the principle token (the base of the parse tree)
     * @throws ParseException there is nothing to parse or there is an unknown symbol
     * @see parseOrNull
     */
    fun parse(input: String, mutableState: M) = parse(StringCharStream(input), mutableState)

    /**
     * Parses the input while modifying the given mutable state.
     * @return the payload of the principle token (the base of the parse tree),
     * or null if there is nothing to parse or there is an unknown symbol
     * @see parse
     */
    fun parseOrNull(input: String, mutableState: M) = parseOrNull(StringCharStream(input), mutableState)

    /**
     * Parses the input present within the given text file while modifying the given mutable state.
     * @return the payload of the principle token (the base of the parse tree)
     * @throws ParseException there is nothing to parse or there is an unknown symbol
     * @see parseFileOrNull
     */
    fun parseFile(inputPath: String, mutableState: M) = parse(FileCharStream(inputPath), mutableState)

    /**
     * Parses the input present within the given text file while modifying the given mutable state.
     * @return the payload of the principle token (the base of the parse tree),
     * or null if there is nothing to parse or there is an unknown symbol
     * @see parseFile
     */
    fun parseFileOrNull(inputPath: String, mutableState: M) = parseOrNull(FileCharStream(inputPath), mutableState)

    /**
     * The scope wherein the start rule, skip rule, and listeners of a grammar are defined.
     */
    inner class BuilderScope internal constructor() {
        internal var rules: RuleMap
            get() = this@Grammar.rules
            set(value) {
                this@Grammar.rules = value
            }
        var name: String by AssignOnce()
        var definition: String by AssignOnce()

        private val sharedListener = Listener()

        /**
         * Imports a rule from another grammar.
         * Importing a rule does not import the rules that it is defined by; those must be imported individually.
         * @throws MissingRuleException the specified rule is not defined in the other grammar
         */
        infix fun String.from(other: Grammar<*,*>) {
            rules[this] = other.rules[this] ?: throw MissingRuleException("Rule '$this' is undefined")
        }

        /**
         * Declares the rule with this ID to be the principle rule.
         */
        fun Listener.start() {
            try {
                startID = id
            } catch (e: ReassignmentException) {
                throw ReassignmentException("Start rule already defined")
            }
        }

        /**
         * Declares the rule with this ID to be skipped between match attempts.
         *
         * Because the skip rule is used primarily to determine the boundary between tokens
         * and is not part of the final token (parse) tree, there is little use in providing it a listener.
         * This is especially true considering how many times the skip rule is used during tokenization.
         */
        fun String.skip() {
            try {
                skipID = this
            } catch (e: ReassignmentException) {
                throw ReassignmentException("Skip rule already defined")
            }
        }

        /**
         * Assigns the rule with this ID the given listener.
         * @return the rule ID
         * @throws MissingRuleException the rule is not present in the definition and has not been imported
         * @throws ReassignmentException the listener for this rule has already been declared
         */
        operator fun <P> String.invoke(listener: Token.(M) -> P): Listener<P> {
            if (this !in rules) {
                throw MissingRuleException("Rule '$this' is undefined")
            }
            if (this in listeners) {
                throw ReassignmentException("Listener for rule '$this' is already defined")
            }
            listeners[this] = listener
            return sharedListener.apply { id = this@invoke }
        }

        /* ------------------------------ listener entry points ------------------------------ */

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a sequence of tokens.
         * @throws RuleMismatchException the assertion fails
         */
        infix fun String.sequence(listener: SequenceToken.(M) -> Any?) = listenerOf<Sequence,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a rule defined using the '|' operator.
         * @throws RuleMismatchException the assertion fails
         */
        infix fun String.junction(listener: JunctionToken.(M) -> Any?) =  listenerOf<Junction,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a rule defined using the '' operator.
         * @throws RuleMismatchException the assertion fails
         */
        infix fun String.multiple(listener: MultipleToken.(M) -> Any?) = listenerOf<Repetition,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a rule defined using the '?' operator.
         * @throws RuleMismatchException the assertion fails
         */
        infix fun String.option( listener: OptionToken.(M) -> Any?) = listenerOf<Option,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a rule defined using the '*' operator.
         * @throws RuleMismatchException the assertion fails
         */
        infix fun String.star(listener: StarToken.(M) -> Any?) = listenerOf<RepeatOption,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a character literal.
         * @throws RuleMismatchException the assertion fails
         */
        infix fun String.char(listener: CharToken.(M) -> Any?) = listenerOf<Character,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a text literal.
         * @throws RuleMismatchException the assertion fails
         */
        infix fun String.text(listener: StringToken.(M) -> Any?) = listenerOf<Text,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a switch literal.
         * @throws RuleMismatchException the assertion fails
         */
        infix fun String.switch(listener: SwitchToken.(M) -> Any?) = listenerOf<Switch,_,_>(listener)

        /* ------------------------------ client-side error handling ------------------------------ */

        /**
         * Throws an exception containing the error message and the current substring.
         * @throws ParseException
         */
        fun Token.raise(message: String, mutableState: M): Nothing {
            throw ParseException("$message (in '$substring')", mutableState.position)
        }

        /* ------------------------------ private API ------------------------------ */

        private inline fun <reified S : Symbol,reified T : Token> String.listenerOf(
            crossinline listener: T.(M) -> Any?
        ): Listener {
            invoke {
                if ((this as ContextFreeToken).origin.reference()::class != S::class) {
                    throw RuleMismatchException("Listener type does not agree with type of rule '$this'")
                }
                listener(this as T, it)
            }
            val symbol = rules.getValue(this).reference()
            if (symbol !is S) {
                throw RuleMismatchException("Type of rule '$this' described by listener (${S::class.simpleName}) " +
                        "does not agree with actual type (${symbol::class.simpleName})")
            }
            return sharedListener.apply { id = this@listenerOf }
        }
    }

    private fun parse(inputStream: AbstractCharStream, mutableState: M): R {
        inputStream.use {
            val start = try {
                rules.getValue(startID)
            } catch (_: NoSuchElementException) {
                throw MissingRuleException("Start symbol is not defined")
            }
            val skip = try {
                rules.getValue(skipID)
            } catch (_: NoSuchElementException) {
                throw MissingRuleException("Skip symbol is not defined")
            }
            val parserState = LexicalAnalyzerState(inputStream, skip)
            val rootToken: ContextFreeToken
            if (mutableState is MetaGrammarState) { // Meta-grammar definition asserted to be legal syntax
                 rootToken = start.match(parserState) // Base of parse tree
            } else {
                skip.consume(parserState)
                rootToken = start.match(parserState)
                if (rootToken === ContextFreeToken.NOTHING) {
                    throw ParseException("No tokens matching start rule", 0)
                }
                skip.consume(parserState)
                if (inputStream.hasNext()) {
                    throw ParseException("Unknown symbol in input", inputStream.position)
                }
            }
            rootToken.walk(listeners, mutableState)
            return rootToken.payload()
        }
    }

    private fun parseOrNull(input: AbstractCharStream, mutableState: M): R? {
        return try {
            parse(input, mutableState)
        } catch (e: ParseException) {
            null
        }
    }

    /**
     * Returned by listener definition.
     *
     * API Note: Enforces correct declaration of start symbol
     */
    class Listener internal constructor() {
        internal lateinit var id: String
    }

    private companion object {
        @Serial val serialVersionUID = 1L
    }
}
