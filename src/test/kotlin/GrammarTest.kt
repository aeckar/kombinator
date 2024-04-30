import kombinator.internal.MetaGrammar
import org.junit.jupiter.api.Test

// TODO add toString() to all grammar classes

class GrammarTest {
    @Test
    fun grammar() {
        kombinator.grammar(MetaGrammar.definition, "testCache", MetaGrammar.builderContext)
            .parse("   rule: \"e\"", MetaGrammar.MutableState())
    }
}