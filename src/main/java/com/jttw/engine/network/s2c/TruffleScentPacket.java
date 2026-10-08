package com.jttw.engine.network.s2c;

import com.jttw.engine.character.marshal.MarshalAbilityHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * What the Marshal's Truffle Snout smelt: creatures by entity id and blocks with the colour to
 * outline them in, shown through walls for {@code ticks}.
 */
public class TruffleScentPacket {

    private final int ticks;
    private final int[] entityIds;
    private final long[] blocks;
    private final int[] colours;

    public TruffleScentPacket(int ticks, int[] entityIds, long[] blocks, int[] colours) {
        if (blocks.length != colours.length) throw new IllegalArgumentException("a colour per block");
        this.ticks = ticks;
        this.entityIds = entityIds;
        this.blocks = blocks;
        this.colours = colours;
    }

    public static TruffleScentPacket of(MarshalAbilityHandler.Scent scent) {
        return new TruffleScentPacket(scent.ticks(),
                scent.entityIds().stream().mapToInt(Integer::intValue).toArray(),
                scent.blocks().stream().mapToLong(BlockPos::asLong).toArray(),
                scent.colours().stream().mapToInt(Integer::intValue).toArray());
    }

    public static void encode(TruffleScentPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.ticks);
        buf.writeVarIntArray(pkt.entityIds);
        buf.writeLongArray(pkt.blocks);
        buf.writeVarIntArray(pkt.colours);
    }

    public static TruffleScentPacket decode(FriendlyByteBuf buf) {
        int ticks = buf.readVarInt();
        int[] ids = buf.readVarIntArray(MarshalAbilityHandler.SCENT_CAP);
        long[] blocks = buf.readLongArray(null, MarshalAbilityHandler.SCENT_CAP);
        int[] colours = buf.readVarIntArray(MarshalAbilityHandler.SCENT_CAP);
        if (colours.length != blocks.length) colours = new int[blocks.length];
        return new TruffleScentPacket(ticks, ids, blocks, colours);
    }

    public static void handle(TruffleScentPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(TruffleScentPacket pkt) {
        com.jttw.engine.client.renderer.TruffleScentRenderer.show(pkt.ticks, pkt.entityIds, pkt.blocks, pkt.colours);
    }
}
