package com.jttw.engine.network.c2s;

import com.jttw.engine.transformation.BlockForms;
import com.jttw.engine.transformation.TransformationData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Sent client → server when a sign-form player finishes writing their sign. */
public class SetSignTextPacket {

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

    public static void handle(SetSignTextPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) ctx.get().enqueueWork(() -> BlockForms.setSignText(sender, msg.lines));
        ctx.get().setPacketHandled(true);
    }
}
