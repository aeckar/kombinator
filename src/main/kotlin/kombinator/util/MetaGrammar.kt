package kombinator.util

import kombinator.*
import kombinator.util.*
import kombinator.ranges.MutableRangeVector
import kombinator.symbols.*
import kombinator.symbols.CatchAllSymbol
import kombinator.symbols.Character
import kombinator.symbols.ImplicitSymbol
import kombinator.symbols.Junction
import kombinator.symbols.Switch
import kombinator.symbols.Symbol

internal val literalEscapes = mapOf(
    '\t' to 't',
    '\n' to 'n',
    '\r' to 'r',
    '\'' to '\'',
    '\\' to '\\'
)

internal val switchEscapes = mapOf(
    't' to '\t',
    'n' to '\n',
    'r' to '\r',
    '-' to '-',
    ']' to ']',
    '\\' to '\\'
)

internal class MetaGrammarState : MutableState() {
    val implicitSymbols = mutableMapOf<String, ImplicitSymbol>()
    val rules = mutableMapOf<String, Symbol>()
}

internal val metaGrammar = grammar<Map<String, Symbol>, MetaGrammarState> {
    name = "meta"

    rules = buildRuleMap {}

    definition = """
            id: [a-zA-Z] [a-zA-Z0-9_]*;
    
            symbol: "(" symbol ")"
                | junction
                | sequence
                | multiple
                | option
                | id
                | literal
                | switch
                ;
            
            rule: id ':' symbol ';'
    
            sequence: symbol symbol+;
            junction: symbol ('|' symbol)+;
            multiple: symbol '+';
            option: symbol '?';
            star: symbol '*';
    
            meta: rule+;
            skip: ([\u0000-\u0009\u000B-\u0001F]+ | '/*'  [-]* '*/' | '//' [-\u0009\u000B-])+;
        """

    "literal" from literalGrammar   // => Symbol
    "switch" from switchGrammar     // => Symbol

    "id".default()

    junction("symbol") produces { mutableState ->
        when (ordinal()) {
            0 -> (child as Sequence)[1].payload()

            5 -> {  // id
                val id = child.substring
                mutableState.rules.getOrElse(id) {
                    mutableState.implicitSymbols[id] ?: ImplicitSymbol().name(id).also {
                        mutableState.implicitSymbols[id] = it
                    }
                }
            }

            else -> child.payload()
        }
    }

    "rule" sequence { mutableState ->
        val id = sequenceAt(0).substring
        val definition = junctionAt(2) {    // -> symbol
            val symbol = match.payload<Symbol>()
            if (id in mutableState.implicitSymbols) {
                mutableState.implicitSymbols.getValue(id).apply { reference = symbol }
            } else if (symbol.isTerminal() && symbol.hasGeneratedName()) {
                symbol.apply { this.id = id }
            } else {
                ImplicitSymbol().name(id).apply { reference = symbol }
            }
        }
        mutableState.rules[id] = definition
    }

    "sequence" multiple {
        val symbols = ArrayList<Symbol>(matches.size)
        matches.forEach { symbols.add(it.payload()) }
        Sequence(symbols)
    }

    "junction" sequence {
        val symbols = mutableListOf<Symbol>(this[0].payload())
        multipleAt(1) { // -> ('|' symbol)+
            sequences().forEach { symbols.add(it[1].payload()) }
        }
        Junction(symbols)
    }

    // disregard operator, which is often index [1]

    "multiple" sequence {
        Repetition(this[0].payload())
    }

    "option" sequence {
        Option(this[0].payload())
    }

    "star" sequence {
        RepeatOption(this[0].payload())
    }

    "meta" multiple { mutableState ->
        if (mutableState.implicitSymbols.any()) {
            raise("No definitions found for implicit symbols: ${mutableState.implicitSymbols}", mutableState)
        }
        mutableState.rules
    }.start()

    "skip".skip()
}

private val literalEscapesList = literalEscapes.toList()
private val switchEscapesList = switchEscapes.toList()

@Suppress("RemoveExplicitTypeArguments")
private val literalGrammar = grammar<Symbol, MutableState> {
    name = "literal"

    rules = buildRuleMap {
        val quote = char('\'').generateName()   // TODO check generateName

        this["literal"] = sequence(
            quote,
            multiple(this["char"]),
            quote
        )

        this["char"] = junction(
            this["escape"],
            invertedSwitch('\n', '\'')
        )

        this["escape"] = sequence(
            char('\\'),
            switch('t', 'n', 'r', '\'', '\\')
        )
    }

    // TODO do I need these escapes?
    definition = """
        literal: '\'' char+ '\''
        
        char: escape | (~[\n'])
        escape: '\\' [tnr'\\]
    """

    "literal".sequence<Symbol> {
        val chars = multipleAt(1).payload<List<Char>>()
        if (chars.size == 1) {
            return@sequence Character(chars.single())
        }
        Text(chars.joinToString(separator = ""))
    }.start()

    "char".default()

    // TODO make these opener functions infix with P as Any? for conciseness
    "escape".sequence<Char> {
        switchAt(1) {
            literalEscapesList.single { it.second == substring.single() }.first
        }
    }

    skipNothing()
}

@Suppress("RemoveExplicitTypeArguments")
private val switchGrammar = grammar<Symbol, MutableState> {
    name = "switch"

    rules = buildRuleMap {
        val dash = char('-').generateName()

        this["switch"] = sequence(
            option(char('~')),
            char('['),
            junction(
                sequence(
                    option(this["upToRange"]),
                    star(
                        junction(
                            this["boundedRange"],
                            this["singleChar"]
                        )
                    ),
                    option(this["atLeastRange"])
                ),
                this["catchAll"]
            ),
            char(']')
        )

        this["boundedRange"] = sequence(
            this["char"],
            dash,
            this["char"]
        )

        this["singleChar"] = this["char"]

        this["upToRange"] = sequence(
            dash,
            this["char"]
        )

        this["atLeastRange"] = sequence(
            this["char"],
            dash
        )

        this["catchAll"] = char('-')

        this["char"] = junction(
            this["escape"],
            invertedSwitch('\n', ']')
        )

        this["escape"] = sequence(
            char('\\'),
            switch('t', 'n', 'r', '-', ']', '\\')
        )
    }

    definition = """
            switch: '~'? '[' ((upToRange? (boundedRange | singleChar)* atLeastRange?) | catchAll) ']'
            
            boundedRange: char '-' char
            singleChar: char
            
            upToRange: '-' char
            atLeastRange: char '-'
            catchAll: '-'
            
            char: escape | (~[\n\]])
            escape: '\\' [tnr\-\\\]]
        """

    "switch".sequence<Symbol> {
        junctionAt(2) {
            if (match.id == "catchAll") {
                return@sequence CatchAllSymbol()
            }
            val ranges = MutableRangeVector()
            flatten().forEach { ranges.add(it.payload()) }
            optionAt(0).ifPresent {
                ranges.invertRanges()
            }
            Switch(ranges)
        }
    }.start()

    "boundedRange".sequence<CharRange> {
        CharRange(this[0].payload(), this[2].payload())
    }

    "singleChar".sequence<CharRange> {
        val char = this[0].payload<Char>()
        CharRange(char, char)
    }

    "upToRange".sequence<CharRange> {
        CharRange(Char.MIN_VALUE, this[1].payload())
    }

    "atLeastRange".sequence<CharRange> {
        CharRange(this[0].payload(), Char.MAX_VALUE)
    }

    "catchAll"<CharRange> {
        CharRange(Char.MIN_VALUE, Char.MAX_VALUE)
    }

    "char".default()

    "escape".sequence<Char> {
        switchAt(1) {
            switchEscapesList.single { it.second == substring.single() }.first
        }
    }

    skipNothing()
}