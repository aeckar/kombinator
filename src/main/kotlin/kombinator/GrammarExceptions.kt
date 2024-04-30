package kombinator

/**
 * An exception through by the [kombinator] API.
 */
sealed class GrammarException(message: String) : Exception(message)

/**
 * Thrown when a rule is expected to be defined, but is not.
 * Thrown after building [building][Grammar.BuilderContext] when the start and skip rules are resolved,
 * or when [importing][Grammar.BuilderContext.from] a rule from another grammar.
 */
class MissingRuleException(message: String) : GrammarException(message)

/**
 * Thrown to denote that a fatal error has occurred during parsing.
 * @property index the index in the input where the error occurred
 */
class ParseException internal constructor(
    message : String,
    index: Int
) : GrammarException("$message at index $index") {
    var index = index
        internal set
}

/**
 * Thrown when there is an attempt to assign a value to a property that has already been given a value
 * and can only be assigned a value once.
 */
class ReassignmentException internal constructor(message: String) : GrammarException(message)

/**
 * Thrown when a token is asserted to be of a certain type, but is actually of a different type.
 * Thrown from a listener during while the token (parse) tree is being walked.
 */
class TokenMismatchException internal constructor(message: String) : GrammarException(message)

/**
 * Thrown when a rule is asserted to produce a token of a certain type, but is actually of a different type.
 * Thrown during the [building][Grammar.BuilderContext] of a grammar.
 */
class RuleMismatchException internal constructor(message: String) : GrammarException(message)