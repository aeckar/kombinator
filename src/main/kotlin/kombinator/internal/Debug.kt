package kombinator.internal

inline fun debug(block: () -> Unit) {
    block() // Comment out during debugging
}