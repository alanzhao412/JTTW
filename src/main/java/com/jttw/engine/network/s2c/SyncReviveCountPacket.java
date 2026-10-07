package com.jttw.engine.network.s2c;

import com.jttw.engine.character.phoenix.PhoenixReviveData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs Phoenix revive charge count from server to client for HUD display.
 */
public class SyncReviveCountPacket {

    private final int remaining;

    public SyncReviveCountPacket(int remaining) {
        this.remaining = remaining;
    }

    public static void encode(SyncReviveCountPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.remaining);
    }

    public static SyncReviveCountPacket decode(FriendlyByteBuf buf) {
        return new SyncReviveCountPacket(buf.readInt());
    }

    public static void handle(SyncReviveCountPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncReviveCountPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        PhoenixReviveData.setRevives(player, pkt.remaining);
    }
}
