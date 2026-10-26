package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.kunlun.Kunlun;
import com.jttw.engine.world.kunlun.KunlunPalace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * Mount Kunlun: the summit's blueprint (the way from the stair's head through the gate, over the
 * Jade Pool's causeway and up the palace's steps to the treasure chest, room to stand, every sign
 * on a wall); the treasure's loot (the Banana-Leaf Fan in it once, every time, and no other
 * signature weapon), and the chest as laid holding it; and the Earth God's Luopan, asked sneaking
 * in the world of the living, pointing there for anyone of any level, its needle too, held sneaking.
 * Its place, its peak and its stair: KunlunPlacementTest.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class KunlunGameTests {

    // ── The blueprint ───────────────────────────────────────────────────

    /** Whether one could stand on the blueprint's block at local (x, y, z): solid (the summit's ground where it leaves the ground be). */
    private static boolean solid(int x, int y, int z) {
        BlockState s = KunlunPalace.at(x, y, z);
        return s == null ? y <= 0 : s.blocksMotion();
    }

    /** Whether the blueprint's local (x, y, z) is open to walk through. */
    private static boolean open(int x, int y, int z) {
        BlockState s = KunlunPalace.at(x, y, z);
        if (s == null) return y > 0;
        return !s.blocksMotion() && s.getFluidState().isEmpty();
    }

    private static boolean standable(int x, int y, int z) {
        return solid(x, y - 1, z) && open(x, y, z) && open(x, y + 1, z);
    }

    @GameTest(template = PLATFORM)
    public static void theSummitLeadsFromTheStairsHeadToTheTreasureChest(GameTestHelper h) {
        int top = KunlunPalace.SUMMIT;
        // the stair arrives at the summit's south edge, where the way in begins
        int[] start = {0, 1, top};
        check(h, KunlunPalace.onApproach(0, top) && standable(start[0], start[1], start[2]), "standing at the stair's head");
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> todo = new ArrayDeque<>();
        todo.add(start);
        seen.add(BlockPos.asLong(start[0], start[1], start[2]));
        while (!todo.isEmpty()) {
            int[] c = todo.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int dy = 1; dy >= -2; dy--) {
                    int x = c[0] + d.getStepX(), y = c[1] + dy, z = c[2] + d.getStepZ();
                    if (Math.abs(x) > top + 1 || Math.abs(z) > top + 2 || y < 1 || y > KunlunPalace.TOP) continue;
                    if (dy == 1 && !open(c[0], c[1] + 2, c[2])) continue; // no room to step up
                    if (!standable(x, y, z)) continue;
                    if (seen.add(BlockPos.asLong(x, y, z))) todo.add(new int[]{x, y, z});
                    break;
                }
            }
        }
        check(h, seen.contains(BlockPos.asLong(0, 1, KunlunPalace.GATE_Z)), "through the gate");
        check(h, seen.contains(BlockPos.asLong(0, 1, 9)), "over the Jade Pool's causeway");
        int floor = KunlunPalace.PLINTH_H + 1;
        check(h, seen.contains(BlockPos.asLong(0, floor, KunlunPalace.PLINTH_S - 1)), "up the palace's steps");
        check(h, seen.contains(BlockPos.asLong(0, floor, KunlunPalace.HALL_S)), "in at its door");
        check(h, seen.contains(BlockPos.asLong(KunlunPalace.CHEST_X, floor, KunlunPalace.CHEST_Z + 1)), "before the treasure chest");
        check(h, seen.contains(BlockPos.asLong(-15, 1, 12)) && seen.contains(BlockPos.asLong(12, 1, -19)), "round the terrace");
        check(h, seen.size() > 900, "much standing room: " + seen.size());
        // the chest: on the dais, facing the door, at hand from the floor before it
        BlockState chest = KunlunPalace.at(KunlunPalace.CHEST_X, KunlunPalace.CHEST_Y, KunlunPalace.CHEST_Z);
        check(h, chest != null && chest.is(Blocks.CHEST) && chest.getValue(ChestBlock.FACING) == Direction.SOUTH, "the chest, facing the door: " + chest);
        check(h, solid(KunlunPalace.CHEST_X, KunlunPalace.CHEST_Y - 1, KunlunPalace.CHEST_Z), "on the dais");
        check(h, open(KunlunPalace.CHEST_X, KunlunPalace.CHEST_Y + 1, KunlunPalace.CHEST_Z), "its lid free to open");
        check(h, KunlunPalace.CHEST_Y - floor <= 1, "within reach of one standing before it");
        int chests = 0;
        for (int x = -top - 1; x <= top + 1; x++) for (int z = -top - 1; z <= top + 2; z++) for (int y = KunlunPalace.BOTTOM; y <= KunlunPalace.TOP; y++) {
            BlockState s = KunlunPalace.at(x, y, z);
            if (s != null && s.is(Blocks.CHEST)) chests++;
            if (s != null && !s.getFluidState().isEmpty()) {
                check(h, KunlunPalace.inPool(x, z), "water only in the pool: " + x + " " + y + " " + z);
            }
        }
        eq(h, 1, chests, "one chest");
        // every sign hangs on a wall
        for (KunlunPalace.Sign s : KunlunPalace.SIGNS) {
            int bx = s.x() - s.facing().getStepX(), bz = s.z() - s.facing().getStepZ();
            check(h, solid(bx, s.y(), bz) && KunlunPalace.at(bx, s.y(), bz) != null, "the sign " + s.lines()[1] + " on a wall");
            BlockState own = KunlunPalace.at(s.x(), s.y(), s.z());
            check(h, own == null || own.isAir(), "the sign " + s.lines()[1] + " in the open");
        }
        h.succeed();
    }

    // ── The treasure ────────────────────────────────────────────────────

    private static int count(List<ItemStack> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static final List<net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ? extends net.minecraft.world.item.Item>> WEAPONS =
            List.of(ModItems.RUYI_JINGU_BANG, ModItems.THREE_POINTED_SPEAR, ModItems.ERLANG_AXE, ModItems.FIRE_TIP_SPEAR, ModItems.QIANKUN_RING,
                    ModItems.YIN_YANG_SWORD, ModItems.WIND_FIRE_WHEELS, ModItems.HUNTIAN_CLOTH, ModItems.BELL_OF_TAIYI, ModItems.NINE_TOOTH_RAKE,
                    ModItems.YIN_YANG_VASE, ModItems.MIXED_IRON_CUDGEL);

    @GameTest(template = PLATFORM)
    public static void theTreasureHoldsTheBananaLeafFanOnceEveryTime(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(Kunlun.TREASURE);
        check(h, table != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "the table is there");
        for (int i = 0; i < 40; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
                    .create(LootContextParamSets.CHEST);
            List<ItemStack> items = table.getRandomItems(params);
            eq(h, 1, count(items, ModItems.BANANA_LEAF_FAN.get()), "the fan, once, roll " + i);
            check(h, items.size() >= 4 && items.size() <= 6, "and a few gifts with it: " + items);
            for (var w : WEAPONS) eq(h, 0, count(items, w.get()), "no " + w.getId().getPath());
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "kunlun_summit")
    public static void theChestOnTheSummitHoldsTheFan(GameTestHelper h) {
        ServerLevel overworld = h.getLevel().getServer().overworld();
        BlockPos ground = Kunlun.summit(overworld);
        int[] c = Kunlun.centre(overworld.getSeed());
        eq(h, c[0], ground.getX(), "at the region's centre");
        check(h, ground.getY() >= Math.min(236, overworld.getMaxBuildHeight() - 32) && ground.getY() + KunlunPalace.TOP < overworld.getMaxBuildHeight(),
                "high, under the sky: " + ground.getY());
        GateSites.hold(h, overworld, ground);
        KunlunPalace.layWhole(overworld, ground);
        BlockPos at = Kunlun.chest(overworld);
        onEnd(h, () -> {
            if (overworld.getBlockEntity(at) instanceof ChestBlockEntity box) box.clearContent();
            overworld.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        });
        check(h, overworld.getBlockState(at).is(Blocks.CHEST), "the chest laid on the summit at " + at.toShortString());
        if (!(overworld.getBlockEntity(at) instanceof ChestBlockEntity box)) {
            h.fail("no chest there");
            return;
        }
        ServerPlayer p = player(h);
        box.unpackLootTable(p);
        int fans = 0, filled = 0;
        for (int i = 0; i < box.getContainerSize(); i++) {
            ItemStack s = box.getItem(i);
            if (s.is(ModItems.BANANA_LEAF_FAN.get())) fans += s.getCount();
            if (!s.isEmpty()) filled++;
        }
        eq(h, 1, fans, "the Banana-Leaf Fan in it, once");
        check(h, filled >= 4, "with its gifts: " + filled);
        h.succeed();
    }

    // ── The luopan ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theLuopanAskedSneakingPointsAnyoneToKunlun(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        int[] k = Kunlun.centre(seed);
        for (int lv : new int[]{1, 30, 60, 100}) {
            var a = EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, lv, seed, Set.of(), true);
            eq(h, Zones.KUNLUN, a.zone(), "level " + lv + ", sneaking: Kunlun");
            eq(h, new BlockPos(k[0], 64, k[1]), a.target().pos(), "its needle to the summit");
            String words = a.words().getString();
            check(h, words.contains("Mount Kunlun") && words.contains("Banana-Leaf Fan") && words.contains("west") && words.contains("blocks"),
                    "told where it is: " + words);
            // not sneaking: as ever, the zone for the level, and a word on how to ask
            var plain = EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, lv, seed, Set.of(), false);
            check(h, plain.zone() != Zones.KUNLUN, "standing: the zone for the level");
            check(h, plain.words().getString().contains("Sneak as you ask, for the way to Mount Kunlun"), "and how to ask: " + plain.words().getString());
        }
        eq(h, Zones.WHITE_BONE, EarthGod.answer(Level.OVERWORLD, Vec3.ZERO, 25, seed, Set.of(), false).zone(), "the bands' guidance as before");
        var there = EarthGod.answer(Level.OVERWORLD, new Vec3(k[0] + 40.5, 200, k[1] + 0.5), 50, seed, Set.of(), true);
        check(h, there.words().getString().contains("on its slopes"), "on the mountain: " + there.words().getString());
        // in Diyu, sneaking, still the way back up; in the Nether, no one
        var diyu = EarthGod.answer(com.jttw.engine.dimension.JTTWDimensions.HELL, Vec3.ZERO, 60, seed, Set.of(), true);
        check(h, diyu.zone() == null && diyu.words().getString().contains("back to the living"), "in Diyu, the way out: " + diyu.words().getString());
        eq(h, null, EarthGod.answer(Level.NETHER, Vec3.ZERO, 60, seed, Set.of(), true).target(), "in the Nether, no Earth God");
        // the luopan itself: used sneaking, and held sneaking
        ServerPlayer p = player(h);
        ItemStack luopan = new ItemStack(ModItems.EARTH_GOD_LUOPAN.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, luopan);
        PlayerLevel.setLevel(p, 40);
        p.setShiftKeyDown(true);
        luopan.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        eq(h, new BlockPos(k[0], 64, k[1]), luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos(), "used sneaking: to Kunlun");
        p.setShiftKeyDown(false);
        p.tickCount = 0;
        luopan.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        check(h, !luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos().equals(new BlockPos(k[0], 64, k[1])), "held standing: the zone for the level");
        p.setShiftKeyDown(true);
        luopan.inventoryTick(h.getLevel(), p, p.getInventory().selected, true);
        eq(h, new BlockPos(k[0], 64, k[1]), luopan.get(DataComponents.LODESTONE_TRACKER).target().get().pos(), "held sneaking: to Kunlun");
        h.succeed();
    }
}
