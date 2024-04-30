

@file:Suppress("UNUSED")
package kombinator

import kombinator.internal.*
import java.io.*

// TODO test
// TODO improve error messages (similar to a compiler, particularly the Rust compiler which gives good insights)

/**
 * Provides the primary functionality of the API.
 * @return a context-free grammar with the given definition and specifications
 */
@Suppress("UNCHECKED_CAST")
fun <R,M : Grammar.MutableState> grammar(
    formalGrammar: String,
    cachePath: String? = null,
    builder: Grammar<R,M>.BuilderContext.() -> Unit
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
        rules = MetaGrammar.grammar.parse(formalGrammar, MetaGrammar.MutableState())
        builder(BuilderContext())
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
class Grammar<R,M : Grammar.MutableState> internal constructor() : Serializable {
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
     * Contains mutable properties which are manipulated by each listener invokation.
     * Useful for collecting information regardless of location in the parse tree.
     * Grammars that do not need such information should create an instance of the base class.
     * The mutable state of parsing attempt must be a subclass of this class, as this class holds
     * information regarding the position of the cursor in a source of input.
     */
    open class MutableState {
        internal var position = 0
    }

    /**
     * The scope wherein the start rule, skip rule, and listeners of a grammar are defined.
     */
    inner class BuilderContext internal constructor() {
        private val listenerDefinition = ListenerDefinition<Any?>() // Reused for each definition

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
        fun ListenerDefinition<out R>.start() {
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
        @Suppress("UNCHECKED_CAST")
        operator fun <P> String.invoke(listener: Token.(M) -> P): ListenerDefinition<P> {
            if (this !in rules) {
                throw MissingRuleException("Rule '$this' is undefined")
            }
            if (this in listeners) {
                throw ReassignmentException("Listener for rule '$this' is already defined")
            }
            listeners[this] = listener
            return (listenerDefinition as ListenerDefinition<P>).apply { id = this@invoke }
        }

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a sequence of tokens.
         * @throws RuleMismatchException the assertion fails
         */
        fun <P> String.sequence(listener: SequenceToken.(M) -> P) = listenerOf<SequenceSymbol,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a rule defined using the '|' operator.
         * @throws RuleMismatchException the assertion fails
         */
        fun <P> String.junction(listener: JunctionToken.(M) -> P) =  listenerOf<JunctionSymbol,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a rule defined using the '' operator.
         * @throws RuleMismatchException the assertion fails
         */
        fun <P> String.multiple(listener: MultipleToken.(M) -> P) = listenerOf<MultipleSymbol,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a rule defined using the '?' operator.
         * @throws RuleMismatchException the assertion fails
         */
        fun <P> String.option( listener: OptionToken.(M) -> P) = listenerOf<OptionSymbol,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a rule defined using the '*' operator.
         * @throws RuleMismatchException the assertion fails
         */
        fun <P> String.star(listener: StarToken.(M) -> P) = listenerOf<StarSymbol,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a character literal.
         * @throws RuleMismatchException the assertion fails
         */
        fun <P> String.character(listener: CharacterToken.(M) -> P) = listenerOf<CharSymbol,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a text literal.
         * @throws RuleMismatchException the assertion fails
         */
        fun <P> String.text(listener: TextToken.(M) -> P) = listenerOf<StringSymbol,_,_>(listener)

        /**
         * Assigns the rule with this ID the given listener.
         * Asserts that the given rule is delegated to a switch literal.
         * @throws RuleMismatchException the assertion fails
         */
        fun <P> String.switch(listener: SwitchToken.(M) -> P) = listenerOf<SwitchSymbol,_,_>(listener)

        /**
         * Throws an exception containing the error message and the current substring.
         * @throws ParseException
         */
        fun Token.raise(message: String, mutableState: M): Nothing {
            throw ParseException("$message (in '$substring')", mutableState.position)
        }

        @Suppress("UNCHECKED_CAST")
        private inline fun <reified S : Symbol,reified T : Token,P> String.listenerOf(
            crossinline listener: T.(M) -> P
        ): ListenerDefinition<P> {
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
            return (listenerDefinition as ListenerDefinition<P>).apply { id = this@listenerOf }
        }
    }

    private fun parse(inputStream: CharStream, mutableState: M): R {
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
            val recursions = mutableListOf<String>()
            val rootToken: ContextFreeToken
            if (mutableState is MetaGrammar.MutableState) { // Meta-grammar definition asserted to be legal syntax
                 rootToken = start.match(inputStream, skip, recursions) // Base of parse tree
            } else {
                skip.consume(inputStream, recursions)
                rootToken = start.match(inputStream, skip, recursions)
                if (rootToken === ContextFreeToken.NOTHING) {
                    throw ParseException("No tokens matching start rule", 0)
                }
                skip.consume(inputStream, recursions)
                if (inputStream.hasNext()) {
                    throw ParseException("Unknown symbol in input", inputStream.position)
                }
            }
            rootToken.walk(listeners, mutableState)
            return rootToken.payload()
        }
    }

    private fun parseOrNull(input: CharStream, mutableState: M): R? {
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
    class ListenerDefinition<P> internal constructor() {
        internal lateinit var id: String
    }

    private companion object {
        @Serial val serialVersionUID = 1L
    }
}
