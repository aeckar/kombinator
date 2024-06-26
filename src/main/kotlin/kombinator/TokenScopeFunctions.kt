@file:Suppress("UNUSED")
package kombinator

/**
 * Provides a scope with the receiver as the single matched [SequenceToken].
 * @throws TokenMismatchException the single match is not a sequence of tokens
 */
inline fun <T : SingleMatchToken, R> T.sequence(block: QualifiedSequenceToken.() -> R) = block(sequence())

/**
 * Provides a scope with the receiver as the single matched [JunctionToken].
 * @throws TokenMismatchException the single match was not created using the '|' operator
 */
inline fun <T : SingleMatchToken, R> T.junction(block: QualifiedJunctionToken.() -> R) = block(junction())

/**
 * Provides a scope with the receiver as the single matched [MultipleToken].
 * @throws TokenMismatchException the single match was not created using the '+' operator
 */
inline fun <T : SingleMatchToken, R> T.multiple(block: QualifiedMultipleToken.() -> R) = block(multiple())

/**
 * Provides a scope with the receiver as the single matched [OptionToken].
 * @throws TokenMismatchException the single match was not created using the '?' operator
 */
inline fun <T : SingleMatchToken, R> T.option(block: QualifiedOptionToken.() -> R) = block(option())

/**
 * Provides a scope with the receiver as the single matched [StarToken].
 * @throws TokenMismatchException the single match was not created using the '*' operator
 */
inline fun <T : SingleMatchToken, R> T.star(block: QualifiedStarToken.() -> R) = block(star())

/**
 * Provides a scope with the receiver as the single matched [CharToken].
 * @throws TokenMismatchException the single match is not or is not delegated to a character literal
 */
inline fun <T : SingleMatchToken, R> T.char(block: QualifiedCharToken.() -> R) = block(character())

/**
 * Provides a scope with the receiver as the single matched [StringToken].
 * @throws TokenMismatchException the single match is not or is not delegated to a text literal
 */
inline fun <T : SingleMatchToken, R> T.text(block: QualifiedStringToken.() -> R) = block(text())

/**
 * Provides a scope with the receiver as the single matched [SwitchToken].
 * @throws TokenMismatchException the single match is not or is not delegated to a switch literal
 */
inline fun <T : SingleMatchToken, R> T.switch(block: QualifiedSwitchToken.() -> R) = block(switch())

/**
 * Provides a scope with the receiver as the matched [SequenceToken] at the specified index in this sequence.
 * @throws TokenMismatchException the match is not a sequence of tokens
 */
inline fun <R> SequenceToken.sequenceAt(index: Int, block: QualifiedSequenceToken.() -> R) = block(sequenceAt(index))

/**
 * Provides a scope with the receiver as the matched [JunctionToken] at the specified index in this sequence.
 * @throws TokenMismatchException the match was not created using the '|' operator
 */
inline fun <R> SequenceToken.junctionAt(index: Int, block: QualifiedJunctionToken.() -> R) = block(junctionAt(index))

/**
 * Provides a scope with the receiver as the matched [MultipleToken] at the specified index in this sequence.
 * @throws TokenMismatchException the match was not created using the '+' operator
 */
inline fun <R> SequenceToken.multipleAt(index: Int, block: QualifiedMultipleToken.() -> R) = block(multipleAt(index))

/**
 * Provides a scope with the receiver as the matched [OptionToken] at the specified index in this sequence.
 * @throws TokenMismatchException the match was not created using the '?' operator
 */
inline fun <R> SequenceToken.optionAt(index: Int, block: QualifiedOptionToken.() -> R) = block(optionAt(index))

/**
 * Provides a scope with the receiver as the matched [StarToken] at the specified index in this sequence.
 * @throws TokenMismatchException the match is not created using the '*' operator
 */
inline fun <R> SequenceToken.starAt(index: Int, block: QualifiedStarToken.() -> R) = block(starAt(index))

/**
 * Provides a scope with the receiver as the matched [CharToken] at the specified index in this sequence.
 * @throws TokenMismatchException the match is not or is not delegated to a character literal
 */
inline fun <R> SequenceToken.charAt(index: Int, block: QualifiedCharToken.() -> R) = block(charAt(index))

/**
 * Provides a scope with the receiver as the matched [StringToken] at the specified index in this sequence.
 * @throws TokenMismatchException the match is not or is not delegated to a text literal
 */
inline fun <R> SequenceToken.stringAt(index: Int, block: QualifiedStringToken.() -> R) = block(stringAt(index))

/**
 * Provides a scope with the receiver as the matched [SwitchToken] at the specified index in this sequence.
 * @throws TokenMismatchException the match is not or is not delegated to a switch literal
 */
inline fun <R> SequenceToken.switchAt(index: Int, block: QualifiedSwitchToken.() -> R) = block(switchAt(index))
