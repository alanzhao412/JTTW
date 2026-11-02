package com.jttw.engine.village.raid;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.RaidChiefEntity;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.grind.GrindXp;
import com.jttw.engine.grind.RingLevels;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModGrinds;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.TangVillagePiece;
import com.jttw.engine.world.ZonePosts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * A raided village (劫掠之村): a Tang village a band of yaoguai has sacked and holds. Laid as a Tang
 * village is ({@link TangVillagePiece#lay}, without its folk), then sacked ({@link #sack}): its doors
 * smashed, its lanterns and awnings torn down, its timbers burnt black and its roofs fallen in where the
 * fire took, the ground scorched, embers still smouldering, cobwebs in the empty rooms, its crops
 * trampled and its chests emptied; then marked ({@link #marks}): the raiders' black banners with their
 * red skull, totems of skulls on poles over heaps of bone, the chief's hoard before the shrine, and at
 * each edge a sign telling what holds it and at what levels. Its band is its place's
 * ({@link RingLevels#bandAt}: the farther from spawn, the higher), fixed.
 *
 * <p>Its raiders ({@link #muster}) hold {@value #POSTS_N} posts round the village, {@value #PER_POST}
 * to each, the open world's yaoguai of its land: regulars of the band, a captain at the gate and one
 * on the square. Each is kept at its post ({@link ZonePosts}): fallen, it comes back 20 minutes after
 * its post's last falls, while no one is near. Their chief, the {@link RaidChiefEntity}, holds the
 * shrine's forecourt; his fall frees the village for good ({@link RaidedVillages}).
 */
public final class RaidedVillage {

    private RaidedVillage() {}

    /** The raiders' posts (plan x, z): the gate, the square, the cross street east and west, the north-east fields. */
    public static final int[][] POSTS = {{0, 28}, {-4, 4}, {24, 0}, {-26, 0}, {36, -20}};
    public static final int POSTS_N = 5, PER_POST = 3;
    /** Which posts have a captain at their head: the gate's and the square's. */
    public static final int CAPTAINS = 2;
    /** Where the chief stands (plan x, z): before the shrine of the Earth God. */
    public static final int[] CHIEF = {0, -21};
    /** How far a raider wanders from its post; the chief from his. */
    public static final int POST_REACH = 10, CHIEF_REACH = 24;
    /** Where the chief's hoard is (plan x, z). */
    public static final int[] HOARD = {3, -22};

    public static final ResourceKey<LootTable> HOARD_LOW = hoard("low"), HOARD_MID = hoard("mid"), HOARD_HIGH = hoard("high");

    private static ResourceKey<LootTable> hoard(String tier) {
        return ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/raided_village_hoard_" + tier));
    }

    /** The hoard a chief of this band keeps: low under 34, middling to 66, rich past. */
    public static ResourceKey<LootTable> hoardOf(RingLevels.Band band) {
        return band.hi() < 34 ? HOARD_LOW : band.hi() < 67 ? HOARD_MID : HOARD_HIGH;
    }

    /** The band of a raided village centred at {@code centre} in a world of {@code seed}. */
    public static RingLevels.Band band(long seed, BlockPos centre) {
        return RingLevels.bandAt(seed, centre.getX(), centre.getZ());
    }

    /** The kinds of its raiders, by its land: the plains' goblins and wolfmen, the mountains' wolfmen and ogres, the oasis's kobolds. */
    public static List<EntityType<? extends WildYaoguai>> kinds(TangVillage.Variant v) {
        return switch (v) {
            case PLAINS -> List.of(ModEntities.GOBLIN.get(), ModEntities.WOLFMAN.get(), ModEntities.GOBLIN.get(), ModEntities.KOBOLD.get());
            case MOUNTAIN -> List.of(ModEntities.WOLFMAN.get(), ModEntities.GOBLIN.get(), ModEntities.WOLFMAN.get(), ModEntities.OGRE.get());
            case OASIS -> List.of(ModEntities.KOBOLD.get(), ModEntities.GOBLIN.get(), ModEntities.KOBOLD.get(), ModEntities.OGRE.get());
        };
    }

    /** Lays the village's share within {@code area}: built, sacked, marked, and (if {@code raiders}) held. */
    public static void lay(WorldGenLevel level, TangVillage.Plan plan, BlockPos centre, BoundingBox area, long layout, RingLevels.Band band, boolean raiders) {
        TangVillagePiece.lay(level, plan, centre, area, false);
        sack(level, plan, centre, area, layout);
        marks(level, plan, centre, area, band);
        if (raiders) muster(level, plan, centre, area, layout, band);
    }

    /** The village's whole box (every column, its ground to its sky) round {@code centre}. */
    public static BoundingBox box(BlockPos centre) {
        return new BoundingBox(centre.getX() + TangVillage.WEST, centre.getY() + TangVillage.BOTTOM, centre.getZ() + TangVillage.NORTH,
                centre.getX() + TangVillage.EAST, centre.getY() + TangVillage.CLEAR, centre.getZ() + TangVillage.SOUTH);
    }

    // ── The sack ──────────────────────────────────────────────────────

    /** A hash of a place, 0-999, the same whichever chunk asks. */
    static int roll(long layout, int x, int y, int z) {
        return (int) Math.floorMod(Mth.getSeed(x, y, z) ^ (layout * 0x9E3779B97F4A7C15L), 1000L);
    }

    /** Whether the fire took at (x, z): in cells of six blocks, a little under half of them. */
    static boolean burnt(long layout, int x, int z) {
        return roll(layout, Math.floorDiv(x, 6), 7, Math.floorDiv(z, 6)) < 450;
    }

    /** Sacks the village's share within {@code area}. */
    public static void sack(WorldGenLevel level, TangVillage.Plan plan, BlockPos centre, BoundingBox area, long layout) {
        int cx = centre.getX(), floor = centre.getY(), cz = centre.getZ();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = Math.max(area.minX(), cx + TangVillage.WEST), x1 = Math.min(area.maxX(), cx + TangVillage.EAST);
        int z0 = Math.max(area.minZ(), cz + TangVillage.NORTH), z1 = Math.min(area.maxZ(), cz + TangVillage.SOUTH);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean fire = burnt(layout, x, z);
                for (int ly = 0; ly <= TangVillage.TOP; ly++) {
                    int y = floor + ly;
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || s.getBlock() instanceof BedBlock) continue;
                    int r = roll(layout, x, y, z);
                    BlockState to = sacked(s, ly, fire, r);
                    if (to != s) level.setBlock(p, to, Block.UPDATE_CLIENTS);
                }
                // cobwebs under what roofs are left, embers on the scorched ground
                for (int ly = 2; ly <= TangVillage.TOP - 1; ly++) {
                    p.set(x, floor + ly, z);
                    if (!level.getBlockState(p).isAir()) continue;
                    if (level.getBlockState(p.above()).blocksMotion() && roll(layout, x, floor + ly, z) < 45) {
                        level.setBlock(p, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                p.set(x, floor + 1, z);
                if (fire && level.getBlockState(p).isAir() && level.getBlockState(p.below()).blocksMotion() && roll(layout, x, floor + 1, z) < 5) {
                    level.setBlock(p, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), Block.UPDATE_CLIENTS);
                }
            }
        }
        // the chests the raiders emptied
        for (TangVillage.Chest chest : plan.chests) {
            BlockPos at = new BlockPos(cx + plan.planX(chest.x()), floor + chest.y(), cz + chest.z());
            if (area.isInside(at) && level.getBlockEntity(at) instanceof RandomizableContainerBlockEntity c) c.setLootTable(null, 0L);
        }
    }

    /** What the raid left of {@code s}, {@code ly} over the ground, where the fire took or not, by a roll of 0-999. */
    static BlockState sacked(BlockState s, int ly, boolean fire, int r) {
        Block b = s.getBlock();
        if (b instanceof DoorBlock) return Blocks.AIR.defaultBlockState();                     // every door smashed in
        if (b instanceof TrapDoorBlock) return r < 500 ? Blocks.AIR.defaultBlockState() : s;
        if (b instanceof LanternBlock) return r < 650 ? Blocks.AIR.defaultBlockState() : s;
        if (b instanceof FlowerPotBlock) return r < 500 ? Blocks.AIR.defaultBlockState() : s;
        if (b instanceof BannerBlock || s.is(BlockTags.BANNERS)) return Blocks.AIR.defaultBlockState();
        if (s.is(BlockTags.WOOL) || s.is(BlockTags.WOOL_CARPETS)) return r < 600 ? Blocks.AIR.defaultBlockState() : s;
        if (b instanceof CropBlock || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.PUMPKIN)) {   // the crops trampled
            return r < 650 ? Blocks.AIR.defaultBlockState() : s;
        }
        if (b instanceof FarmBlock) return r < 700 ? Blocks.COARSE_DIRT.defaultBlockState() : s;
        if (s.is(Blocks.GLASS_PANE) || s.is(Blocks.GLASS)) return Blocks.AIR.defaultBlockState();
        if (ly == 0) {                                                                    // the ground: scorched where it burnt
            if (!fire || !s.blocksMotion() || !s.getFluidState().isEmpty()) return s;
            if (r < 160) return Blocks.COARSE_DIRT.defaultBlockState();
            if (r < 230) return Blocks.SMOOTH_BASALT.defaultBlockState();
            if (r < 260) return Blocks.GRAVEL.defaultBlockState();
            return s;
        }
        if (!fire) {
            if (s.is(BlockTags.LEAVES)) return r < 150 ? Blocks.AIR.defaultBlockState() : s;
            return s;
        }
        // where the fire took: timbers burnt black or gone, roofs fallen in, walls blackened and broken
        if (s.is(BlockTags.LEAVES)) return r < 550 ? Blocks.AIR.defaultBlockState() : s;
        if (s.is(BlockTags.LOGS)) {
            if (r < 450) return Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                    s.hasProperty(RotatedPillarBlock.AXIS) ? s.getValue(RotatedPillarBlock.AXIS) : Direction.Axis.Y);
            return ly >= 3 && r < 600 ? Blocks.AIR.defaultBlockState() : s;
        }
        if (s.is(BlockTags.PLANKS)) {
            if (r < 300) return Blocks.BLACKSTONE.defaultBlockState();
            return ly >= 2 && r < 520 ? Blocks.AIR.defaultBlockState() : s;
        }
        if (b instanceof StairBlock || b instanceof SlabBlock) {
            if (ly >= 3 && r < 380) return Blocks.AIR.defaultBlockState();
            if (s.is(BlockTags.WOODEN_STAIRS) || s.is(BlockTags.WOODEN_SLABS)) return r < 600 ? Blocks.AIR.defaultBlockState() : s;
            return s;
        }
        if (s.is(BlockTags.WOODEN_FENCES)) return r < 450 ? Blocks.AIR.defaultBlockState() : s;
        if (s.is(Blocks.CALCITE) || s.is(Blocks.SMOOTH_SANDSTONE) || s.is(Blocks.DEEPSLATE_TILES) || s.is(Blocks.MUD_BRICKS)) {
            if (ly >= 2 && r < 120) return Blocks.AIR.defaultBlockState();                // holes broken through
            if (r < 330) return Blocks.SMOOTH_BASALT.defaultBlockState();                // soot
            if (r < 400) return Blocks.COBBLED_DEEPSLATE.defaultBlockState();            // patched rubble
            return s;
        }
        if (s.is(Blocks.STONE_BRICKS) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.CUT_SANDSTONE)) {
            if (r < 200) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            if (r < 280) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            return s;
        }
        return s;
    }

    // ── The marks ─────────────────────────────────────────────────────

    /** The signs at its edges (plan x, z, the way they face, unmirrored). */
    static final Object[][] SIGNS = {{3, 35, 0}, {-3, 35, 0}, {6, -34, 8}, {TangVillage.EAST, 3, 12}, {TangVillage.WEST, -3, 4}};
    /** The raiders' banners and totems (plan x, z). */
    static final int[][] BANNERS = {{5, 31}, {-5, 31}, {6, -22}, {-6, -22}, {7, 7}, {-7, -7}};
    static final int[][] TOTEMS = {{9, 29}, {-9, 29}, {8, -6}, {-8, 6}, {40, 8}, {-40, -8}, {30, -26}, {-30, 26}};

    /** The lines of its signs: what holds it, its levels, its chief's. */
    public static String[] signLines(RingLevels.Band band) {
        return new String[]{"妖怪占据", "Held by yaoguai", "Lv " + band.lo() + "–" + band.hi(), "Chief Lv " + band.leader()};
    }

    /** Puts up its signs, its raiders' banners and totems, and its chief's hoard, within {@code area}. */
    public static void marks(WorldGenLevel level, TangVillage.Plan plan, BlockPos centre, BoundingBox area, RingLevels.Band band) {
        String[] lines = signLines(band);
        for (Object[] s : SIGNS) {
            BlockPos at = open(level, plan, centre, area, (int) s[0], (int) s[1]);
            if (at == null) continue;
            int rot = (int) s[2];
            if (plan.mirror && (rot == 4 || rot == 12)) rot = 16 - rot;
            level.setBlock(at, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), Block.UPDATE_CLIENTS);
            TangVillagePiece.writeSign(level, at, DyeColor.ORANGE, lines);
        }
        for (int[] b : BANNERS) {
            BlockPos at = open(level, plan, centre, area, b[0], b[1]);
            if (at == null) continue;
            level.setBlock(at, Blocks.BLACK_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, Math.floorMod(b[0] * 3 + b[1], 16)), Block.UPDATE_CLIENTS);
            demonBanner(level, at);
        }
        for (int[] t : TOTEMS) {
            BlockPos at = open(level, plan, centre, area, t[0], t[1]);
            if (at == null) continue;
            totem(level, at, Math.floorMod(t[0] * 5 + t[1], 16));
        }
        BlockPos hoard = open(level, plan, centre, area, HOARD[0], HOARD[1]);
        if (hoard != null) {
            level.setBlock(hoard, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, plan.facing(Direction.SOUTH)), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(hoard) instanceof RandomizableContainerBlockEntity c) c.setLootTable(hoardOf(band), level.getSeed() ^ hoard.asLong());
        }
    }

    /**
     * A totem: a heap of bone, a pole of nether brick three high, a skull on top turned {@code rot},
     * and off its top an arm with a soul lantern hung from it, burning blue (seen far off by night).
     */
    public static void totem(WorldGenLevel level, BlockPos at, int rot) {
        Direction arm = Direction.from2DDataValue(Math.floorMod(rot / 4, 4));
        level.setBlock(at, Blocks.BONE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int y = 1; y <= 3; y++) {
            BlockState pole = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
            if (y == 3) pole = pole.setValue(net.minecraft.world.level.block.CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(arm), true);
            level.setBlock(at.above(y), pole, Block.UPDATE_CLIENTS);
        }
        level.setBlock(at.above(4), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rot), Block.UPDATE_CLIENTS);
        BlockPos out = at.relative(arm);
        if (level.getBlockState(out.above(3)).isAir() && level.getBlockState(out.above(2)).isAir()) {
            level.setBlock(out.above(3), Blocks.NETHER_BRICK_FENCE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(arm.getOpposite()), true), Block.UPDATE_CLIENTS);
            level.setBlock(out.above(2), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), Block.UPDATE_CLIENTS);
        }
    }

    /** The banner at {@code at} made a demon banner: black, a red skull, a red border. */
    public static void demonBanner(WorldGenLevel level, BlockPos at) {
        if (!(level.getBlockEntity(at) instanceof BannerBlockEntity be)) return;
        var patterns = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        BannerPatternLayers layers = new BannerPatternLayers.Builder()
                .add(patterns.getOrThrow(BannerPatterns.GRADIENT_UP), DyeColor.RED)
                .add(patterns.getOrThrow(BannerPatterns.SKULL), DyeColor.RED)
                .add(patterns.getOrThrow(BannerPatterns.BORDER), DyeColor.RED)
                .build();
        var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLACK_BANNER);
        stack.set(net.minecraft.core.component.DataComponents.BANNER_PATTERNS, layers);
        be.fromItem(stack, DyeColor.BLACK);
    }

    /** The open ground at plan (x, z), one over the village's ground, within {@code area}; or null if something stands there. */
    static BlockPos open(WorldGenLevel level, TangVillage.Plan plan, BlockPos centre, BoundingBox area, int x, int z) {
        BlockPos at = centre.offset(plan.planX(x), 1, z);
        if (!area.isInside(at)) return null;
        for (int up = 0; up < 4; up++) {
            BlockPos p = at.above(up);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.below()).blocksMotion()) return p;
        }
        return null;
    }

    // ── The raiders ───────────────────────────────────────────────────

    /** Puts its raiders at their posts and its chief before the shrine, those whose places are within {@code area}. Returns them. */
    public static List<WildYaoguai> muster(WorldGenLevel level, TangVillage.Plan plan, BlockPos centre, BoundingBox area, long layout, RingLevels.Band band) {
        List<WildYaoguai> out = new ArrayList<>();
        List<EntityType<? extends WildYaoguai>> kinds = kinds(plan.variant);
        for (int post = 0; post < POSTS.length; post++) {
            BlockPos at = centre.offset(plan.planX(POSTS[post][0]), 1, POSTS[post][1]);
            if (!area.isInside(at)) continue;
            RandomSource r = RandomSource.create(layout ^ (post * 0x2545F4914F6CDD1DL));
            boolean ogre = false;
            for (int i = 0; i < PER_POST; i++) {
                EntityType<? extends WildYaoguai> kind = kinds.get((post + i) % kinds.size());
                if (kind == ModEntities.OGRE.get()) {
                    if (ogre) kind = ModEntities.GOBLIN.get();
                    ogre = true;
                }
                boolean captain = post < CAPTAINS && i == 0;
                int lv = band.lo() + r.nextInt(band.hi() - band.lo() + 1);
                WildYaoguai m = raider(level, kind, at.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2), at, band,
                        captain ? band.captainOver(lv, r.nextInt(3)) : lv, captain ? WildYaoguai.Rank.CAPTAIN : WildYaoguai.Rank.REGULAR, centre);
                if (m != null) out.add(m);
            }
        }
        BlockPos seat = centre.offset(plan.planX(CHIEF[0]), 1, CHIEF[1]);
        if (area.isInside(seat)) {
            RaidChiefEntity chief = ModGrinds.RAID_CHIEF.get().create(level.getLevel());
            if (chief != null) {
                chief.moveTo(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5, 180f, 0f);
                chief.finalizeSpawn(level, level.getCurrentDifficultyAt(seat), MobSpawnType.STRUCTURE, null);
                chief.become(band.leader());
                chief.restrictTo(seat, CHIEF_REACH);
                GrindXp.mark(chief, band, EnemyTier.BOSS);
                RaidedVillages.markChief(chief, centre, plan.variant, layout);
                level.addFreshEntityWithPassengers(chief);
                out.add(chief);
            }
        }
        return out;
    }

    /** A raider of {@code kind} at {@code at}, kept at {@code post}, of {@code level} and {@code rank}, of the village at {@code village}. */
    public static WildYaoguai raider(WorldGenLevel world, EntityType<? extends WildYaoguai> kind, BlockPos at, BlockPos post, RingLevels.Band band,
                              int level, WildYaoguai.Rank rank, BlockPos village) {
        WildYaoguai m = kind.create(world.getLevel());
        if (m == null) return null;
        BlockPos stand = world.getBlockState(at).isAir() && world.getBlockState(at.above()).isAir() ? at : post;
        m.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, world.getRandom().nextFloat() * 360f, 0f);
        m.finalizeSpawn(world, world.getCurrentDifficultyAt(stand), MobSpawnType.STRUCTURE, null);
        m.become(level, rank);
        m.setPersistenceRequired();
        m.restrictTo(post, POST_REACH);
        GrindXp.mark(m, band, rank == WildYaoguai.Rank.REGULAR ? EnemyTier.GRUNT : EnemyTier.GENERAL);
        RaidedVillages.markRaider(m, village);
        ZonePosts.keep(m, post, POST_REACH);
        world.addFreshEntityWithPassengers(m);
        return m;
    }
}
