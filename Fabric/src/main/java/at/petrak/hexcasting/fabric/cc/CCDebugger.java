package at.petrak.hexcasting.fabric.cc;

import at.petrak.hexcasting.api.casting.eval.debug.DebugState;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class CCDebugger implements Component {
    public static final String TAG_DEBUG_CONTINUATION = "hexcasting:debug_continuation";
    public static final String TAG_DEBUG_INFO = "hexcasting:debug_info";

    private final ServerPlayer owner;
    private CompoundTag lazyLoadedContinuation = new CompoundTag();
    private CompoundTag lazyLoadedInfo = new CompoundTag();

    public CCDebugger(ServerPlayer owner) {
        this.owner = owner;
    }

    public @Nullable DebugState getState() {
        if (lazyLoadedContinuation.isEmpty())
            return null;

        var cont = SpellContinuation.fromNBT(lazyLoadedContinuation, owner.getLevel());
        var info = CastingVM.TempControllerInfo.deserializeFromNbt(lazyLoadedInfo, owner.getLevel());
        return new DebugState(cont, info);
    }

    public void setState(@Nullable DebugState state) {
        if (state == null) {
            lazyLoadedContinuation = new CompoundTag();
            lazyLoadedInfo = new CompoundTag();
            return;
        }

        lazyLoadedContinuation = state.getContinuation().serializeToNBT();
        lazyLoadedInfo = state.getTempControllerInfo().serializeToNbt();
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.lazyLoadedContinuation = tag.getCompound(TAG_DEBUG_CONTINUATION);
        this.lazyLoadedInfo = tag.getCompound(TAG_DEBUG_INFO);
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put(TAG_DEBUG_CONTINUATION, this.lazyLoadedContinuation);
        tag.put(TAG_DEBUG_INFO, this.lazyLoadedInfo);
    }
}
