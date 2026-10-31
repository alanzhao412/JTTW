package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.wukong.CloneWork;
import com.jttw.engine.character.wukong.WuCloneManager;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/**
 * Shadow clones acting as players: gathering with the tools their owner carries, keeping a pack,
 * emptying it into a chest or sending him their best finds, fighting with a ring of fire when
 * hurt, working where they are set or following him, and opening their pack to him.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class CloneAiGameTests {

    /** Wukong with a clone out, holding nothing; the clone is returned. */
    private static WuCloneEntity clone(GameTestHelper h, ServerPlayer p) {
        grant(p, "wu_clone_1");
        WukongCharacter.INSTANCE.use(p, "wu_clone", AbilityAction.USE);
        List<WuCloneEntity> mine = near(p, WuCloneEntity.class, 8).stream()
                .filter(c -> c.getOwnerUUID().map(p.getUUID()::equals).orElse(false)).toList();
        check(h, mine.size() == 1, "one clone out");
        com.jttw.engine.gametest.GT.onEnd(h, () -> WuCloneManager.dismissAll(p));
        return mine.get(0);
    }

    private static ServerPlayer wukong(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        qi(p, 100f, 100f);
        return p;
    }

    private static boolean packHas(WuCloneEntity clone, net.minecraft.world.item.Item item) {
        return clone.pack().countItem(item) > 0;
    }

    @GameTest(template = PLATFORM)
    public static void hisClonesAreFoundToCopyHim(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        check(h, WuCloneManager.clonesOf(p).contains(c), "his clone, found");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "clone_staff", timeoutTicks = 120)
    public static void aClonesStaffGrowsAndFallsAsHisDoes(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        var at = h.absolutePos(new BlockPos(1, 2, 1));
        var his = new com.jttw.engine.entity.PlacedStaffEntity(h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getUUID());
        h.getLevel().addFreshEntity(his);
        var echo = com.jttw.engine.entity.PlacedStaffEntity.echoOf(his, at.getX() + 3.5, at.getY(), at.getZ() + 3.5);
        h.getLevel().addFreshEntity(echo);
        com.jttw.engine.gametest.GT.onEnd(h, () -> { his.discard(); echo.discard(); });
        check(h, echo.isEcho() && !his.isEcho(), "his, and its echo");
        echo.adjustScale(5);
        eq(h, 1f, echo.getStaffScale(), "the echo heeds no wheel of its own");
        his.adjustScale(3);
        float grown = his.getStaffScale();
        p.setYRot(90f);
        h.runAfterDelay(2, () -> {
            eq(h, grown, echo.getStaffScale(), "it grew as his grew");
            check(h, his.toppleBy(p), "he strikes his down");
        });
        h.runAfterDelay(4, () -> {
            check(h, echo.isFalling(), "the echo falls too");
            eq(h, 90f, echo.getToppleYaw(), "the same way");
            his.discard(); // back to his hand
        });
        h.runAfterDelay(6, () -> {
            check(h, !echo.isAlive(), "and the echo is gone with it");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void aCloneHasItsOwnersHealth(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        com.jttw.engine.progression.level.PlayerLevel.setLevel(p, 30);
        com.jttw.engine.progression.level.LevelManager.reapplyStats(p);
        WuCloneEntity c = clone(h, p);
        near(h, p.getMaxHealth(), c.getMaxHealth(), "its owner's max health");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aCloneMinesAnOreItCanSeeWithAGoodEnoughPickaxe(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        BlockPos ore = new BlockPos(4, 2, 4);
        h.setBlock(ore, Blocks.IRON_ORE);
        WuCloneEntity c = clone(h, p);
        h.succeedWhen(() -> {
            check(h, h.getBlockState(ore).isAir(), "the ore is mined");
            check(h, packHas(c, Items.RAW_IRON), "its raw iron is in the clone's pack");
            check(h, p.getInventory().countItem(Items.IRON_PICKAXE) == 1, "and his pickaxe is still his");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void withTooPoorAPickaxeItLeavesTheOre(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.WOODEN_PICKAXE));
        BlockPos ore = new BlockPos(4, 2, 4);
        h.setBlock(ore, Blocks.IRON_ORE);
        clone(h, p);
        h.runAfterDelay(100, () -> {
            check(h, h.getBlockState(ore).is(Blocks.IRON_ORE), "iron needs a stone pickaxe or better");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aCloneFellsALogWithAnAxe(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.STONE_AXE));
        BlockPos log = new BlockPos(4, 2, 4);
        h.setBlock(log, Blocks.OAK_LOG);
        h.setBlock(log.above(), Blocks.OAK_LEAVES); // a tree, with its own leaves
        WuCloneEntity c = clone(h, p);
        h.succeedWhen(() -> {
            check(h, h.getBlockState(log).isAir(), "the log is felled");
            check(h, packHas(c, Items.OAK_LOG), "into the pack");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void withoutAnAxeItLeavesTheTrees(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        BlockPos log = new BlockPos(4, 2, 4);
        h.setBlock(log, Blocks.OAK_LOG);
        clone(h, p);
        h.runAfterDelay(100, () -> {
            check(h, h.getBlockState(log).is(Blocks.OAK_LOG), "no axe, no felling");
            h.succeed();
        });
    }

    private static void fill(WuCloneEntity c, int cobble, int diamonds) {
        for (int i = 0; i < cobble; i++) c.pack().addItem(new ItemStack(Items.COBBLESTONE, 64));
        for (int i = 0; i < diamonds; i++) c.pack().addItem(new ItemStack(Items.DIAMOND, 64));
    }

    // alone: a hostile mob in a neighbouring test would rightly come first, and it would never stash
    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "clone_stash")
    public static void aNearlyFullPackGoesIntoTheNearestChest(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        BlockPos chest = new BlockPos(4, 2, 4);
        h.setBlock(chest, Blocks.CHEST);
        WuCloneEntity c = clone(h, p);
        fill(c, 25, 0);
        check(h, CloneWork.nearlyFull(c.pack()), "nearly full");
        h.succeedWhen(() -> {
            var box = (ChestBlockEntity) h.getBlockEntity(chest);
            eq(h, 25 * 64, box.countItem(Items.COBBLESTONE), "the chest took all of it");
            eq(h, 0, c.pack().countItem(Items.COBBLESTONE), "and the pack is empty");
        });
    }

    @GameTest(template = PLATFORM)
    public static void withNoChestAboutItsBestFindsGoToItsOwner(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        fill(c, 20, 4);
        CloneWork.sendValuables(c, p);
        eq(h, 4 * 64, p.getInventory().countItem(Items.DIAMOND), "every diamond, first");
        check(h, CloneWork.value(new ItemStack(Items.DIAMOND)) > CloneWork.value(new ItemStack(Items.COBBLESTONE)), "diamonds over stone");
        check(h, !CloneWork.nearlyFull(c.pack()) && c.pack().countItem(Items.COBBLESTONE) > 0, "until it has room; the stone stays");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void badlyHurtItLightsARingOfFireThatSparesItsOwnSide(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_ring_1", "wu_clone_2");
        WuCloneEntity c = clone(h, p);
        WukongCharacter.INSTANCE.use(p, "wu_clone", AbilityAction.USE); // a second clone, by it
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        zombie.setNoAi(true);
        c.getPersistentData().putFloat(WuCloneManager.CLONE_QI_TAG, 50f);
        c.setHealth(c.getMaxHealth() * 0.2f);
        h.succeedWhen(() -> {
            check(h, c.ringBurning(), "its ring is lit");
            check(h, zombie.isOnFire(), "what is near it burns");
            for (WuCloneEntity other : near(p, WuCloneEntity.class, 8)) {
                check(h, other == c || !other.isOnFire(), "not his other clone");
            }
            check(h, !p.isOnFire(), "nor him");
        });
    }

    @GameTest(template = PLATFORM)
    public static void sneakUsingItSetsItToWorkWhereItStands(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        check(h, c.getPriority() == WuCloneEntity.Priority.FOLLOW, "it follows him at first");
        check(h, WuCloneManager.followsOwner(c), "and so keeps with him");
        p.setShiftKeyDown(true);
        c.interact(p, InteractionHand.MAIN_HAND);
        check(h, c.getPriority() == WuCloneEntity.Priority.WORK, "set to work");
        eq(h, c.blockPosition(), c.workSite(), "where it stands");
        check(h, !WuCloneManager.followsOwner(c), "it stays at its work");
        c.tickCount += CloneWork.IDLE_BEFORE_FOLLOW + 1; // a long while with nothing to do
        check(h, WuCloneManager.followsOwner(c), "with nothing left to do, it comes back to him");
        c.interact(p, InteractionHand.MAIN_HAND);
        check(h, c.getPriority() == WuCloneEntity.Priority.GUARD, "next: guard");
        eq(h, c.blockPosition(), c.workSite(), "its post, where it stands");
        check(h, !WuCloneManager.followsOwner(c), "it keeps its post");
        c.interact(p, InteractionHand.MAIN_HAND);
        check(h, c.getPriority() == WuCloneEntity.Priority.FOLLOW, "and again: follow");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void usingItOpensItsPackToItsOwnerAlone(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        c.pack().addItem(new ItemStack(Items.DIAMOND, 3));
        p.setShiftKeyDown(false);
        c.interact(p, InteractionHand.MAIN_HAND);
        check(h, p.containerMenu instanceof ChestMenu menu && menu.getContainer() == c.pack(), "its pack, open to him");
        ServerPlayer stranger = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        c.interact(stranger, InteractionHand.MAIN_HAND);
        check(h, !(stranger.containerMenu instanceof ChestMenu), "not to anyone else");
        p.closeContainer();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aFallenOrDismissedCloneSpillsItsPack(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        c.pack().addItem(new ItemStack(Items.EMERALD, 5));
        WuCloneManager.dismissAll(p);
        var dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, c.getBoundingBox().inflate(3),
                e -> e.getItem().is(Items.EMERALD));
        eq(h, 5, dropped.stream().mapToInt(e -> e.getItem().getCount()).sum(), "its emeralds where it stood");
        dropped.forEach(net.minecraft.world.entity.Entity::discard);
        h.succeed();
    }

    // ── Reach, planning, safety, keeping up, fighting ────────────────────

    /** Puts the clone at relative (x, 2, z), standing. */
    private static void put(GameTestHelper h, WuCloneEntity c, double x, double z) {
        var at = h.absoluteVec(new net.minecraft.world.phys.Vec3(x, 2, z));
        c.teleportTo(at.x, at.y, at.z);
        c.getNavigation().stop();
    }

    /** Roots the clone where it stands: it cannot take a step. */
    private static void root(WuCloneEntity c) {
        c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
    }

    /** All his clones out, after pressing the key {@code n} times (granting the tiers). */
    private static List<WuCloneEntity> clones(GameTestHelper h, ServerPlayer p, int n) {
        grant(p, "wu_clone_1", "wu_clone_2", "wu_clone_3");
        for (int i = 0; i < n; i++) WukongCharacter.INSTANCE.use(p, "wu_clone", AbilityAction.USE);
        com.jttw.engine.gametest.GT.onEnd(h, () -> WuCloneManager.dismissAll(p));
        List<WuCloneEntity> all = WuCloneManager.clonesOf(p);
        eq(h, n, all.size(), "clones out");
        return all;
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void aCloneMinesABlockFourAwayWithoutStepping(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        BlockPos ore = new BlockPos(4, 2, 2);
        h.setBlock(ore, Blocks.IRON_ORE);
        WuCloneEntity c = clone(h, p);
        put(h, c, 0.5, 2.5);
        root(c);
        double startX = c.getX();
        h.succeedWhen(() -> {
            check(h, h.getBlockState(ore).isAir(), "the ore, four blocks off, is mined");
            check(h, packHas(c, Items.RAW_IRON), "into its pack");
            check(h, Math.abs(c.getX() - startX) < 0.5, "from where it stood: a player's reach");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aCloneWorksAnOreFromASpotInReachNotFromOnTopOfIt(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        BlockPos ore = new BlockPos(4, 1, 4); // in the floor
        h.setBlock(ore, Blocks.IRON_ORE);
        WuCloneEntity c = clone(h, p);
        put(h, c, 0.5, 0.5);
        BlockPos onIt = h.absolutePos(ore.above());
        boolean[] stoodOnIt = {false};
        h.onEachTick(() -> {
            if (c.blockPosition().equals(onIt)) stoodOnIt[0] = true;
        });
        h.succeedWhen(() -> {
            check(h, h.getBlockState(ore).isAir(), "the ore is mined");
            check(h, !stoodOnIt[0], "never from on top of it");
            check(h, c.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(ore))) > 1.2, "it stands off from the hole");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void twoClonesNeverWorkOneBlockAndShareTheWork(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        BlockPos a = new BlockPos(0, 2, 4), b = new BlockPos(4, 2, 4);
        h.setBlock(a, Blocks.IRON_ORE);
        h.setBlock(b, Blocks.IRON_ORE);
        List<WuCloneEntity> two = clones(h, p, 2);
        WuCloneEntity c1 = two.get(0), c2 = two.get(1);
        boolean[] clash = {false};
        h.onEachTick(() -> {
            if (c1.workingOn() != null && c1.workingOn().equals(c2.workingOn())) clash[0] = true;
        });
        h.succeedWhen(() -> {
            check(h, !clash[0], "never both at one block");
            check(h, h.getBlockState(a).isAir() && h.getBlockState(b).isAir(), "both ores mined");
            check(h, packHas(c1, Items.RAW_IRON) && packHas(c2, Items.RAW_IRON), "one each: they split the work");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void aCloneMinesTheWholeVeinEvenWhatWasHidden(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        for (int x = 3; x <= 4; x++) for (int y = 2; y <= 3; y++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        List<BlockPos> vein = List.of(new BlockPos(3, 2, 2), new BlockPos(4, 2, 2), new BlockPos(4, 3, 2), new BlockPos(4, 2, 1));
        for (BlockPos v : vein) h.setBlock(v, Blocks.IRON_ORE);
        WuCloneEntity c = clone(h, p);
        put(h, c, 0.5, 2.5);
        h.succeedWhen(() -> {
            for (BlockPos v : vein) check(h, h.getBlockState(v).isAir(), "every block of the vein mined: " + v);
            eq(h, 4, c.pack().countItem(Items.RAW_IRON), "four raw iron");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void aCloneLeavesAnOreThatHoldsBackLava(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        BlockPos held = new BlockPos(4, 2, 4), free = new BlockPos(4, 2, 0);
        h.setBlock(held, Blocks.IRON_ORE);
        h.setBlock(new BlockPos(3, 3, 4), Blocks.GLASS);
        h.setBlock(new BlockPos(4, 3, 3), Blocks.GLASS);
        h.setBlock(new BlockPos(4, 3, 4), Blocks.LAVA);
        h.setBlock(free, Blocks.IRON_ORE);
        WuCloneEntity c = clone(h, p);
        h.runAfterDelay(260, () -> {
            check(h, h.getBlockState(free).isAir(), "the free ore is mined");
            check(h, h.getBlockState(held).is(Blocks.IRON_ORE), "the one under the lava is left");
            check(h, h.getBlockState(new BlockPos(4, 3, 4)).is(Blocks.LAVA), "the lava stays where it was");
            check(h, !c.isOnFire() && !c.isInLava(), "and the clone is not burning");
            h.setBlock(new BlockPos(4, 3, 4), Blocks.AIR);
            h.succeed();
        });
    }

    // alone: a working clone looks far round its site, into its neighbours' platforms
    @GameTest(template = PLATFORM, timeoutTicks = 140, batch = "clone_floor")
    public static void aCloneNeverMinesTheFloorFromUnderItself(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        BlockPos floor = new BlockPos(0, 1, 0);
        h.setBlock(floor, Blocks.IRON_ORE);
        WuCloneEntity c = clone(h, p);
        put(h, c, 0.5, 0.5);
        root(c);
        c.setPriority(WuCloneEntity.Priority.WORK);
        double y = c.getY();
        h.runAfterDelay(100, () -> {
            check(h, h.getBlockState(floor).is(Blocks.IRON_ORE), "the ore under its feet is left");
            near(h, y, c.getY(), "it has not dropped");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void aStuckCloneGetsBackToHim(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        var at = h.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, 4.5));
        p.moveTo(at.x, at.y, at.z);
        WuCloneEntity c = clone(h, p);
        put(h, c, 0.5, 0.5);
        for (BlockPos g : List.of(new BlockPos(1, 2, 0), new BlockPos(1, 3, 0), new BlockPos(0, 2, 1), new BlockPos(0, 3, 1),
                new BlockPos(1, 2, 1), new BlockPos(1, 3, 1), new BlockPos(0, 4, 0))) {
            h.setBlock(g, Blocks.GLASS); // a cage of what someone built: it may not dig its way out
        }
        h.succeedWhen(() -> {
            check(h, c.distanceTo(p) < 4, "it is back by him");
            check(h, h.getBlockState(new BlockPos(1, 2, 0)).is(Blocks.GLASS), "without breaking his glass");
        });
    }

    // alone: it lands far off the platform
    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "clone_far")
    public static void aCloneLeftFarBehindAppearsByHimOnTheGroundBelowHim(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        List<BlockPos> pad = new java.util.ArrayList<>();
        for (int x = 0; x <= 4; x++) for (int z = 26; z <= 30; z++) pad.add(new BlockPos(x, 1, z));
        for (BlockPos b : pad) h.setBlock(b, Blocks.STONE);
        com.jttw.engine.gametest.GT.onEnd(h, () -> pad.forEach(b -> h.getLevel().setBlockAndUpdate(h.absolutePos(b), Blocks.AIR.defaultBlockState())));
        var up = h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 7, 28.5)); // flown off, five blocks over the pad
        p.moveTo(up.x, up.y, up.z);
        double padTop = h.absolutePos(new BlockPos(0, 2, 0)).getY();
        h.succeedWhen(() -> {
            check(h, Math.hypot(c.getX() - p.getX(), c.getZ() - p.getZ()) < 4.5, "it is by him");
            near(h, padTop, c.getY(), "on the ground below him");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void clonesSplitBetweenEnemiesAndLeaveHisPetAlone(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        var z1 = mob(h, EntityType.ZOMBIE, 0, 4);
        var z2 = mob(h, EntityType.ZOMBIE, 4, 4);
        var wolf = mob(h, EntityType.WOLF, 2, 4);
        wolf.tame(p);
        List<WuCloneEntity> two = clones(h, p, 2);
        boolean[] petHit = {false};
        h.onEachTick(() -> {
            for (WuCloneEntity c : two) if (c.getTarget() == wolf || c.getTarget() == p) petHit[0] = true;
        });
        h.succeedWhen(() -> {
            check(h, !petHit[0], "never his wolf, nor him");
            var t1 = two.get(0).getTarget();
            var t2 = two.get(1).getTarget();
            check(h, t1 != null && t2 != null, "both fighting");
            check(h, t1 != t2, "each its own zombie");
            check(h, (t1 == z1 || t1 == z2) && (t2 == z1 || t2 == z2), "the zombies");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void aCloneGoesForWhatAttacksHimFirst(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        put(h, c, 0.5, 0.5);
        var zombie = mob(h, EntityType.ZOMBIE, 0, 2); // nearer the clone
        var pig = mob(h, EntityType.PIG, 4, 4);       // but this one struck him
        p.setLastHurtByMob(pig);
        h.succeedWhen(() -> check(h, c.getTarget() == pig, "what struck him, not the nearer zombie (" + c.getTarget() + ")"));
    }

    @GameTest(template = PLATFORM)
    public static void pressingAtTheCapCallsThemBack(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        c.setPriority(WuCloneEntity.Priority.WORK);
        c.pack().addItem(new ItemStack(Items.COAL, 3));
        WukongCharacter.INSTANCE.use(p, "wu_clone", AbilityAction.USE); // at the cap: one clone
        eq(h, 1, WuCloneManager.clonesOf(p).size(), "no second clone");
        check(h, c.getPriority() == WuCloneEntity.Priority.FOLLOW, "it comes back to follow him");
        h.succeed();
    }

    /** Iron ore shut in bedrock on every side, his clone set to work beside it. */
    private static BlockPos oreInBedrock(GameTestHelper h) {
        BlockPos ore = new BlockPos(3, 2, 4);
        h.setBlock(ore, Blocks.IRON_ORE);
        for (var d : net.minecraft.core.Direction.values()) h.setBlock(ore.relative(d), Blocks.BEDROCK);
        return ore;
    }

    // alone: a working clone looks far round its site, into its neighbours' platforms
    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "clone_bedrock")
    public static void anOrdinaryClonesMiningRespectsBedrock(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.DIAMOND_PICKAXE));
        BlockPos ore = oreInBedrock(h);
        WuCloneEntity c = clone(h, p);
        c.setPriority(WuCloneEntity.Priority.WORK);
        h.runAfterDelay(160, () -> {
            check(h, h.getBlockState(ore).is(Blocks.IRON_ORE), "the ore stays shut in");
            for (var d : net.minecraft.core.Direction.values()) check(h, h.getBlockState(ore.relative(d)).is(Blocks.BEDROCK), "the bedrock holds");
            h.succeed();
        });
    }

    // alone: a working clone looks far round its site, into its neighbours' platforms
    @GameTest(template = PLATFORM, timeoutTicks = 400, batch = "clone_buddha")
    public static void theFightingBuddhasClonesBreakBedrock(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        com.jttw.engine.progression.level.PlayerLevel.setLevel(p, 100);
        check(h, com.jttw.engine.character.wukong.FightingBuddha.is(p), "the Fighting Buddha");
        p.getInventory().add(new ItemStack(Items.DIAMOND_PICKAXE));
        BlockPos ore = oreInBedrock(h);
        WuCloneEntity c = clone(h, p);
        c.setPriority(WuCloneEntity.Priority.WORK);
        h.succeedWhen(() -> {
            check(h, h.getBlockState(ore).isAir(), "the ore is dug out");
            check(h, packHas(c, Items.RAW_IRON), "and in its pack");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void logsThatAreNoTreeAreLeftStanding(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.STONE_AXE));
        BlockPos log = new BlockPos(4, 2, 4);
        h.setBlock(log, Blocks.OAK_LOG); // no leaves of its own: someone's wall
        clone(h, p);
        h.runAfterDelay(100, () -> {
            check(h, h.getBlockState(log).is(Blocks.OAK_LOG), "a log house is not a tree");
            h.succeed();
        });
    }

    /** Fills a box (relative, inclusive) with {@code block}, cleared again when the test ends. */
    private static void fill(GameTestHelper h, BlockPos from, BlockPos to, net.minecraft.world.level.block.Block block) {
        List<BlockPos> all = new java.util.ArrayList<>();
        BlockPos.betweenClosed(from, to).forEach(b -> all.add(b.immutable()));
        for (BlockPos b : all) h.setBlock(b, block);
        com.jttw.engine.gametest.GT.onEnd(h, () -> all.forEach(b ->
                h.getLevel().setBlock(h.absolutePos(b), Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS)));
    }

    // alone: it digs out of its platform
    @GameTest(template = PLATFORM, timeoutTicks = 900, batch = "clone_tunnel")
    public static void aWorkingCloneTunnelsToABuriedOre(GameTestHelper h) {
        com.jttw.engine.gametest.GT.openAir(h);
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        fill(h, new BlockPos(0, 1, 5), new BlockPos(4, 4, 14), Blocks.STONE);
        BlockPos ore = new BlockPos(2, 2, 12);
        h.setBlock(ore, Blocks.IRON_ORE);
        WuCloneEntity c = clone(h, p);
        put(h, c, 2.5, 3.5);
        c.setPriority(WuCloneEntity.Priority.WORK);
        h.succeedWhen(() -> {
            check(h, h.getBlockState(ore).isAir(), "the buried ore is dug out");
            check(h, packHas(c, Items.RAW_IRON), "into its pack");
            check(h, packHas(c, Items.COBBLESTONE), "with the stone of its tunnel");
            check(h, h.getBlockState(new BlockPos(2, 1, 8)).is(Blocks.STONE), "the tunnel keeps its floor");
        });
    }

    // alone: it climbs above the platform's roof
    @GameTest(template = PLATFORM, timeoutTicks = 400, batch = "clone_pillar")
    public static void aWorkingClonePillarsUpToWhatIsOverheadAndComesDownAgain(GameTestHelper h) {
        com.jttw.engine.gametest.GT.openAir(h);
        ServerPlayer p = wukong(h);
        var corner = h.absoluteVec(new net.minecraft.world.phys.Vec3(0.5, 2, 0.5));
        p.moveTo(corner.x, corner.y, corner.z);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        BlockPos ore = new BlockPos(2, 9, 2);
        h.setBlock(ore, Blocks.IRON_ORE);
        com.jttw.engine.gametest.GT.onEnd(h, () -> h.getLevel().setBlockAndUpdate(h.absolutePos(ore), Blocks.AIR.defaultBlockState()));
        WuCloneEntity c = clone(h, p);
        put(h, c, 2.5, 2.5);
        c.pack().addItem(new ItemStack(Items.COBBLESTONE, 16));
        c.setPriority(WuCloneEntity.Priority.WORK);
        double ground = c.getY();
        boolean[] climbed = {false};
        h.onEachTick(() -> {
            if (c.getY() > ground + 0.9 && c.onGround()) climbed[0] = true; // stood on a block it put down
        });
        h.succeedWhen(() -> {
            check(h, h.getBlockState(ore).isAir(), "the ore overhead is mined");
            check(h, climbed[0], "it climbed to it");
            near(h, ground, c.getY(), "and came back down");
            check(h, h.getBlockState(new BlockPos(2, 2, 2)).isAir(), "taking its pillar with it");
        });
    }
}
