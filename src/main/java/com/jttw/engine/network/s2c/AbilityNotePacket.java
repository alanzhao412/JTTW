package com.jttw.engine.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/** A short note on an ability's key (what it holds, say); empty to clear it. */
public class AbilityNotePacket implements CustomPacketPayload {

    public static final Type<AbilityNotePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "ability_note"));
    public static final StreamCodec<FriendlyByteBuf, AbilityNotePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), AbilityNotePacket::decode);

    @Override
    public Type<AbilityNotePacket> type() {
        return TYPE;
    }

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

    public static void handle(AbilityNotePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.jttw.engine.client.ClientAbilityData.setNote(pkt.abilityId, pkt.note));
    }
}
