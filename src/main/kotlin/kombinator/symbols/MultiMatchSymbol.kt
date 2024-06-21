package kombinator.symbols

/**
 * Symbol matching multiple other symbols.
 */
internal sealed class MultiMatchSymbol : Symbol() {
    abstract val members: List<Symbol>
}