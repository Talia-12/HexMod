package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.api.casting.eval.DebugClientView;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import at.petrak.hexcasting.common.lib.HexSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public record MsgDebuggerActionS2C(DebugClientView info) implements IMessage {
    public static final ResourceLocation ID = modLoc("dbg_sc");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    public static MsgDebuggerActionS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);

        var isDebugComplete = buf.readBoolean();
        var didDebugError = buf.readBoolean();
        var isStackEmpty = buf.readBoolean();

        var stack = buf.readList(FriendlyByteBuf::readNbt);
        var raven = buf.readOptional(FriendlyByteBuf::readNbt).orElse(null);
        var debugged = buf.readOptional(FriendlyByteBuf::readNbt).orElse(null);

        return new MsgDebuggerActionS2C(
                new DebugClientView(isDebugComplete, didDebugError, isStackEmpty, stack, raven, debugged)
        );
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeBoolean(this.info.isDebugComplete());
        buf.writeBoolean(this.info.getDidDebugError());
        buf.writeBoolean(this.info.isStackClear());

        buf.writeCollection(this.info.getStackDescs(), FriendlyByteBuf::writeNbt);
        buf.writeOptional(Optional.ofNullable(this.info.getRavenmind()), FriendlyByteBuf::writeNbt);
        buf.writeOptional(Optional.ofNullable(this.info.getDebuggedContinuation()), FriendlyByteBuf::writeNbt);
    }

    public static void handle(MsgDebuggerActionS2C self) {
        Minecraft.getInstance().execute(() -> {
            var mc = Minecraft.getInstance();
            if (self.info.isDebugComplete() && self.info().isStackClear()) {
                // don't pay attention to the screen, so it also stops when we die
                mc.getSoundManager().stop(HexSounds.CASTING_AMBIANCE.getLocation(), null);
            }
            var screen = Minecraft.getInstance().screen;
            if (screen instanceof GuiSpellcasting spellGui) {
                spellGui.recvServerDebugUpdate(self.info());
            }
        });
    }
}
