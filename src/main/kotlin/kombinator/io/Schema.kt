package kombinator.io

import io.github.aeckar.kanary.schema
import kombinator.ranges.MutableRangeVector
import kombinator.ranges.RangeVector
import kombinator.symbols.*
import kombinator.symbols.namedRead

internal val SCHEMA = schema(threadSafe = true) {
    define<Symbol> {
        write { write(it.id) }
    }

    define<Option> {
        namedRead { Option(read()) }
        write { write(it.inner) }
    }
    define<Repetition> {
        namedRead { Repetition(read()) }
        write { write(it.inner) }
    }
    define<Junction> {
        namedRead { Junction(read()) }
        write { write(it.members) }
    }
    define<RepeatOption> {
        namedRead { RepeatOption(read()) };
        write { write(it.equivalent) }
    }
    define<Sequence> {
        namedRead { Sequence(read()) };
        write { write(it.members) }
    }
    define<Text> {
        namedRead { Text(read()) };
        write { write(it.acceptable) }
    }
    define<Character> {
        namedRead { Character(read()) };
        write { write(it.acceptable) }
    }
    define<Switch> {
        namedRead { Switch(read()) };
        write { write(it.ranges) }
    }

    define<ImplicitSymbol> {

    }


    define<RangeVector> {
        fallback read RangeVector.Protocol
    }
    define<MutableRangeVector> {
        static write MutableRangeVector.Protocol
    }
}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class Serialized