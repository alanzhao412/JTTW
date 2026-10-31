package com.jttw.engine.network.s2c;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Tells the players near it that Wukong's Fiery Golden Eyes fired
 * ({@link com.jttw.engine.character.wukong.EyeLaser}): from whose eyes, from where to where it
 * reached, at which output and how wide, whether something stopped it, whether it was the
 * Fighting Buddha's, and a sample of the blocks it burnt away (their place and kind), for the
 * glowing debris. Sent before the blocks go, so the client still has them.
 */
public class EyeLaserBeamPacket implements CustomPacketPayload {

    public static final Type<EyeLaserBeamPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "eye_laser_beam"));
    public static final StreamCodec<FriendlyByteBuf, EyeLaserBeamPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> pkt.encode(buf), EyeLaserBeamPacket::decode);

    /** A block it burnt away: where, and its state's id. */
    public record Debris(BlockPos pos, int state) {}

    public final int owner;
    public final Vec3 from, to;
    public final int output;
    public final float radius;
    public final boolean stopped, buddha;
    public final List<Debris> debris;

    public EyeLaserBeamPacket(int owner, Vec3 from, Vec3 to, int output, float radius, boolean stopped, boolean buddha, List<Debris> debris) {
        this.owner = owner;
        this.from = from;
        this.to = to;
        this.output = output;
        this.radius = radius;
        this.stopped = stopped;
        this.buddha = buddha;
        this.debris = List.copyOf(debris);
    }

    @Override
    public Type<EyeLaserBeamPacket> type() {
        return TYPE;
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(owner);
        buf.writeDouble(from.x); buf.writeDouble(from.y); buf.writeDouble(from.z);
        buf.writeDouble(to.x); buf.writeDouble(to.y); buf.writeDouble(to.z);
        buf.writeByte(output);
        buf.writeFloat(radius);
        buf.writeByte((stopped ? 1 : 0) | (buddha ? 2 : 0));
        buf.writeVarInt(debris.size());
        for (Debris d : debris) {
            buf.writeBlockPos(d.pos());
            buf.writeVarInt(d.state());
        }
    }

    private static EyeLaserBeamPacket decode(FriendlyByteBuf buf) {
        int owner = buf.readVarInt();
        Vec3 from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 to = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int output = buf.readByte();
        float radius = Math.max(0, Math.min(8, buf.readFloat()));
        int flags = buf.readByte();
        int n = Math.min(buf.readVarInt(), 256);
        List<Debris> debris = new ArrayList<>(n);
        for (int i = 0; i < n; i++) debris.add(new Debris(buf.readBlockPos(), buf.readVarInt()));
        return new EyeLaserBeamPacket(owner, from, to, output, radius, (flags & 1) != 0, (flags & 2) != 0, debris);
    }

    public static void handle(EyeLaserBeamPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(EyeLaserBeamPacket pkt) {
        com.jttw.engine.client.renderer.EyeLaserRenderer.add(pkt);
    }
}
