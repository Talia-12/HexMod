package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;


import static at.petrak.hexcasting.api.HexAPI.modLoc;

public record MsgDebuggerActionC2S(InteractionHand handUsed, DebugType type) implements IMessage {
    public static final ResourceLocation ID = modLoc("dbg_cs");

    public enum DebugType {
        StepOut,
        StepOver,
        StepInto,
        StepIntoSkipParens
    }

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    public static MsgDebuggerActionC2S deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        var hand = buf.readEnum(InteractionHand.class);
        var type = buf.readEnum(DebugType.class);

        return new MsgDebuggerActionC2S(hand, type);
    }


    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeEnum(handUsed);
        buf.writeEnum(type);
    }

    public void handle(MinecraftServer server, ServerPlayer sender) {
        server.execute(() -> StaffCastEnv.handleDebugActionOnServer(sender, this));
    }
}
