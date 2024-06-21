package kombinator.util

import kombinator.ranges.MutableRangeVector
import kombinator.symbols.*

internal typealias RuleMap = MutableMap<String, Symbol>

/**
 * Permits the creation of a [RuleMap] in an easily readable format.
 */
internal inline fun buildRuleMap(builder: RuleMapBuilder.() -> Unit) = RuleMapBuilder().apply(builder).build()

/**
 * Provides a scope wherein a [map][buildRuleMap] of IDs to their respective symbol
 * can be created in an easily readable format.
 */
internal class RuleMapBuilder {
    private val rules = mutableMapOf<String, Symbol>()
    private val implicitSymbols = mutableMapOf<String, ImplicitSymbol>()

    fun sequence(vararg symbols: Symbol): Symbol = Sequence(symbols.toList())
    fun junction(vararg possibilities: Symbol): Symbol = Junction(possibilities.toList())
    fun multiple(inner: Symbol): Symbol = Repetition(inner)
    fun option(inner: Symbol): Symbol = Option(inner)
    fun star(inner: Symbol): Symbol = RepeatOption(inner)
    fun string(acceptable: String) = Text(acceptable)
    fun char(acceptable: Char) = Character(acceptable)
    fun switch(vararg possibilities: Char) = switch(possibilities, false)
    fun invertedSwitch(vararg possibilities: Char) = switch(possibilities, true)

    // TODO remove if unneeded
    fun switch(vararg possibilities: CharRange): Symbol {
        val ranges = MutableRangeVector()
        possibilities.forEach { ranges.add(it.first, it.last) }
        return Switch(ranges)
    }

    operator fun get(id: String): Symbol {
        rules[id]?.let {
            return it
        }
        val newImplicit = ImplicitSymbol().name(id)
        implicitSymbols[id] = newImplicit
        return newImplicit
    }

    operator fun set(id: String, definition: Symbol) {
        val implicit = implicitSymbols[id]
        if (implicit != null) {
            implicit.reference = definition
        } else {
            rules[id] = definition
        }
    }

    fun build(): RuleMap = rules

    private fun switch(possibilities: CharArray, invert: Boolean): Symbol {
        val ranges = MutableRangeVector()
        possibilities.forEach { ranges.add(it, it) }
        return Switch(ranges.apply { if (invert) invertRanges() })
    }
}