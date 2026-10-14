package com.example.domain.focus

import com.example.data.local.entity.FocusRunEntity
import kotlin.math.max

enum class FocusPhaseType {
    FOCUS,
    BREAK
}

data class ReconciledFocusState(
    val phase: FocusPhaseType,
    val cycleNumber: Int,
    val phaseStartTime: Long,
    val phaseEndTime: Long,
    val phaseDurationMillis: Long,
    val remainingMillis: Long,
    val progressFraction: Float,
    val totalFocusSecondsCompleted: Long,
    val totalBreakSecondsCompleted: Long,
    val isWarningActive: Boolean
)

object FocusEngine {

    /**
     * Reconciles current focus phase, cycle, and remaining countdown deterministically from timestamps.
     * Guarantees zero alarm cascades and instant correct state recovery after reboots or process death.
     */
    fun reconcile(
        run: FocusRunEntity,
        now: Long = System.currentTimeMillis()
    ): ReconciledFocusState {
        val focusMs = max(1, run.focusDurationMinutes) * 60 * 1000L
        val breakMs = max(1, run.breakDurationMinutes) * 60 * 1000L
        val cycleMs = focusMs + breakMs
        val warningMs = max(0, run.warningSeconds) * 1000L

        val elapsed = max(0L, now - run.startTime)
        val cycleIndex = elapsed / cycleMs
        val cycleOffset = elapsed % cycleMs

        val (phase, cycleNumber, phaseStartTime, phaseEndTime, phaseDuration) = if (cycleOffset < focusMs) {
            val start = run.startTime + (cycleIndex * cycleMs)
            val end = start + focusMs
            ReconciledFocusStateTuple(
                phase = FocusPhaseType.FOCUS,
                cycleNumber = (cycleIndex + 1).toInt(),
                phaseStartTime = start,
                phaseEndTime = end,
                phaseDurationMillis = focusMs
            )
        } else {
            val start = run.startTime + (cycleIndex * cycleMs) + focusMs
            val end = start + breakMs
            ReconciledFocusStateTuple(
                phase = FocusPhaseType.BREAK,
                cycleNumber = (cycleIndex + 1).toInt(),
                phaseStartTime = start,
                phaseEndTime = end,
                phaseDurationMillis = breakMs
            )
        }

        val remainingMillis = max(0L, phaseEndTime - now)
        val elapsedInPhase = max(0L, now - phaseStartTime)
        val progressFraction = if (phaseDuration > 0) {
            (elapsedInPhase.toFloat() / phaseDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }

        // Calculate cumulative seconds completed
        val fullCycles = cycleIndex
        val completedFocusFromFull = fullCycles * (run.focusDurationMinutes * 60L)
        val completedBreakFromFull = fullCycles * (run.breakDurationMinutes * 60L)

        val (curFocusSecs, curBreakSecs) = if (phase == FocusPhaseType.FOCUS) {
            Pair(elapsedInPhase / 1000L, 0L)
        } else {
            Pair(run.focusDurationMinutes * 60L, elapsedInPhase / 1000L)
        }

        val totalFocusSeconds = completedFocusFromFull + curFocusSecs
        val totalBreakSeconds = completedBreakFromFull + curBreakSecs

        // 30s warning is active if within warning window before phaseEndTime
        val isWarningActive = warningMs > 0 && remainingMillis in 1..warningMs

        return ReconciledFocusState(
            phase = phase,
            cycleNumber = cycleNumber,
            phaseStartTime = phaseStartTime,
            phaseEndTime = phaseEndTime,
            phaseDurationMillis = phaseDuration,
            remainingMillis = remainingMillis,
            progressFraction = progressFraction,
            totalFocusSecondsCompleted = totalFocusSeconds,
            totalBreakSecondsCompleted = totalBreakSeconds,
            isWarningActive = isWarningActive
        )
    }

    private data class ReconciledFocusStateTuple(
        val phase: FocusPhaseType,
        val cycleNumber: Int,
        val phaseStartTime: Long,
        val phaseEndTime: Long,
        val phaseDurationMillis: Long
    )
}
