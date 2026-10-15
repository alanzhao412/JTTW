package com.jttw.engine.network.c2s;

import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.character.dragon.DragonAbilityHandler;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Sent client → server when the Dragon player scrolls to select a dimension.
 * The scroll direction (+1 or -1) cycles through the available dimensions.
 */
public class DimensionSelectPacket implements CustomPacketPayload {

    public static final Type<DimensionSelectPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "dimension_select"));
    public static final StreamCodec<FriendlyByteBuf, DimensionSelectPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), DimensionSelectPacket::decode);

    @Override
    public Type<DimensionSelectPacket> type() {
        return TYPE;
    }

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

    public static void handle(DimensionSelectPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            // Validate: must be a Dragon line Divine Beast
            if (PlayerLevel.getProgressionClass(player) != ProgressionClass.DIVINE_BEAST) return;
            DivineBeastLine line = PlayerLevel.getBeastLine(player);
            if (line != DivineBeastLine.WATER) return;

            DragonAbilityHandler.scrollDimension(player, pkt.direction);
        });
    }
}
