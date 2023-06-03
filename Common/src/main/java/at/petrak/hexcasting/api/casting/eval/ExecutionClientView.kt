package at.petrak.hexcasting.api.casting.eval

import net.minecraft.nbt.CompoundTag

/**
 * Information sent back to the client
 */
data class ExecutionClientView(
    val isStackClear: Boolean,
    val resolutionType: ResolvedPatternType,

    // These must be tags so the wrapping of the text can happen on the client
    // otherwise we don't know when to stop rendering
    val stackDescs: List<CompoundTag>,
    val ravenmind: CompoundTag?,
)

/**
 * Information sent back to the client when running the debugger
 */
data class DebugClientView(
    val isDebugComplete: Boolean,
    val didDebugError: Boolean,
    val isStackClear: Boolean,

    // These must be tags so the wrapping of the text can happen on the client
    // otherwise we don't know when to stop rendering
    val stackDescs: List<CompoundTag>,
    val ravenmind: CompoundTag?,
    val debuggedContinuation: CompoundTag?
)