package com.jttw.engine.network;

import com.jttw.engine.progression.DivineBeastLine;
import com.jttw.engine.progression.DragonAbilityHandler;
import com.jttw.engine.progression.PlayerLevel;
import com.jttw.engine.progression.ProgressionClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when the Dragon player scrolls to select a dimension.
 * The scroll direction (+1 or -1) cycles through the available dimensions.
 */
public class DimensionSelectPacket {

    private final int direction;

    public DimensionSelectPacket(int direction) {
        this.direction = direction;
    }

    public static void encode(DimensionSelectPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.direction);
    }

    public static DimensionSelectPacket decode(FriendlyByteBuf buf) {
        return new DimensionSelectPacket(buf.readByte());
    }

    public static void handle(DimensionSelectPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Validate: must be a Dragon line Divine Beast
            if (PlayerLevel.getProgressionClass(player) != ProgressionClass.DIVINE_BEAST) return;
            DivineBeastLine line = PlayerLevel.getBeastLine(player);
            if (line != DivineBeastLine.WATER) return;

            DragonAbilityHandler.scrollDimension(player, pkt.direction);
        });
        ctx.get().setPacketHandled(true);
    }
}
