package kombinator.charstreams

internal object StreamTerminator : Throwable() {
    private fun readResolve(): Any = StreamTerminator
}