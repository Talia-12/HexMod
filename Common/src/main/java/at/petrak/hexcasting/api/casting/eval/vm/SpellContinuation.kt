package at.petrak.hexcasting.api.casting.eval.vm

import at.petrak.hexcasting.api.utils.NBTBuilder
import at.petrak.hexcasting.api.utils.asCompound
import at.petrak.hexcasting.api.utils.getList
import net.minecraft.client.gui.Font
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.FormattedCharSequence

/**
 * A continuation during the execution of a spell.
 */
sealed interface SpellContinuation {
    object Done : SpellContinuation

    data class NotDone(val frame: ContinuationFrame, val next: SpellContinuation) : SpellContinuation

    fun pushFrame(frame: ContinuationFrame): SpellContinuation = NotDone(frame, this)

    fun serializeToNBT() = NBTBuilder {
        TAG_FRAME %= list(getNBTFrames())
    }
    fun getNBTFrames(): List<CompoundTag> {
        var self = this
        val frames = mutableListOf<CompoundTag>()
        while (self is NotDone) {
            frames.add(ContinuationFrame.toNBT(self.frame))
            self = self.next
        }
        return frames
    }
    companion object {
        const val TAG_FRAME = "frame"

        @JvmStatic
        fun fromNBT(nbt: CompoundTag, world: ServerLevel): SpellContinuation {
            val frames = nbt.getList(TAG_FRAME, Tag.TAG_COMPOUND)
            var result: SpellContinuation = Done
            for (frame in frames.asReversed()) {
                if (frame is CompoundTag) {
                    result = result.pushFrame(ContinuationFrame.fromNBT(frame, world))
                }
            }
            return result
        }

        @JvmStatic
        fun getDisplayWithMaxWidth(nbt: CompoundTag, expanded: List<Boolean>, width: Int, font: Font): List<FormattedCharSequence> {
            val outList = mutableListOf<FormattedCharSequence>()

            val frames = nbt.getList(TAG_FRAME, Tag.TAG_COMPOUND)
            for ((frame, expanded) in (frames.reversed().zip(expanded))) {
                if (expanded) outList.addAll(ContinuationFrame.displayExpanded(frame.asCompound, width, font))
                else outList.add(ContinuationFrame.displayOneLine(frame.asCompound, width, font))
            }

            return outList
        }
    }
}
