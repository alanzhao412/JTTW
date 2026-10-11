package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A short note on an ability's key (what it holds, say); empty to clear it. */
public class AbilityNotePacket {

    private final String abilityId;
    private final String note;

    public AbilityNotePacket(String abilityId, String note) {
        this.abilityId = abilityId;
        this.note = note;
    }

    public static void encode(AbilityNotePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.abilityId, 64);
        buf.writeUtf(pkt.note, 16);
    }

    public static AbilityNotePacket decode(FriendlyByteBuf buf) {
        return new AbilityNotePacket(buf.readUtf(64), buf.readUtf(16));
    }

    public static void handle(AbilityNotePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> com.jttw.engine.client.ClientAbilityData.setNote(pkt.abilityId, pkt.note));
        ctx.get().setPacketHandled(true);
    }
}
