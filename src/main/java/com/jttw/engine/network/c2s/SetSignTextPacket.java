package com.jttw.engine.network.c2s;

import com.jttw.engine.transformation.BlockForms;
import com.jttw.engine.transformation.TransformationData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Sent client → server when a sign-form player finishes writing their sign. */
public class SetSignTextPacket implements CustomPacketPayload {

    public static final Type<SetSignTextPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "set_sign_text"));
    public static final StreamCodec<FriendlyByteBuf, SetSignTextPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SetSignTextPacket::decode);

    @Override
    public Type<SetSignTextPacket> type() {
        return TYPE;
    }

    private final List<String> lines;

    public SetSignTextPacket(List<String> lines) {
        this.lines = List.copyOf(lines);
    }

    public List<String> lines() {
        return lines;
    }

    public static void encode(SetSignTextPacket msg, FriendlyByteBuf buf) {
        int n = Math.min(TransformationData.SIGN_LINES, msg.lines.size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) buf.writeUtf(msg.lines.get(i), TransformationData.SIGN_LINE_LENGTH);
    }

    public static SetSignTextPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > TransformationData.SIGN_LINES) throw new IllegalArgumentException("sign lines: " + n);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < n; i++) lines.add(buf.readUtf(TransformationData.SIGN_LINE_LENGTH));
        return new SetSignTextPacket(lines);
    }

    public static void handle(SetSignTextPacket msg, IPayloadContext ctx) {
        ServerPlayer sender = (ServerPlayer) ctx.player();
        if (sender != null) ctx.enqueueWork(() -> BlockForms.setSignText(sender, msg.lines));
    }
}
