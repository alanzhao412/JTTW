package com.jttw.engine.network.s2c;

import com.jttw.engine.JTTWEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to client: how a player stands as the Victorious Fighting Buddha (see FightingBuddha#packed):
 * bit 0 ascended (drawn as the Buddha), bit 1 vanished (told only to himself; no one else's game knows he is there).
 */
public class FightingBuddhaPacket implements CustomPacketPayload {

    public static final Type<FightingBuddhaPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "fighting_buddha"));
    public static final StreamCodec<FriendlyByteBuf, FightingBuddhaPacket> STREAM_CODEC =
            StreamCodec.of((buf, pkt) -> { buf.writeVarInt(pkt.entityId); buf.writeByte(pkt.packed); },
                    buf -> new FightingBuddhaPacket(buf.readVarInt(), buf.readByte()));

    private final int entityId, packed;

    public FightingBuddhaPacket(int entityId, int packed) {
        this.entityId = entityId;
        this.packed = packed;
    }

    @Override
    public Type<FightingBuddhaPacket> type() {
        return TYPE;
    }

    public static void handle(FightingBuddhaPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(FightingBuddhaPacket pkt) {
        com.jttw.engine.client.FightingBuddhaClient.set(pkt.entityId, pkt.packed);
    }
}
