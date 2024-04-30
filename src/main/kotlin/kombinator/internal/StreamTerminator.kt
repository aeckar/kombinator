package kombinator.internal

internal object StreamTerminator : Throwable() {
    private fun readResolve(): Any = StreamTerminator
}