import kombinator.util.grammars.MetaGrammar
import org.junit.jupiter.api.Test

// TODO add toString() to all grammar classes
// TODO add optimizations for same starting, pre-parsed tokens (ex: " in character and text)

class GrammarTest {
    @Test
    fun grammar() {
        kombinator.grammar(MetaGrammar.definition, "testCache", MetaGrammar.builderScope)
            .parse("   rule: \"e\"", MetaGrammar.MutableState())
    }
}