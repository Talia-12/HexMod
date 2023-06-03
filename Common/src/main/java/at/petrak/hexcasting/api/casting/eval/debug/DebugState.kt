package at.petrak.hexcasting.api.casting.eval.debug

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation

data class DebugState(var continuation: SpellContinuation, var tempControllerInfo: CastingVM.TempControllerInfo)
