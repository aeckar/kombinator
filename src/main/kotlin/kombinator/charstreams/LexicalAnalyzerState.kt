package kombinator.charstreams

import kombinator.symbols.Symbol

/**
 * Stores important information context-free mutable variables during [lexical analysis][Symbol.match]
 * @property input the input stream;
 * this object is delegated to [CharStream] through this property to improve conciseness
 * @property skip the skip symbol, called after every matched token
 * @property recursions the symbol stack, used to disallow infinite recursion
 * @property failCache previously attempted matches that failed. Used to optimize lexer
 */
internal data class LexicalAnalyzerState(
    val input: AbstractCharStream,
    val skip: Symbol,
    val recursions: MutableList<String> = mutableListOf(),
    val failCache: MutableList<String> = mutableListOf()
) : CharStream by input