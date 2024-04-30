package kombinator.internal

import kombinator.*

internal object MetaGrammar {
    class MutableState : Grammar.MutableState() {
        val implicitNamedSymbols = mutableListOf<ImplicitSymbol>()
        val rules = mutableMapOf<String, Symbol>()
    }

    val grammar: Grammar<MutableMap<String,Symbol>,MutableState>

    val definition = """
        ID: [a-zA-Z] [a-zA-Z0-9_]*;
        DIGIT: [0-9];
        QUOTE: "${'"'}";

        symbol: "(" symbol ")" | junction | sequence | multiple | option | ID | switch | character | text;
        escape: "\" ([...] | ("u" DIGIT DIGIT DIGIT DIGIT));
        char: escape | [-];

        switch: "[" ((("-" char)? (char | (char "-" char))* (char "-")?) | "-") "]";
        character: QUOTE char QUOTE;
        text: QUOTE char+ QUOTE;

        rule: ID ":" symbol ";"

        sequence: symbol+;
        junction: symbol ("|" symbol)+;
        multiple: symbol "+";
        option: symbol "?";
        star: symbol "*";

        start: rule+;
        skip: ([\u0000-\u0009\u000B-\u0001F]+ | "/*"  [-]* "*/" | "//" [-\u0009\u000B-])+;
    """.trimIndent()

    val builderContext: Grammar<MutableMap<String,Symbol>,MutableState>.BuilderContext.() -> Unit = {
        "escape".sequence {
            when (val c = substring[1]) {
                'u' -> substring.substring(2..5).toInt(8).toChar()
                else -> characterEscapes[c]
            }
        }

        "char".junction {
            if (match.id == "escape") match.payload() else substring.single()
        }

        "switch".sequence {
            if (substring.length == 3) {    // .substring == "[-]"
                return@sequence AnyCharacterSymbol()
            }
            val lowerBounds = MutableIntVector()
            val upperBounds = MutableIntVector()
            junctionAt(1).sequence {    // -> <switch body>
                optionAt(1).ifPresent { // -> <up-to>
                    lowerBounds += Char.MIN_VALUE
                    upperBounds += sequence()[1].payload<Char>()
                }
                starAt(2).ifPresent { // -> <single character/range>
                    for (j in junctions()) {
                        val payload: Any? = j.match.payload()
                        if (payload is Char) {
                            lowerBounds += payload
                            upperBounds += payload
                        } else {
                            j.sequence {    // -> <range>
                                lowerBounds += this[0].payload<Char>()
                                upperBounds += this[2].payload<Char>()
                            }
                        }
                    }
                }
                optionAt(3).ifPresent { // -> <at-least>
                    lowerBounds += sequence()[0].payload<Char>()
                    upperBounds += Char.MAX_VALUE
                }
            }
            SwitchSymbol(lowerBounds, upperBounds)
        }

        "character".sequence {
            CharSymbol(this[1].payload())
        }

        "text".sequence {
            StringSymbol(this[1].substring)
        }

        "rule".sequence { mutableState ->
            val id = sequenceAt(0).substring
            val symbol = junctionAt(2) {    // -> symbol
                if (ordinal() == 5) {   // -> SequenceSymbol.ID
                    raise("Delegation to another named symbol is forbidden", mutableState)
                }
                val rhs = match.payload<Symbol>()
                if (rhs.isTerminal()) {    // Delegation to literal
                    rhs.apply { this.id = id }
                } else {
                    ImplicitSymbol(id).apply { reference = rhs }
                }
            }
            mutableState.implicitNamedSymbols
                .indexOfFirst { it.id == id }
                .takeIf { it != -1 }
                ?.let {
                    val reference = if (symbol is ImplicitSymbol) symbol.reference else symbol
                    mutableState.implicitNamedSymbols[it].reference = reference
                    mutableState.implicitNamedSymbols.removeAt(it)
                }
            id to symbol
        }

        "sequence".multiple {
            val symbols = ArrayList<Symbol>(matches.size)
            matches.forEach { symbols.add(it.payload()) }
            SequenceSymbol(members = symbols)
        }

        "junction".sequence {
            val symbols = mutableListOf<Symbol>(this[0].payload())
            multipleAt(1) {
                sequences().forEach { symbols.add(it[1].payload()) }
            }
            JunctionSymbol(members = symbols)
        }

        "multiple".sequence {
            MultipleSymbol(this[0].payload())
        }

        "option".sequence {
            OptionSymbol(this[0].payload())
        }

        "star".sequence {
            StarSymbol(this[0].payload())
        }

        "start".multiple { mutableState ->
            if (mutableState.implicitNamedSymbols.any()) {
                raise("No definitions found for implicit symbols: ${mutableState.implicitNamedSymbols}", mutableState)
            }
            mutableState.rules
        }.start()

        "skip".skip()

        "symbol".junction { mutableState ->
            when (ordinal()) {
                0 -> {  // :: (<symbol>)
                    sequence {
                        this[1].payload()
                    }
                }
                5 -> {  // :: <id>
                    val id = match.substring
                    mutableState.rules.getOrElse(id) {
                        if (mutableState.implicitNamedSymbols.none { implicit -> implicit.id == id }) {
                            mutableState.implicitNamedSymbols += ImplicitSymbol(id)
                        }
                    }
                }
                else -> match.payload()
            }
        }
    }

    val characterEscapes = mapOf(
        't' to '\t',
        'n' to '\n',
        'r' to '\r',
        '"' to '"',
        '-' to '-',
        '\\' to '\\'
    )

    private val rules: Map<String,Symbol>

    private val symbol = ImplicitSymbol("symbol")

    private val escape = SequenceSymbol("escape",
        CharSymbol('\\'),
        JunctionSymbol(
            SwitchSymbol.including('t', 'n', 'r', '-', '\'', '\\'),
            SequenceSymbol(
                CharSymbol('u'),
                SwitchSymbol.DIGIT,
                SwitchSymbol.DIGIT,
                SwitchSymbol.DIGIT,
                SwitchSymbol.DIGIT,
            )
        )
    )

    private val char = JunctionSymbol("char",
        escape,
        AnyCharacterSymbol()
    )

    private val switch = SequenceSymbol("switch",
        CharSymbol('['),
        JunctionSymbol(
            SequenceSymbol(
                OptionSymbol(   // up-to
                    SequenceSymbol(
                        CharSymbol.DASH,
                        char
                    )
                ),
                StarSymbol(
                    JunctionSymbol(
                        char,       // single character
                        SequenceSymbol(   // range
                            char,
                            CharSymbol.DASH,
                            char
                        ),
                    )
                ),
                OptionSymbol(   // at-least
                    SequenceSymbol(
                        char,
                        CharSymbol.DASH
                    )
                ),
            ),
            CharSymbol.DASH  // catch-all
        ),
        CharSymbol(']')
    )

    private val character = SequenceSymbol("character",
        CharSymbol.QUOTE,
        char,
        CharSymbol.QUOTE
    )

    private val text = SequenceSymbol("text",
        CharSymbol.QUOTE,
        MultipleSymbol(char),
        CharSymbol.QUOTE
    )

    private val rule = SequenceSymbol("rule",
        SequenceSymbol.ID,
        CharSymbol(':'),
        symbol,
        CharSymbol(';')
    )

    private val sequence = MultipleSymbol("sequence", symbol)

    private val junction = SequenceSymbol("junction",
        symbol,
        MultipleSymbol(
            SequenceSymbol(
                CharSymbol('|'),
                symbol
            )
        )
    )

    private val multiple = SequenceSymbol("multiple",
        symbol,
        CharSymbol('+')
    )

    private val option = SequenceSymbol("option",
        symbol,
        CharSymbol('?')
    )

    private val star = SequenceSymbol("star",
        symbol,
        CharSymbol('*')
    )

    private val start = MultipleSymbol("start", rule)

    private val skip = MultipleSymbol("skip",
        JunctionSymbol(
            MultipleSymbol(SwitchSymbol(vectorOf('\u0000', '\u000B'), vectorOf('\u0009', '\u001F'))),   // whitespace
            SequenceSymbol(   // inline comment
                StringSymbol("/*"),
                StarSymbol(AnyCharacterSymbol()),
                StringSymbol("*/")
            ),
            SequenceSymbol(
                // line comment
                StringSymbol("//"),
                StarSymbol(SwitchSymbol.excluding('\n')),
            )
        )
    )

    fun rules() = rules.toMutableMap()

    init {
        symbol.reference = JunctionSymbol("symbol",
            SequenceSymbol(
                CharSymbol('('),
                symbol,
                CharSymbol(')')
            ),
            junction,
            sequence,
            multiple,
            option,
            SequenceSymbol.ID,
            switch,
            character,
            text
        )

        rules = mapOf(
            "symbol" to symbol,         "escape" to escape,
            "char" to char,             "switch" to switch,
            "character" to character,   "text" to text,
            "rule" to rule,             "sequence" to sequence,
            "junction" to junction,     "multiple" to multiple,
            "option" to option,         "star" to star,
            "start" to start,           "skip" to skip
        )

        grammar = Grammar<MutableMap<String, Symbol>,MutableState>().apply {
            rules = this@MetaGrammar.rules()
            BuilderContext().apply(builderContext)
        }
    }
}