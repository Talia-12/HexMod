package at.petrak.hexcasting.api.casting.castables

import at.petrak.hexcasting.api.casting.arithmetic.engine.NoOperatorCandidatesException
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidOperatorArgs
import at.petrak.hexcasting.common.lib.hex.HexArithmetics
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import java.util.*
import java.util.function.Consumer

data class OperationAction(val pattern: HexPattern) : Action {
    override fun operate(env: CastingEnvironment, image: CastingImage, continuation: SpellContinuation): OperationResult {
        val stackList = image.stack
        val stack = Stack<Iota>()
        stack.addAll(stackList)
        val startingLength = stackList.size
        return try {
            val ret: Iterable<Iota> = HexArithmetics.ENGINE.run(pattern, stack, startingLength)
            ret.forEach(Consumer { e: Iota -> stack.add(e) })
            val image2 = image.copy(stack = stack, opsConsumed = image.opsConsumed + 1)
            OperationResult(image2, listOf(), continuation, HexEvalSounds.NORMAL_EXECUTE)
        } catch (e: NoOperatorCandidatesException) {
            throw MishapInvalidOperatorArgs(e.args, e.pattern)
        }
    }
}