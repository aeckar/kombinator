@file:Suppress("NOTHING_TO_INLINE")
package kombinator.util

import kombinator.ContextFreeToken

internal object DebugContext {
    var isEnabled = true
    inline operator fun String.unaryPlus() {
        if (isEnabled) {
            Indent()
            println(this)
        }
    }
}

internal object Indent {
    var count = 0
    operator fun invoke() = print("  ".repeat(count))
}

internal inline fun debug(block: DebugContext.() -> Unit) {
    block(DebugContext)
}

internal inline fun <T> debugIgnore(block: () -> T): T {
    DebugContext.isEnabled = false
    return block().also { DebugContext.isEnabled = true }
}

internal inline fun debugWithIndent(block: DebugContext.() -> Unit) {
    ++Indent.count
    block(DebugContext)
}

internal inline fun ContextFreeToken.debugStatus(): ContextFreeToken {
    if (this === ContextFreeToken.NOTHING) {
        debug { + "Failed to match token" }
    } else {
        debug { + "Successfully matched token ($origin)" }
    }
    debug { + "<-" }
    --Indent.count
    return this
}
