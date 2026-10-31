package com.jttw.engine.network.s2c;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.SomersaultTravel.Destination;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server to client: open Somersault Anywhere's menu, with what it offers now (see SomersaultTravel#menu) and the realms. */
public class OpenSomersaultMenuPacket implements CustomPacketPayload {

    public static final Type<OpenSomersaultMenuPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "open_somersault_menu"));
    public static final StreamCodec<FriendlyByteBuf, OpenSomersaultMenuPacket> STREAM_CODEC = StreamCodec.of(OpenSomersaultMenuPacket::encode, OpenSomersaultMenuPacket::decode);

    public final List<Destination> destinations;
    public final List<String> realms;

    public OpenSomersaultMenuPacket(List<Destination> destinations, List<String> realms) {
        this.destinations = destinations;
        this.realms = realms;
    }

    @Override
    public Type<OpenSomersaultMenuPacket> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, OpenSomersaultMenuPacket pkt) {
        buf.writeVarInt(pkt.destinations.size());
        for (Destination d : pkt.destinations) {
            buf.writeUtf(d.id());
            buf.writeUtf(d.group());
            buf.writeUtf(d.label());
        }
        buf.writeVarInt(pkt.realms.size());
        for (String r : pkt.realms) buf.writeUtf(r);
    }

    private static OpenSomersaultMenuPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Destination> ds = new ArrayList<>(n);
        for (int i = 0; i < n; i++) ds.add(new Destination(buf.readUtf(), buf.readUtf(), buf.readUtf()));
        int m = buf.readVarInt();
        List<String> realms = new ArrayList<>(m);
        for (int i = 0; i < m; i++) realms.add(buf.readUtf());
        return new OpenSomersaultMenuPacket(ds, realms);
    }

    public static void handle(OpenSomersaultMenuPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(OpenSomersaultMenuPacket pkt) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new com.jttw.engine.client.gui.SomersaultScreen(pkt.destinations, pkt.realms));
    }
}
