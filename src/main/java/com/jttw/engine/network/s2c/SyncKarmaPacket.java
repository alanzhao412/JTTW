package com.jttw.engine.network.s2c;

import com.jttw.engine.world.morality.Morality;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A player's karma, for their character screen. */
public class SyncKarmaPacket {

    private final int karma;

    public SyncKarmaPacket(int karma) {
        this.karma = karma;
    }

    public static void encode(SyncKarmaPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.karma);
    }

    public static SyncKarmaPacket decode(FriendlyByteBuf buf) {
        return new SyncKarmaPacket(buf.readVarInt());
    }

    public static void handle(SyncKarmaPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncKarmaPacket pkt) {
        if (Minecraft.getInstance().player != null) Morality.set(Minecraft.getInstance().player, pkt.karma);
    }
}
