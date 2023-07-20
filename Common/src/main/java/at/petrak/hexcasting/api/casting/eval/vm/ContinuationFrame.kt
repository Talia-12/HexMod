package at.petrak.hexcasting.api.casting.eval.vm

import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.common.lib.hex.HexContinuationTypes
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.utils.asCompound
import at.petrak.hexcasting.api.utils.asTextComponent
import at.petrak.hexcasting.api.utils.getList
import at.petrak.hexcasting.api.utils.hasList
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.FormattedCharSequence

/**
 * A single frame of evaluation during the execution of a spell.
 *
 * Specifically, an evaluation will keep a stack of these frames.
 * An evaluation with no meta-eval will consist of a single [Evaluate(rest of the pats)] at all times.
 * When an Eval is invoked, we push Evaluate(pats) to the top of the stack.
 *
 * Evaluation is performed by repeatedly popping the top-most (i.e. innermost) frame from the stack,
 * then evaluating that frame (and possibly allowing it to push other frames (e.g. if it's a Hermes)).
 *
 * Once the stack of frames is empty, there are no more computations to run, so we're done.
 *
 */
interface ContinuationFrame {
    /**
     * Step the evaluation forward once.
     * For Evaluate, this consumes one pattern; for ForEach this queues the next iteration of the outer loop.
     * @return the result of this pattern step
     */
    fun evaluate(continuation: SpellContinuation, level: ServerLevel, harness: CastingVM): CastResult

    /**
     * The OpHalt instruction wants us to "jump to" the END of the nearest meta-eval.
     * In other words, we should consume Evaluate frames until we hit a FinishEval or Thoth frame.
     * @return whether the break should stop here, alongside the new stack state (e.g. for finalizing a Thoth)
     */
    fun breakDownwards(stack: List<Iota>): Pair<Boolean, List<Iota>>

    /**
     * Serializes this frame. Used for things like delays, where we pause execution.
     */
    fun serializeToNBT(): CompoundTag

    /**
     * Return the number of iotas contained inside this frame, used for determining whether it is valid to serialise.
     */
    fun size(): Int

    val type: Type<*>

    interface Type<U : ContinuationFrame> {
        fun deserializeFromNBT(tag: CompoundTag, world: ServerLevel): U?
    }

    companion object {
        /**
         * Takes a tag containing the ContinuationFrame.Type resourcelocation and the serialized continuation frame, and returns
         * the deserialized continuation frame.
         */
        @JvmStatic
        fun fromNBT(tag: CompoundTag, world: ServerLevel): ContinuationFrame {
            val type = getTypeFromTag(tag) ?: return FrameEvaluate(SpellList.LList(0, listOf()), false)

            return (tag.get(HexContinuationTypes.KEY_DATA) as? CompoundTag)?.let { type.deserializeFromNBT(it, world) }
                    ?: FrameEvaluate(SpellList.LList(0, listOf()), false)
        }

        /**
         * Takes a continuation frame and serializes it along with its type.
         */
        @JvmStatic
        fun toNBT(frame: ContinuationFrame): CompoundTag {
            val type = frame.type
            val typeId = HexContinuationTypes.REGISTRY.getKey(type)
                ?: throw IllegalStateException(
                    "Tried to serialize an unregistered continuation type. Continuation: " + frame
                        + " ; Type" + type.javaClass.typeName)

            val data = frame.serializeToNBT()

            val out = CompoundTag()
            out.putString(HexContinuationTypes.KEY_TYPE, typeId.toString())
            out.put(HexContinuationTypes.KEY_DATA, data)
            return out
        }

        /**
         * This method attempts to find the type from the `type` key.
         * See [ContinuationFrame.serializeToNBT] for the storage format.
         *
         * @return `null` if it cannot get the type.
         */
        private fun getTypeFromTag(tag: CompoundTag): Type<*>? {
            if (!tag.contains(HexContinuationTypes.KEY_TYPE, Tag.TAG_STRING.toInt())) {
                return null
            }

            val typeKey = tag.getString(HexContinuationTypes.KEY_TYPE)
            if (!ResourceLocation.isValidResourceLocation(typeKey)) {
                return null
            }

            val typeLoc = ResourceLocation(typeKey)
            return HexContinuationTypes.REGISTRY[typeLoc]
        }

        fun displayOneLine(tag: CompoundTag, width: Int, font: Font): FormattedCharSequence {
            val display = display(tag)
            val splitted = font.split(display, width - font.width("..."))
            return if (splitted.isEmpty()) FormattedCharSequence.EMPTY else if (splitted.size == 1) splitted[0] else {
                val first = splitted[0]
                FormattedCharSequence.fromPair(first,
                        Component.literal("...").withStyle(ChatFormatting.GRAY).visualOrderText)
            }
        }

        @JvmStatic
        fun display(tag: CompoundTag): Component {
            return when (tag.getString("type")) {
                "evaluate" ->  "evaluate".asTextComponent
                "end" -> "end".asTextComponent
                "foreach" -> "foreach".asTextComponent
                else -> Component.translatable("hexcasting.spelldata.unknown").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
            }
        }

        @JvmStatic
        fun displayExpanded(tag: CompoundTag, width: Int, font: Font): List<FormattedCharSequence> {
            val displays = displayExpanded(tag)
            val splitted = displays.map { font.split(it, width - font.width("...")) }.map {
                if (it.isEmpty()) FormattedCharSequence.EMPTY else if (it.size == 1) it[0] else {
                    val first = it[0]
                    FormattedCharSequence.fromPair(first, Component.literal("...").withStyle(ChatFormatting.GRAY).visualOrderText)
                }
            }
            return splitted
        }

        @JvmStatic
        fun displayExpanded(tag: CompoundTag): List<Component> {
            return when (tag.getString("type")) {
                "evaluate" -> listOf(Component.literal("Evaluate ["))
                                .plus(tag.getList("patterns", Tag.TAG_COMPOUND).map { IotaType.getDisplay(it.asCompound) })
                                .plus(Component.literal("]"))
                "end" -> listOf(Component.literal("End"))
                "foreach" -> listOf(Component.literal("Foreach ["))
                        .plus(tag.getList("code", Tag.TAG_COMPOUND).map { IotaType.getDisplay(it.asCompound) })
                        .plus(Component.literal("]"))
                else -> listOf(Component.translatable("hexcasting.spelldata.unknown").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
            }
        }
    }
}
