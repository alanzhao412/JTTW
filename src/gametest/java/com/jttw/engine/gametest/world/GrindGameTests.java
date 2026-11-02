package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.RaidChiefEntity;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.grind.GrindXp;
import com.jttw.engine.grind.Infestation;
import com.jttw.engine.grind.InfestationSpawner;
import com.jttw.engine.grind.RingLevels;
import com.jttw.engine.grind.VillageClearance;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.village.TangFolkEntity;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.VillageSites;
import com.jttw.engine.village.raid.RaidedVillage;
import com.jttw.engine.village.raid.RaidedVillages;
import com.jttw.engine.world.ZonePosts;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.jttw.engine.gametest.GT.*;

/**
 * The open world's grinding grounds: the raided villages (their band, their sack and marks, their
 * raiders held at their posts and back until their chief falls, then freed for good, its folk home and
 * its freer paid karma) and the infested wilds (their themes by land, their clearance, their heart
 * held again after it falls); and what their kills pay.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class GrindGameTests {

    /** Lays a raided village whole far from everything (its chunks forced), and returns its centre and plan; all undone when the test ends. */
    private static BlockPos layRaided(GameTestHelper h, int far, TangVillage.Plan plan, long layout, RingLevels.Band band) {
        ServerLevel level = h.getLevel();
        BlockPos centre = new BlockPos(far, -61, far);
        BoundingBox all = new BoundingBox(centre.getX() + TangVillage.WEST, level.getMinBuildHeight(), centre.getZ() + TangVillage.NORTH,
                centre.getX() + TangVillage.EAST, level.getMaxBuildHeight() - 1, centre.getZ() + TangVillage.SOUTH);
        List<ChunkPos> chunks = new ArrayList<>();
        for (int cx = (all.minX() >> 4) - 1; cx <= (all.maxX() >> 4) + 1; cx++) for (int cz = (all.minZ() >> 4) - 1; cz <= (all.maxZ() >> 4) + 1; cz++) {
            chunks.add(new ChunkPos(cx, cz));
            level.setChunkForced(cx, cz, true);
            level.getChunk(cx, cz);
        }
        RaidedVillage.lay(level, plan, centre, all, layout, band, true);
        onEnd(h, () -> {
            AABB box = AABB.of(all).inflate(32);
            level.getEntitiesOfClass(Entity.class, box, e -> e instanceof Mob).forEach(Entity::discard);
            RaidedVillages.State.of(level).unfree(centre);
            ZonePosts.State posts = ZonePosts.State.of(level);
            for (ZonePosts.State.Post p : posts.posts()) if (all.isInside(p.post())) posts.forget(p.dimension(), p.post());
            chunks.forEach(c -> level.setChunkForced(c.x, c.z, false));
        });
        return centre;
    }

    private static List<WildYaoguai> raiders(ServerLevel level, BlockPos centre) {
        return level.getEntitiesOfClass(WildYaoguai.class, AABB.of(RaidedVillage.box(centre)).inflate(16),
                m -> m.isAlive() && RaidedVillages.villageOf(m) == centre.asLong() && !(m instanceof RaidChiefEntity));
    }

    // ── The bands ───────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aRaidedVillagesBandIsItsRingsAndFixed(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        for (int d = 0; d <= 12000; d += 250) {
            for (int a = 0; a < 4; a++) {
                int x = (int) (d * Math.cos(a * 1.3)), z = (int) (d * Math.sin(a * 1.3));
                RingLevels.Band b = RaidedVillage.band(seed, new BlockPos(x, 70, z));
                eq(h, b, RaidedVillage.band(seed, new BlockPos(x, 90, z)), "the same village, the same band");
                check(h, b.lo() >= 1 && b.hi() <= 100 && b.hi() - b.lo() + 1 == RingLevels.WIDTH, "a band of seven within 1-100: " + b);
                double ring = RingLevels.levelAt(Math.hypot(x, z));
                check(h, Math.abs((b.lo() + b.hi()) / 2.0 - ring) <= RingLevels.JITTER + 3.5, "round its ring's level " + ring + ": " + b);
            }
        }
        check(h, RaidedVillage.band(seed, new BlockPos(300, 70, 0)).lo() <= 8, "by spawn, the first levels");
        check(h, RaidedVillage.band(seed, new BlockPos(0, 70, -1500)).contains(28), "White Bone Ridge's ring, its band's levels");
        check(h, RaidedVillage.band(seed, new BlockPos(12000, 70, 0)).hi() == 100, "past the last ring, the last levels");
        h.succeed();
    }

    // ── A raided village, held and freed ────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 400, batch = "raided_village_laid")
    public static void aRaidedVillageIsSackedMarkedAndHeld(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TangVillage.Plan plan = TangVillage.plan(TangVillage.Variant.MOUNTAIN, 4242L);
        RingLevels.Band band = new RingLevels.Band(40, 46);
        BlockPos centre = layRaided(h, 52000, plan, 4242L, band);
        BoundingBox box = RaidedVillage.box(centre);
        int doors = 0, signs = 0, banners = 0, skulls = 0, webs = 0;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), centre.getY(), box.minZ(), box.maxX(), centre.getY() + TangVillage.TOP, box.maxZ())) {
            var s = level.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock) doors++;
            if (s.is(Blocks.BLACK_BANNER)) banners++;
            if (s.is(Blocks.SKELETON_SKULL)) skulls++;
            if (s.is(Blocks.COBWEB)) webs++;
            if (s.is(Blocks.DARK_OAK_SIGN) && level.getBlockEntity(p) instanceof SignBlockEntity sign) {
                signs++;
                String all = sign.getFrontText().getMessage(2, false).getString() + " " + sign.getFrontText().getMessage(3, false).getString();
                check(h, all.contains("Lv 40–46") && all.contains("Chief Lv 49"), "its sign tells its levels: " + all);
            }
        }
        eq(h, 0, doors, "every door smashed");
        check(h, signs >= 3, "signs at its edges: " + signs);
        check(h, banners >= 3 && skulls >= 4 && webs >= 10, "banners " + banners + ", totems " + skulls + ", cobwebs " + webs);
        for (TangVillage.Chest c : plan.chests) {
            if (level.getBlockEntity(centre.offset(plan.planX(c.x()), c.y(), c.z())) instanceof ChestBlockEntity chest) {
                check(h, chest.getLootTable() == null && chest.isEmpty(), "the folk's chests emptied");
            }
        }
        BlockPos hoard = centre.offset(plan.planX(RaidedVillage.HOARD[0]), 1, RaidedVillage.HOARD[1]);
        boolean hoarded = false;
        for (int up = 0; up < 4; up++) {
            if (level.getBlockEntity(hoard.above(up)) instanceof ChestBlockEntity c && RaidedVillage.HOARD_MID.equals(c.getLootTable())) hoarded = true;
        }
        check(h, hoarded, "the chief's hoard before the shrine");
        h.succeedWhen(() -> {
            List<WildYaoguai> raiders = raiders(level, centre);
            eq(h, RaidedVillage.POSTS_N * RaidedVillage.PER_POST, raiders.size(), "every post held");
            int captains = 0;
            for (WildYaoguai m : raiders) {
                check(h, ZonePosts.kept(m), "each kept at its post");
                if (m.rank() == WildYaoguai.Rank.CAPTAIN) {
                    captains++;
                    check(h, m.mobLevel() > band.lo() + 2 && m.mobLevel() <= band.hi() + 1, "a captain a few over: " + m.mobLevel());
                    eq(h, EnemyTier.GENERAL, GrindXp.tierOf(m), "a captain pays a General's");
                } else {
                    check(h, band.contains(m.mobLevel()), "a regular of the band: " + m.mobLevel());
                }
            }
            eq(h, RaidedVillage.CAPTAINS, captains, "a captain at the gate and one on the square");
            List<RaidChiefEntity> chief = level.getEntitiesOfClass(RaidChiefEntity.class, AABB.of(box).inflate(8));
            eq(h, 1, chief.size(), "the chief before the shrine");
            eq(h, 49, chief.get(0).mobLevel(), "the chief at the band's top and three");
            eq(h, EnemyTier.BOSS, GrindXp.tierOf(chief.get(0)), "the chief pays a Boss's");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400, batch = "raided_village_freed")
    public static void raidersComeBackUntilTheChiefFallsThenTheVillageIsFreedForGood(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TangVillage.Plan plan = TangVillage.plan(TangVillage.Variant.PLAINS, 777L);
        RingLevels.Band band = new RingLevels.Band(20, 26);
        BlockPos centre = layRaided(h, 56000, plan, 777L, band);
        ServerPlayer p = com.jttw.engine.gametest.character.CharTest.as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        BlockPos gate = centre.offset(plan.planX(RaidedVillage.POSTS[0][0]), 1, RaidedVillage.POSTS[0][1]);
        h.runAfterDelay(5, () -> {
            check(h, ZonePosts.loaded(level, gate, RaidedVillage.POST_REACH), "the gate's ground awake");
            // the gate's raiders fall
            List<WildYaoguai> atGate = level.getEntitiesOfClass(WildYaoguai.class, new AABB(gate).inflate(RaidedVillage.POST_REACH + ZonePosts.STRAY),
                    m -> gate.equals(ZonePosts.postOf(m)));
            eq(h, RaidedVillage.PER_POST, atGate.size(), "the gate held");
            for (WildYaoguai m : atGate) m.kill();
            ZonePosts.State posts = ZonePosts.State.of(level);
            eq(h, RaidedVillage.PER_POST, posts.fallen(level.dimension(), gate), "its dead written down");
            check(h, ZonePosts.look(level, gate, List.of()).isEmpty(), "found cleared, not yet held again");
            posts.cleared(level.dimension(), gate, level.getGameTime() - ZonePosts.RESPAWN - 1);
            List<Mob> back = ZonePosts.look(level, gate, List.of());
            eq(h, RaidedVillage.PER_POST, back.size(), "twenty minutes on, no one near: the gate held again");
            for (Mob m : back) check(h, RaidedVillages.villageOf(m) == centre.asLong() && band.contains(((WildYaoguai) m).mobLevel())
                    || ((WildYaoguai) m).rank() == WildYaoguai.Rank.CAPTAIN, "the same raiders, of the village's band");
            // they fall again, and before they come back the chief falls
            for (Mob m : back) m.kill();
            int karma = Morality.karma(p);
            RaidChiefEntity chief = level.getEntitiesOfClass(RaidChiefEntity.class, AABB.of(RaidedVillage.box(centre)).inflate(8)).get(0);
            chief.setHealth(1f);
            chief.hurt(level.damageSources().playerAttack(p), 1000f);
            check(h, chief.isDeadOrDying(), "the chief falls");
            check(h, RaidedVillages.freed(level, centre), "the village freed");
            eq(h, karma + RaidedVillages.FREE_KARMA + Morality.SLAY_MONSTER, Morality.karma(p), "its freer paid karma");
            eq(h, 0, raiders(level, centre).size(), "its raiders fled");
            eq(h, 0, posts.fallen(level.dimension(), gate), "and their posts forgotten");
            // a raider put back by any way is turned away
            WildYaoguai again = RaidedVillage.raider(level, ModEntities.GOBLIN.get(), gate, gate, band, 22, WildYaoguai.Rank.REGULAR, centre);
            check(h, again == null || level.getEntity(again.getUUID()) == null, "none comes back to a freed village");
        });
        h.runAfterDelay(20, () -> {
            List<TangFolkEntity> folk = level.getEntitiesOfClass(TangFolkEntity.class, AABB.of(RaidedVillage.box(centre)));
            eq(h, plan.residents.size(), folk.size(), "its folk come home");
            for (TangFolkEntity f : folk) {
                check(h, f.bed() != null && level.getBlockState(f.bed()).getBlock() instanceof net.minecraft.world.level.block.BedBlock, "each to its bed");
                check(h, !f.getOffers().isEmpty(), f.calling().id() + " trades");
            }
            BoundingBox box = RaidedVillage.box(centre);
            for (BlockPos q : BlockPos.betweenClosed(box.minX(), centre.getY() + 1, box.minZ(), box.maxX(), centre.getY() + 6, box.maxZ())) {
                var s = level.getBlockState(q);
                check(h, !s.is(Blocks.BLACK_BANNER) && !s.is(Blocks.SKELETON_SKULL), "the raid's marks taken down");
            }
            eq(h, 0, raiders(level, centre).size(), "and none of its raiders back");
            h.succeed();
        });
    }

    // ── What a kill pays ────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aGrindingGroundsKillPaysAsAZonesAtItsLevel(GameTestHelper h) {
        ServerPlayer p = com.jttw.engine.gametest.character.CharTest.as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        RingLevels.Band band = new RingLevels.Band(48, 54);
        BiomeLevelBand waters = BiomeLevelBand.forBiome(Zones.MYSTICAL_WATERS_BIOME.location());
        check(h, waters != null && GrindXp.zoneBandFor(50) == waters, "level 50 is the Mystical Waters' band");
        WildYaoguai grunt = mark(h, ModEntities.WOLFMAN.get().create(h.getLevel()), 50, WildYaoguai.Rank.REGULAR, band, EnemyTier.GRUNT);
        WildYaoguai captain = mark(h, ModEntities.WOLFMAN.get().create(h.getLevel()), 54, WildYaoguai.Rank.CAPTAIN, band, EnemyTier.GENERAL);
        RaidChiefEntity chief = com.jttw.engine.registry.ModGrinds.RAID_CHIEF.get().create(h.getLevel());
        chief.become(band.leader());
        GrindXp.mark(chief, band, EnemyTier.BOSS);
        onEnd(h, chief::discard);
        eq(h, waters.xpForTier(EnemyTier.GRUNT), GrindXp.forKill(grunt, 50), "a regular: a Mystical Waters grunt's");
        eq(h, waters.xpForTier(EnemyTier.GENERAL), GrindXp.forKill(captain, 50), "a captain: a General's");
        eq(h, GrindXp.zoneBandFor(57).xpForTier(EnemyTier.BOSS), GrindXp.forKill(chief, 50), "the chief (57): a Boss's of his level's band (Hell's)");
        eq(h, 0L, GrindXp.forKill(grunt, band.hi() + GrindXp.OVER_TOP + 1), "nothing for one past the band's top and five");
        check(h, GrindXp.forKill(grunt, 30) == waters.xpForTier(EnemyTier.GRUNT), "punching up still pays");
        WildYaoguai low = mark(h, ModEntities.GOBLIN.get().create(h.getLevel()), 12, WildYaoguai.Rank.REGULAR, new RingLevels.Band(10, 16), EnemyTier.GRUNT);
        eq(h, 0L, GrindXp.forKill(low, 21), "a creature under 20 pays a character of 20 nothing");
        eq(h, LevelManager.vanillaMobXp(low, 12), GrindXp.forKill(low, 12), "below 20, as a vanilla monster");
        // and through the kill itself
        PlayerLevel.setLevel(p, 50);
        long xp = PlayerLevel.getXp(p);
        LevelManager.onEntityKilled(new LivingDeathEvent(grunt, h.getLevel().damageSources().playerAttack(p)));
        eq(h, xp + waters.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a kill paid it");
        h.succeed();
    }

    private static WildYaoguai mark(GameTestHelper h, WildYaoguai m, int lv, WildYaoguai.Rank rank, RingLevels.Band band, EnemyTier tier) {
        m.become(lv, rank);
        GrindXp.mark(m, band, tier);
        onEnd(h, m::discard);
        return m;
    }

    // ── Where ───────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void noRaidedVillageStandsInAZone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long seed = level.getSeed();
        for (String v : List.of("plains", "mountain", "oasis")) {
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(JTTWEngine.id("raided_village_" + v));
            check(h, structure != null, "the raided village is a structure: " + v);
            for (Zones.Zone zone : Zones.MAP.zones()) {
                for (int[] c : zone.centres(seed)) {
                    check(h, at(level, structure, seed, c[0], c[1]).isEmpty(), "no raided village at the heart of " + zone.id());
                    check(h, at(level, structure, seed, c[0] + zone.layout().radius(), c[1]).isEmpty(), "nor at its edge");
                    check(h, !VillageSites.clearOfZones(seed, c[0] + zone.layout().margin(), c[1]), "nor beside it");
                }
            }
        }
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(VillageClearance.RAIDED_VILLAGES);
        check(h, set != null && set.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement, "placed as villages are");
        h.succeed();
    }

    private static Optional<Structure.GenerationStub> at(ServerLevel level, Structure structure, long seed, int x, int z) {
        var gen = level.getChunkSource().getGenerator();
        var ctx = new Structure.GenerationContext(level.registryAccess(), gen, gen.getBiomeSource(), level.getChunkSource().randomState(),
                level.getStructureManager(), seed, new ChunkPos(x >> 4, z >> 4), level, b -> true);
        return structure.findValidGenerationPoint(ctx);
    }

    /** A ground of one biome everywhere, for judging patches. */
    private static Infestation.Ground ground(ServerLevel level, ResourceKey<Biome> biome) {
        Holder<Biome> b = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome);
        long seed = level.getSeed();
        RegistryAccess access = level.registryAccess();
        return new Infestation.Ground() {
            public long seed() { return seed; }
            public Holder<Biome> biomeAt(int x, int z) { return b; }
            public RegistryAccess registries() { return access; }
        };
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void theWildsAreInfestedByTheirLandAndClearOfVillagesZonesAndSpawn(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        eq(h, Infestation.Theme.JUNGLE, Infestation.themeOf(biomes.getHolderOrThrow(Biomes.JUNGLE)), "a jungle's: jungle demons");
        eq(h, Infestation.Theme.JUNGLE, Infestation.themeOf(biomes.getHolderOrThrow(Biomes.BAMBOO_JUNGLE)), "a bamboo jungle's too");
        eq(h, Infestation.Theme.MOUNTAIN, Infestation.themeOf(biomes.getHolderOrThrow(Biomes.JAGGED_PEAKS)), "a peak's: mountain demons");
        eq(h, Infestation.Theme.MOUNTAIN, Infestation.themeOf(biomes.getHolderOrThrow(Biomes.WINDSWEPT_HILLS)), "windswept hills' too");
        eq(h, Infestation.Theme.FOREST, Infestation.themeOf(biomes.getHolderOrThrow(Biomes.DARK_FOREST)), "a dark forest's: forest demons");
        eq(h, null, Infestation.themeOf(biomes.getHolderOrThrow(Biomes.PLAINS)), "the plains: none");
        eq(h, null, Infestation.themeOf(biomes.getHolderOrThrow(Biomes.DESERT)), "the desert: none");
        eq(h, null, Infestation.themeOf(biomes.getHolderOrThrow(Zones.WHITE_BONE_RIDGE)), "a zone: none");
        eq(h, ModEntities.OGRE.get(), Infestation.Theme.MOUNTAIN.heart(), "a mountain's heart: ogres");
        long seed = level.getSeed();
        Infestation.Ground jungle = ground(level, Biomes.JUNGLE), plains = ground(level, Biomes.PLAINS);
        int judged = 0, infested = 0, blocked = 0;
        for (int cx = -14; cx <= 14; cx++) for (int cz = -14; cz <= 14; cz++) {
            Infestation.Candidate c = Infestation.candidate(seed, cx, cz);
            if (c == null) continue;
            judged++;
            boolean villageNear = false;
            int range = (c.radius() + Infestation.GAP + 63) / 16;
            for (var key : VillageClearance.ALL) {
                villageNear |= VillageClearance.near(level.registryAccess(), key, seed, new ChunkPos(c.x() >> 4, c.z() >> 4), range, null);
            }
            eq(h, !villageNear, Infestation.clearOfVillages(plains, c), "in village country, never where a village may be");
            if (villageNear) blocked++;
            Optional<Infestation.Patch> patch = Infestation.judge(jungle, cx, cz);
            if (patch.isEmpty()) continue;
            infested++;
            Infestation.Patch pt = patch.get();
            check(h, Math.hypot(pt.x(), pt.z()) >= Infestation.SPAWN_CLEAR + pt.radius(), "clear of spawn");
            for (Zones.Zone zone : Zones.MAP.zones()) {
                check(h, zone.distanceToCentre(seed, pt.x(), pt.z()) >= zone.layout().margin() + pt.radius(), "clear of " + zone.id());
            }
            eq(h, RingLevels.bandAt(seed, pt.x(), pt.z()), pt.band(), "its band its ring's");
            eq(h, Infestation.Theme.JUNGLE, pt.theme(), "its theme its land's");
        }
        check(h, judged > 300 && infested > 100 && blocked > 50, "patches judged " + judged + ", infested " + infested + ", kept from villages " + blocked);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void anInfestedWildsHeartIsHeldAgainAfterItFalls(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 2, 2));
        RingLevels.Band band = new RingLevels.Band(60, 66);
        Infestation.Patch patch = new Infestation.Patch(new Infestation.Candidate(9999, 9999, mid.getX(), mid.getZ(), 120, 2), Infestation.Theme.MOUNTAIN, band);
        onEnd(h, () -> {
            level.getEntitiesOfClass(WildYaoguai.class, new AABB(mid).inflate(40), m -> InfestationSpawner.patchOf(m) == patch.key()).forEach(Entity::discard);
            InfestationSpawner.State.of(level).held(patch.key());
        });
        List<WildYaoguai> held = InfestationSpawner.heart(level, patch, true, false, false);
        eq(h, 2 + InfestationSpawner.HEART_GUARD, held.size(), "its minibosses and their guard");
        int minis = 0;
        for (WildYaoguai m : held) {
            check(h, m.isPersistenceRequired() && m.hasRestriction(), "kept at the heart");
            if (m.rank() == WildYaoguai.Rank.MINIBOSS) {
                minis++;
                eq(h, ModEntities.OGRE.get(), m.getType(), "a mountain's heart: ogres");
                eq(h, band.miniboss(), m.mobLevel(), "a miniboss one over the band");
                eq(h, EnemyTier.GENERAL, GrindXp.tierOf(m), "pays a General's");
            } else {
                check(h, band.contains(m.mobLevel()), "its guard of the band");
            }
        }
        eq(h, 2, minis, "two minibosses");
        check(h, InfestationSpawner.heart(level, patch, true, false, false).isEmpty(), "held, not held twice");
        for (WildYaoguai m : held) m.kill();
        long fell = InfestationSpawner.State.of(level).heartFell(patch.key());
        check(h, fell != 0, "its fall written down");
        check(h, InfestationSpawner.heart(level, patch, true, false, false).isEmpty(), "not back at once");
        InfestationSpawner.State.of(level).fell(patch.key(), level.getGameTime() - ZonePosts.RESPAWN - 1);
        check(h, InfestationSpawner.heart(level, patch, true, true, false).isEmpty(), "not under someone's nose");
        eq(h, 2 + InfestationSpawner.HEART_GUARD, InfestationSpawner.heart(level, patch, true, false, false).size(), "twenty minutes on: held again");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aChiefsStompIsTelegraphedAndStrikesAllRoundHim(GameTestHelper h) {
        RaidChiefEntity chief = h.spawn(com.jttw.engine.registry.ModGrinds.RAID_CHIEF.get(), new BlockPos(2, 2, 2));
        onEnd(h, chief::discard);
        chief.become(43);
        eq(h, 40, chief.challenger(), "built for his band's top");
        eq(h, RaidChiefEntity.STOMP_WINDUP, chief.slamWindupTicks(), "a wind-up to step out of");
        check(h, chief.slamReach() == RaidChiefEntity.STOMP_REACH, "six blocks round him");
        LivingEntity golem = h.spawn(net.minecraft.world.entity.EntityType.IRON_GOLEM, new BlockPos(4, 2, 2));
        onEnd(h, golem::discard);
        golem.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1e6);
        golem.setHealth(1e6f);
        float before = golem.getHealth();
        chief.slam();
        check(h, Math.abs(chief.slamDamage() - (before - golem.getHealth())) < chief.slamDamage() * 1e-3, "the stomp's hurt: " + (before - golem.getHealth()));
        h.succeed();
    }
}
