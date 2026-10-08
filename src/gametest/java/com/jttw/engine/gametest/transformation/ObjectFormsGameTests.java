package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.jttw.engine.network.c2s.SetSignTextPacket;
import com.jttw.engine.network.c2s.UseFormPacket;
import com.jttw.engine.transformation.BlockForms;
import com.jttw.engine.transformation.BoatForms;
import com.jttw.engine.transformation.FormCatalog;
import com.jttw.engine.transformation.TransformationAbilityHandler;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/** Object and block forms: what may be chosen, and what a chest, sign, TNT, creeper and boat do. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ObjectFormsGameTests {

    private static ResourceLocation id(String path) {
        return new ResourceLocation("minecraft", path);
    }

    /** A Wukong with 72 Transformations III: may take objects. */
    private static ServerPlayer objectTaker(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        com.jttw.engine.reincarnation.AlignmentData.markReincarnated(p,
                com.jttw.engine.reincarnation.ReincarnationOutcome.CELESTIAL_PRIMATE);
        Tf.grantWukongTier(p, 3);
        return p;
    }

    private static void send(ServerPlayer p, Object packet) {
        var ctx = Tf.serverContext(p);
        if (packet instanceof RequestTransformPacket r) RequestTransformPacket.handle(r, () -> ctx);
        else if (packet instanceof SetSignTextPacket s) SetSignTextPacket.handle(s, () -> ctx);
        else if (packet instanceof UseFormPacket u) UseFormPacket.handle(u, () -> ctx);
    }

    // ── what may be chosen ─────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void nonFormsAreNotOfferedAndBlocksAre(GameTestHelper h) {
        List<ResourceLocation> objects = FormCatalog.objectForms(h.getLevel());
        for (String gone : List.of("lightning_bolt", "fishing_bobber", "marker", "interaction",
                "block_display", "item_display", "text_display", "tnt", "item", "evoker_fangs")) {
            check(h, !objects.contains(id(gone)), gone + " is not a form");
        }
        for (String mod : List.of("qi_wave", "thrown_spear", "howl_projectile", "placed_staff")) {
            check(h, !objects.contains(new ResourceLocation(JTTWEngine.MOD_ID, mod)), "the mod's " + mod + " is not a form");
        }
        check(h, objects.contains(id("boat")) && objects.contains(id("minecart")), "real objects still are");
        eq(h, List.of(id("chest"), id("oak_sign"), id("tnt")), FormCatalog.blockForms(), "block forms");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void requestsForNonFormsAreRefusedAndBlocksAreGranted(GameTestHelper h) {
        ServerPlayer p = objectTaker(h);
        send(p, new RequestTransformPacket(RequestTransformPacket.Kind.OBJECT_ENTITY, id("lightning_bolt")));
        check(h, !TransformationManager.isTransformed(p), "no lightning form");
        send(p, new RequestTransformPacket(RequestTransformPacket.Kind.OBJECT_ENTITY, id("tnt")));
        check(h, !TransformationManager.isTransformed(p), "no primed-TNT form");
        send(p, new RequestTransformPacket(RequestTransformPacket.Kind.OBJECT_ENTITY, new ResourceLocation(JTTWEngine.MOD_ID, "qi_wave")));
        check(h, !TransformationManager.isTransformed(p), "no Qi Wave form");
        send(p, RequestTransformPacket.block(id("stone")));
        check(h, !TransformationManager.isTransformed(p), "stone is not a block form");
        send(p, RequestTransformPacket.block(id("chest")));
        TransformationData data = TransformationManager.getTransformation(p);
        check(h, data != null && data.getBlock() == Blocks.CHEST, "became a chest: " + data);

        ServerPlayer novice = Tf.player(h);
        com.jttw.engine.reincarnation.AlignmentData.markReincarnated(novice,
                com.jttw.engine.reincarnation.ReincarnationOutcome.CELESTIAL_PRIMATE);
        Tf.grantWukongTier(novice, 2);
        send(novice, RequestTransformPacket.block(id("tnt")));
        check(h, !TransformationManager.isTransformed(novice), "block forms are objects: tier III only");
        h.succeed();
    }

    // ── the block body ─────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void blockFormsTakeTheBlocksSize(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transformIntoBlock(p, Blocks.CHEST);
        near(h, 0.875, p.getBbWidth(), "chest width");
        near(h, 0.875, p.getBbHeight(), "chest height");
        TransformationManager.transformIntoBlock(p, Blocks.OAK_SIGN);
        near(h, 0.5, p.getBbWidth(), "sign width");
        near(h, 1.0, p.getBbHeight(), "sign height");
        near(h, 0.85, p.getEyeHeight(), "camera at 85% of the sign");
        TransformationManager.transformIntoBlock(p, Blocks.TNT);
        near(h, 1.0, p.getBbWidth(), "TNT width");
        near(h, 1.0, p.getBbHeight(), "TNT height");
        eq(h, id("tnt"), TransformationManager.getTransformation(p).getFormId(), "form id is the block's");
        TransformationManager.demorph(p);
        near(h, 1.8, p.getBbHeight(), "back to a player");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void blockFormsSurviveSavingAndTheNetwork(GameTestHelper h) {
        TransformationData sign = TransformationData.ofBlock(Blocks.OAK_SIGN).withSignLines(List.of("one", "", "three"));
        TransformationData saved = TransformationData.deserialize(sign.serialize());
        check(h, saved != null && saved.getBlock() == Blocks.OAK_SIGN, "saved sign: " + saved);
        eq(h, List.of("one", "", "three"), saved.getSignLines(), "saved text");

        TransformationData lit = TransformationData.ofBlock(Blocks.TNT).withLitUntil(1234L);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        lit.toNetwork(buf);
        TransformationData sent = TransformationData.fromNetwork(buf);
        eq(h, 0, buf.readableBytes(), "read exactly what was written");
        check(h, sent != null && sent.getBlock() == Blocks.TNT && sent.getLitUntil() == 1234L, "lit TNT over the wire: " + sent);
        eq(h, 0L, TransformationData.deserialize(lit.serialize()).getLitUntil(), "a lit fuse is not saved");

        CompoundTag unknown = sign.serialize();
        unknown.putString("block", "nope:missing");
        eq(h, null, TransformationData.deserialize(unknown), "an unknown block loads as no form");
        h.succeed();
    }

    // ── chest ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aChestFormOpensItsOwnStorageWhichItKeeps(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        UseFormPacket.handleServer(p);
        check(h, !(p.containerMenu instanceof ChestMenu), "not a chest yet: nothing opens");
        TransformationManager.transformIntoBlock(p, Blocks.CHEST);
        UseFormPacket.handleServer(p);
        check(h, p.containerMenu instanceof ChestMenu, "opened: " + p.containerMenu);
        ChestMenu menu = (ChestMenu) p.containerMenu;
        eq(h, 3, menu.getRowCount(), "27 slots");
        menu.getSlot(4).set(new ItemStack(Items.DIAMOND, 5));
        p.closeContainer();
        eq(h, 5, BlockForms.loadChest(p).get(4).getCount(), "kept on the player");

        // through death...
        ServerPlayer after = Tf.player(h);
        BlockForms.onClone(new PlayerEvent.Clone(after, p, true));
        eq(h, Items.DIAMOND, BlockForms.loadChest(after).get(4).getItem(), "kept through death");
        // ...and changing form
        TransformationManager.transformIntoBlock(p, Blocks.TNT);
        TransformationManager.transformIntoBlock(p, Blocks.CHEST);
        UseFormPacket.handleServer(p);
        eq(h, Items.DIAMOND, ((ChestMenu) p.containerMenu).getSlot(4).getItem().getItem(), "same chest after changing form");
        p.closeContainer();

        ServerPlayer boat = Tf.playerAs(h, EntityType.CHEST_BOAT);
        UseFormPacket.handleServer(boat);
        check(h, boat.containerMenu instanceof ChestMenu, "a chest boat opens its chest too");
        boat.closeContainer();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void anItemInHandIsNotAnEmptyHandClick(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transformIntoBlock(p, Blocks.CHEST);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        UseFormPacket.handleServer(p);
        check(h, !(p.containerMenu instanceof ChestMenu), "holding a stick: the packet is refused");
        h.succeed();
    }

    // ── sign ───────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aSignFormShowsTheTextItsPlayerWrites(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transformIntoBlock(p, Blocks.OAK_SIGN);
        String longLine = "x".repeat(50);
        send(p, new SetSignTextPacket(List.of("Hello", longLine, "", "world")));
        TransformationData data = TransformationManager.getTransformation(p);
        eq(h, List.of("Hello", "x".repeat(TransformationData.SIGN_LINE_LENGTH), "", "world"), data.getSignLines(),
                "text, long line cut");
        check(h, TransformationData.deserialize(p.getPersistentData().getCompound("jttw_transformation"))
                .getSignLines().get(0).equals("Hello"), "saved on the player");

        ServerPlayer chest = Tf.player(h);
        TransformationManager.transformIntoBlock(chest, Blocks.CHEST);
        send(chest, new SetSignTextPacket(List.of("sneaky")));
        eq(h, List.of(), TransformationManager.getTransformation(chest).getSignLines(), "only a sign takes text");
        h.succeed();
    }

    // ── TNT ────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aTntFormLightsFromItsOwnClickOrFlintAndSteel(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        TransformationManager.transformIntoBlock(p, Blocks.TNT);
        long now = h.getLevel().getGameTime();
        UseFormPacket.handleServer(p);
        eq(h, now + BlockForms.TNT_FUSE_TICKS, TransformationManager.getTransformation(p).getLitUntil(), "lit: 4 s fuse");
        UseFormPacket.handleServer(p);
        eq(h, now + BlockForms.TNT_FUSE_TICKS, TransformationManager.getTransformation(p).getLitUntil(), "lighting again changes nothing");

        ServerPlayer other = Tf.player(h);
        TransformationManager.transformIntoBlock(other, Blocks.TNT);
        ServerPlayer lighter = Tf.player(h);
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        lighter.setItemInHand(InteractionHand.MAIN_HAND, flint);
        var event = new PlayerInteractEvent.EntityInteract(lighter, InteractionHand.MAIN_HAND, other);
        BlockForms.onInteract(event);
        check(h, TransformationManager.getTransformation(other).isLit(), "flint and steel lights it");
        check(h, event.isCanceled(), "the click is used up");
        eq(h, 1, flint.getDamageValue(), "the flint and steel wears");

        ServerPlayer chest = Tf.player(h);
        TransformationManager.transformIntoBlock(chest, Blocks.CHEST);
        BlockForms.onInteract(new PlayerInteractEvent.EntityInteract(lighter, InteractionHand.MAIN_HAND, chest));
        check(h, !TransformationManager.getTransformation(chest).isLit(), "a chest does not light");
        h.succeed();
    }

    /** At the end of its fuse the TNT explodes like TNT, the player unhurt and back in their own form. */
    @GameTest(template = PLATFORM, batch = "tnt_form_blast")
    public static void aLitTntFormExplodesLikeTntAndReturnsThePlayer(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 4), Blocks.DIRT);
        var pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(0, 2, 4));
        ServerPlayer p = Tf.player(h);
        float health = p.getHealth();
        TransformationManager.transformIntoBlock(p, Blocks.TNT);
        BlockForms.light(p);
        BlockForms.tickFuse(p);
        check(h, TransformationManager.isTransformed(p), "still burning");
        TransformationManager.update(p, TransformationManager.getTransformation(p).withLitUntil(h.getLevel().getGameTime()));
        BlockForms.tickFuse(p);
        h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 4));
        check(h, pig.getHealth() < pig.getMaxHealth() || !pig.isAlive(), "the blast hurts what is near");
        near(h, health, p.getHealth(), "the player is unhurt");
        check(h, !TransformationManager.isTransformed(p), "and back in their own form");
        h.succeed();
    }

    // ── creeper ────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "creeper_form_blast")
    public static void aCreeperFormsBlastBreaksBlocksAndItStaysACreeper(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 4), Blocks.DIRT);
        ServerPlayer p = Tf.playerAs(h, EntityType.CREEPER);
        p.setHealth(2f); // a blast that could hurt it would kill it
        Tf.rightClick(p);
        for (int i = 0; i < 30; i++) TransformationAbilityHandler.tickCreeperCharge(p);
        h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 4));
        check(h, p.isAlive(), "survives its own blast");
        near(h, 2.0, p.getHealth(), "unhurt");
        eq(h, EntityType.CREEPER, TransformationManager.getTransformation(p).getEntityType(), "still a creeper");
        h.succeed();
    }

    // ── boat ───────────────────────────────────────────────────────

    private static boolean board(ServerPlayer rider, ServerPlayer boat) {
        var event = new PlayerInteractEvent.EntityInteract(rider, InteractionHand.MAIN_HAND, boat);
        BlockForms.onInteract(event);
        return rider.getVehicle() == boat;
    }

    @GameTest(template = PLATFORM)
    public static void aBoatFormCarriesTwoRidersAndAChestBoatOne(GameTestHelper h) {
        ServerPlayer boat = Tf.playerAs(h, EntityType.BOAT);
        eq(h, 2, BoatForms.seats(boat), "two seats");
        ServerPlayer a = Tf.player(h), b = Tf.player(h), c = Tf.player(h);
        check(h, board(a, boat), "first rider in");
        check(h, board(b, boat), "second rider in");
        check(h, !board(c, boat), "no third seat");
        eq(h, 2, boat.getPassengers().size(), "riders");

        // seated as in a boat: either side of the middle, at deck height
        boat.positionRider(a);
        boat.positionRider(b);
        check(h, a.position().distanceTo(b.position()) > 0.7, "two seats, not one: " + a.position() + " " + b.position());
        near(h, boat.getY() - 0.1 + a.getMyRidingOffset(), a.getY(), "deck height");
        check(h, a.position().subtract(boat.position()).horizontalDistance() < 1.0, "in the boat");

        TransformationManager.demorph(boat);
        check(h, !boat.isVehicle() && a.getVehicle() == null, "riders set down when it stops being a boat");

        ServerPlayer chestBoat = Tf.playerAs(h, EntityType.CHEST_BOAT);
        eq(h, 1, BoatForms.seats(chestBoat), "a chest boat has one seat");
        check(h, board(c, chestBoat), "one rider");
        check(h, !board(a, chestBoat), "the chest takes the other seat");
        TransformationManager.transform(chestBoat, EntityType.BOAT);
        check(h, c.getVehicle() == chestBoat, "still a boat: the rider stays");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nobodyRidesAPlayerWhoIsNotABoat(GameTestHelper h) {
        ServerPlayer cow = Tf.playerAs(h, EntityType.COW);
        ServerPlayer rider = Tf.player(h);
        check(h, !board(rider, cow), "a cow is no boat");
        ServerPlayer human = Tf.player(h);
        check(h, !board(rider, human), "nor is a person");
        ServerPlayer boat = Tf.playerAs(h, EntityType.BOAT);
        rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        check(h, !board(rider, boat), "boarding takes an empty hand");
        h.succeed();
    }
}
