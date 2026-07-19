package com.joutak.acerace.checkpoints

sealed class CheckpointResult {
    object Nothing : CheckpointResult()
    object NotParticipating : CheckpointResult()

    object RaceFinished : CheckpointResult()

    data class LapCompleted(
        val lapDone: Int,
        val lapsTotal: Int
    ) : CheckpointResult()

    data class CheckpointPassed(
        val checkpointIndex: Int,
        val nextRequired: Int
    ) : CheckpointResult()

    data class WrongOrder(
        val attempted: Int,
        val required: Int
    ) : CheckpointResult()
}