package kombinator

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