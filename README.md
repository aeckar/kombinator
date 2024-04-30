# Kombinator
## Parser-Combinator DSL for Kotlin

---

## Overview

## Examples

```kotlin
import kombinator.Grammar
import kombinator.grammar


object MyGrammar {
    class MutableState : Grammar.MutableState() {
        // ... mutable variables
    }
    
    val grammar = grammar<String,MutableState>( /* grammar definition */"""
        // TODO populate
    """.trimIndent(), "myCache.kgc") {
        ""  // TODO populate
    }
}
```

## Token Types



## Definition Notation

The notation used to define grammars is a subset of [ANTLR](https://github.com/antlr/antlr4)-style [EBNF](https://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_form).


```
switch: "[" ((("-" char)? (char | (char "-" char))* (char "-")?) | "-") "]";
```
```
switch 
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
```
