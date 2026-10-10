package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells the players near it that the Saurian Demon King's water is showing: a lash (a rope of
 * water from his hand to a point, or to a foe it caught) or a whirlpool (on the ground, so wide,
 * for so long). The client draws them; particles alone are too thin to read.
 */
public class WaterFxPacket {

    public static final int LASH = 0, WHIRLPOOL = 1;

    private final int kind, owner, caught, ticks;
    private final Vec3 at;
    private final float radius;

    public WaterFxPacket(int kind, int owner, int caught, Vec3 at, float radius, int ticks) {
        this.kind = kind;
        this.owner = owner;
        this.caught = caught;
        this.at = at;
        this.radius = radius;
        this.ticks = ticks;
    }

    public static WaterFxPacket lash(int owner, int caught, Vec3 to) {
        return new WaterFxPacket(LASH, owner, caught, to, 0, 8);
    }

    public static WaterFxPacket whirlpool(int owner, Vec3 at, float radius, int ticks) {
        return new WaterFxPacket(WHIRLPOOL, owner, -1, at, radius, ticks);
    }

    public static void encode(WaterFxPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.kind);
        buf.writeVarInt(pkt.owner);
        buf.writeVarInt(pkt.caught);
        buf.writeDouble(pkt.at.x);
        buf.writeDouble(pkt.at.y);
        buf.writeDouble(pkt.at.z);
        buf.writeFloat(pkt.radius);
        buf.writeVarInt(pkt.ticks);
    }

    public static WaterFxPacket decode(FriendlyByteBuf buf) {
        int kind = buf.readByte(), owner = buf.readVarInt(), caught = buf.readVarInt();
        Vec3 at = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float radius = Math.max(0, Math.min(16, buf.readFloat()));
        int ticks = Math.max(0, Math.min(400, buf.readVarInt()));
        return new WaterFxPacket(kind, owner, caught, at, radius, ticks);
    }

    public static void handle(WaterFxPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(WaterFxPacket pkt) {
        if (pkt.kind == LASH) com.jttw.engine.client.renderer.WaterFxRenderer.lash(pkt.owner, pkt.caught, pkt.at, pkt.ticks);
        else if (pkt.kind == WHIRLPOOL) com.jttw.engine.client.renderer.WaterFxRenderer.whirlpool(pkt.at, pkt.radius, pkt.ticks);
    }
}
