package com.joutak.acerace.arenas

data class Arena(
    val worldName: String,
) {
    private var state = ArenaState.READY

    fun getState(): ArenaState = state

    fun setState(state: ArenaState) {
        this.state = state
    }
}
