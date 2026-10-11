package com.jttw.engine.network;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.network.c2s.ActivateAbilityPacket;
import com.jttw.engine.network.c2s.DimensionSelectPacket;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.jttw.engine.network.c2s.SpendSkillPointPacket;
import com.jttw.engine.network.c2s.StaffScalePacket;
import com.jttw.engine.network.c2s.TransmuteItemPacket;
import com.jttw.engine.network.s2c.OpenTransformationMenuPacket;
import com.jttw.engine.network.s2c.SyncAbilitiesPacket;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncEyeOfHeavenPacket;
import com.jttw.engine.network.s2c.SyncGoldenEyesPacket;
import com.jttw.engine.network.s2c.SyncLevelPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.network.s2c.SyncReincarnationPacket;
import com.jttw.engine.network.s2c.SyncReviveCountPacket;
import com.jttw.engine.network.s2c.SyncRingStatePacket;
import com.jttw.engine.network.s2c.SyncSkillTreePacket;
import com.jttw.engine.network.s2c.SyncTransformStatePacket;
import com.jttw.engine.network.s2c.SyncXtqStatsPacket;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.transformation.transmute.TransmutationCategory;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every packet must decode to the same field values it was encoded from, and decoding must
 * read exactly what encoding wrote. A field added to one side only fails here instead of
 * corrupting every packet after it on the wire.
 *
 * <p>SyncTransformationPacket carries an EntityType, which needs Minecraft's registries; the
 * GameTests cover it.
 */
class PacketRoundTripTest {

    private static final UUID PLAYER = UUID.fromString("0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0");

    private static List<Object> samples() {
        return List.of(
                // client -> server
                new ActivateAbilityPacket(0, AbilityAction.USE),
                new ActivateAbilityPacket(3, AbilityAction.STOP),
                new DimensionSelectPacket(-1),
                new DimensionSelectPacket(1),
                new RequestTransformPacket(RequestTransformPacket.Kind.ENTITY, new ResourceLocation("minecraft", "wolf")),
                new RequestTransformPacket(RequestTransformPacket.Kind.OBJECT_ENTITY, new ResourceLocation("minecraft", "minecart")),
                RequestTransformPacket.block(new ResourceLocation("minecraft", "chest")),
                new com.jttw.engine.network.c2s.UseFormPacket(),
                new com.jttw.engine.network.c2s.SetSignTextPacket(List.of("Hello", "", "from a", "sign")),
                new com.jttw.engine.network.c2s.SetSignTextPacket(List.of()),
                new SpendSkillPointPacket("wu_ring_2"),
                new StaffScalePacket(4242, -0.75f),
                new TransmuteItemPacket(TransmutationCategory.COOKED_FOOD, 3),
                // server -> client
                new OpenTransformationMenuPacket(),
                new SyncAbilitiesPacket(PLAYER, List.of(
                        new AbilitySlot("wu_ring", "Ring of Fire", AbilityType.HELD, 3),
                        AbilitySlot.EMPTY,
                        new AbilitySlot("wu_transform", "72 Transformations", AbilityType.MENU, 1),
                        new AbilitySlot("nz_cycle", "Weapon Cycle", AbilityType.TOGGLE, 2))),
                new SyncAbilitiesPacket(PLAYER, List.of()),
                new SyncCooldownPacket(PLAYER, "dr_lightning", 37, 200),
                new SyncEyeOfHeavenPacket(PLAYER, true, 3),
                new SyncGoldenEyesPacket(PLAYER, 2),
                new SyncLevelPacket(PLAYER, 57, 1_234_567_890_123L, "PRIMORDIAL", 3),
                new SyncLevelPacket(PLAYER, 1, 0L, null, 0),
                new SyncQiPacket(12.5f, 400f),
                new SyncReincarnationPacket(PLAYER, true, "eye_of_heaven"),
                new SyncReincarnationPacket(PLAYER, false, ""),
                new SyncReviveCountPacket(2),
                new com.jttw.engine.network.s2c.SyncLeadHeadPacket(2),
                new com.jttw.engine.network.s2c.SyncLeadHeadPacket(-1),
                new com.jttw.engine.network.s2c.SyncStomachPacket(13),
                new com.jttw.engine.network.c2s.StaffTopplePacket(4321),
                new com.jttw.engine.network.s2c.SyncFlightPacket(new com.jttw.engine.flight.FlightProfile(2.75, false, 2.5, true, true)),
                new com.jttw.engine.network.s2c.SyncFlightPacket(new com.jttw.engine.flight.FlightProfile(1.25, true, 1.0, false, false)),
                new com.jttw.engine.network.s2c.SyncStomachPacket(-1),
                new com.jttw.engine.network.s2c.TruffleScentPacket(400, new int[]{7, 301},
                        new long[]{new net.minecraft.core.BlockPos(4, -12, 9).asLong()}, new int[]{0xFF00BBFF}),
                new com.jttw.engine.network.s2c.TruffleScentPacket(200, new int[0], new long[0], new int[0]),
                new SyncRingStatePacket(PLAYER, true),
                new SyncSkillTreePacket(PLAYER, 4, Set.of("wu_ring_1", "wu_ring_2", "wu_clone_1")),
                new SyncSkillTreePacket(PLAYER, 0, Set.of()),
                new SyncTransformStatePacket(true, 2, 600, 1200, 0, 2400),
                new SyncXtqStatsPacket(18.5f, 40f, 6f, 30f, 100f, 2.5f, 100, true, false, true));
    }

    private static Method method(Class<?> type, String name) {
        return Arrays.stream(type.getMethods())
                .filter(m -> m.getName().equals(name) && Modifier.isStatic(m.getModifiers()))
                .findFirst().orElseThrow(() -> new AssertionError(type.getSimpleName() + " has no static " + name));
    }

    private static byte[] encode(Object packet) throws Exception {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        method(packet.getClass(), "encode").invoke(null, packet, buf);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    @TestFactory
    Stream<DynamicTest> decodeRestoresEveryField() {
        return samples().stream().map(p -> DynamicTest.dynamicTest(p.getClass().getSimpleName(), () -> {
            FriendlyByteBuf in = new FriendlyByteBuf(Unpooled.wrappedBuffer(encode(p)));
            Object decoded = method(p.getClass(), "decode").invoke(null, in);
            assertEquals(0, in.readableBytes(), "decode left bytes unread");
            // field by field, so unordered sets compare by contents rather than wire order
            for (java.lang.reflect.Field f : p.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                if (f.getType().isArray()) {
                    assertTrue(java.util.Objects.deepEquals(f.get(p), f.get(decoded)), f.getName());
                } else {
                    assertEquals(f.get(p), f.get(decoded), f.getName());
                }
            }
        }));
    }

    @Test
    void everyRegisteredPacketTypeHasASample() throws Exception {
        // NetworkHandler registers the packets; keep this list in step with it
        String source = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of(System.getProperty("jttw.projectDir", "."), "src/main/java/com/jttw/engine/network/NetworkHandler.java")));
        List<String> registered = java.util.regex.Pattern.compile("messageBuilder\\((\\w+)\\.class")
                .matcher(source).results().map(m -> m.group(1)).toList();
        Set<String> sampled = new java.util.HashSet<>(samples().stream().map(o -> o.getClass().getSimpleName()).toList());
        sampled.add("SyncTransformationPacket"); // GameTest
        for (String name : registered) assertTrue(sampled.contains(name), name + " has no round-trip sample");
    }

    @Test
    void unknownEnumOrdinalsDecodeToASafeValue() throws Exception {
        // ActivateAbilityPacket clamps a bad action ordinal instead of throwing on the server
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(1);
        buf.writeByte(99);
        assertNotNull(ActivateAbilityPacket.decode(buf));
        // RequestTransformPacket falls back to ENTITY
        FriendlyByteBuf req = new FriendlyByteBuf(Unpooled.buffer());
        req.writeVarInt(42);
        req.writeResourceLocation(new ResourceLocation("minecraft", "cow"));
        FriendlyByteBuf again = new FriendlyByteBuf(Unpooled.buffer());
        RequestTransformPacket.encode(RequestTransformPacket.decode(req), again);
        assertEquals(RequestTransformPacket.Kind.ENTITY.ordinal(), again.readVarInt());
    }
}
