package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModVillages;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformationManager;
import com.jttw.engine.village.Calling;
import com.jttw.engine.village.Coins;
import com.jttw.engine.village.Talismans;
import com.jttw.engine.village.TangFolkEntity;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.TangVillagePiece;
import com.jttw.engine.village.VillageSites;
import com.jttw.engine.village.Wards;
import com.jttw.engine.village.YaoguaiFear;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/**
 * The Tang villages: their folk trade in copper and silver and sell the mod's goods; the
 * money-changer changes at par; no mortal sells a talisman, and the supernatural world's merchants
 * do; the talismans strike, ward, appease and keep monsters away; killing a villager costs karma;
 * mortals flee a Yaoguai (vanilla's villagers too) and will not trade with one, unless the demon is
 * disguised; a village is laid whole, every villager with a bed; and no village stands in a zone.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TangVillageGameTests {

    private static final TagKey<Item> TALISMANS = ItemTags.create(JTTWEngine.id("talismans"));

    private static TangFolkEntity folk(GameTestHelper h, Calling calling, boolean woman) {
        TangFolkEntity f = h.spawn(calling.mortal() ? ModVillages.TANG_VILLAGER.get() : ModVillages.SPIRIT_MERCHANT.get(), new BlockPos(2, 2, 2));
        f.setCalling(calling, woman);
        f.setNoAi(true);
        return f;
    }

    private static MerchantOffer selling(TangFolkEntity f, Item item) {
        return f.getOffers().stream().filter(o -> o.getResult().is(item)).findFirst().orElse(null);
    }

    private static MerchantOffer buying(TangFolkEntity f, Item item) {
        return f.getOffers().stream().filter(o -> o.getCostA().is(item)).findFirst().orElse(null);
    }

    @GameTest(template = PLATFORM)
    public static void aVillagerTradesInCopperAndSilverForTheModsGoods(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        TangFolkEntity inn = folk(h, Calling.INNKEEPER, false);
        Item copper = ModVillages.COPPER_CASH.get(), silver = ModVillages.SILVER_TAEL.get();
        for (MerchantOffer o : inn.getOffers()) {
            boolean coinIn = o.getCostA().is(copper) || o.getCostA().is(silver), coinOut = o.getResult().is(copper) || o.getResult().is(silver);
            check(h, coinIn != coinOut, "every trade is goods for coin or coin for goods: " + o.getCostA() + " -> " + o.getResult());
            check(h, !o.getCostA().is(Items.EMERALD) && !o.getResult().is(Items.EMERALD), "no emeralds");
        }
        MerchantOffer buns = selling(inn, ModVillages.STEAMED_BUN.get());
        check(h, buns != null, "the innkeeper sells steamed buns");
        ItemStack purse = new ItemStack(copper, 10);
        check(h, buns.satisfiedBy(purse, ItemStack.EMPTY), "for copper");
        inn.setTradingPlayer(p);
        check(h, buns.take(purse, ItemStack.EMPTY), "paid");
        inn.notifyTrade(buns);
        eq(h, 4, purse.getCount(), "six wen spent");
        eq(h, 3, buns.getResult().getCount(), "three buns");
        eq(h, Morality.TRADE, Morality.karma(p), "and a fair deal is a good deed");
        // the dearer goods in silver
        TangFolkEntity herbalist = folk(h, Calling.HERBALIST, false);
        MerchantOffer pill = selling(herbalist, ModVillages.QI_GATHERING_PILL.get());
        check(h, pill != null && pill.getCostA().is(silver) && pill.getCostB().is(copper), "a Qi pill costs a tael and copper");
        eq(h, 96, pill.getCostA().getCount() * Coins.COPPER_PER_TAEL + pill.getCostB().getCount(), "a tael and 32 wen");
        // goods sold to them for copper
        MerchantOffer wheat = buying(folk(h, Calling.FARMER, false), Items.WHEAT);
        check(h, wheat != null && wheat.getResult().is(copper), "the farmer buys wheat for copper");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theMoneyChangerChangesAtPar(GameTestHelper h) {
        TangFolkEntity changer = folk(h, Calling.MERCHANT, false);
        Item copper = ModVillages.COPPER_CASH.get(), silver = ModVillages.SILVER_TAEL.get();
        MerchantOffer toSilver = selling(changer, silver), toCopper = buying(changer, silver);
        check(h, toSilver != null && toCopper != null, "silver both ways");
        ItemStack coins = new ItemStack(copper, 64);
        check(h, toSilver.take(coins, ItemStack.EMPTY) && coins.isEmpty(), "sixty-four copper for a tael");
        eq(h, 1, toSilver.getResult().getCount(), "one tael");
        ItemStack tael = new ItemStack(silver);
        check(h, toCopper.take(tael, ItemStack.EMPTY) && tael.isEmpty(), "a tael for copper");
        eq(h, 64, toCopper.getResult().getCount(), "sixty-four copper back");
        check(h, toCopper.getResult().is(copper), "in copper");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void noMortalSellsATalisman(GameTestHelper h) {
        for (Calling c : Calling.values()) {
            if (!c.mortal()) continue;
            for (boolean woman : new boolean[]{false, true}) {
                TangFolkEntity f = folk(h, c, woman);
                for (MerchantOffer o : f.getOffers()) {
                    check(h, !o.getResult().is(TALISMANS), c.id() + " sells " + o.getResult().getHoverName().getString());
                }
                f.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theSupernaturalWorldsMerchantsSellTalismansForSilver(GameTestHelper h) {
        for (Calling c : List.of(Calling.GHOST_MERCHANT, Calling.CELESTIAL_MERCHANT)) {
            TangFolkEntity f = folk(h, c, false);
            List<MerchantOffer> talismans = f.getOffers().stream().filter(o -> o.getResult().is(TALISMANS)).toList();
            check(h, talismans.size() >= 3, c.id() + " sells talismans, charms and incense");
            for (MerchantOffer o : talismans) check(h, o.getCostA().is(ModVillages.SILVER_TAEL.get()), c.id() + ": priced in silver");
            check(h, !f.isMortal(), "a spirit");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void theScholarSellsMapsToTheZones(GameTestHelper h) {
        TangFolkEntity scholar = folk(h, Calling.SCHOLAR, false);
        List<MerchantOffer> maps = scholar.getOffers().stream().filter(o -> o.getResult().is(Items.FILLED_MAP)).toList();
        eq(h, 8, maps.size(), "a map of every zone, Fengdu and Mount Kunlun");
        List<String> names = maps.stream().map(o -> o.getResult().getHoverName().getString()).toList();
        check(h, names.contains("Map to White Bone Ridge") && names.contains("Map to Mount Kunlun") && names.contains("Map to Fengdu"), "named: " + names);
        for (MerchantOffer o : maps) {
            check(h, o.getResult().has(DataComponents.MAP_DECORATIONS) && !o.getResult().get(DataComponents.MAP_DECORATIONS).decorations().isEmpty(),
                    "marked with a cross");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void talismansStrikeAndWardAgainstTheUnclean(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
        zombie.setNoAi(true);
        var cow = h.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        check(h, Talismans.isUnclean(zombie) && !Talismans.isUnclean(cow), "the walking dead are unclean, a cow is not");
        near(h, 10, Talismans.afterTalismans(p, zombie, 10), "no talisman burned");
        ItemStack quell = new ItemStack(ModVillages.DEMON_QUELLING_TALISMAN.get(), 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, quell);
        quell.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        check(h, p.hasEffect(ModVillages.DEMON_QUELLING), "burned, its power on its user");
        eq(h, 1, p.getMainHandItem().getCount(), "and the talisman spent");
        near(h, 12.5, Talismans.afterTalismans(p, zombie, 10), "25% harder on the unclean");
        near(h, 10, Talismans.afterTalismans(p, cow, 10), "no harder on a cow");
        var hit = incoming(zombie, h.getLevel().damageSources().playerAttack(p), 10);
        NeoForge.EVENT_BUS.post(hit);
        near(h, 12.5, hit.getAmount(), "as the blow lands");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModVillages.GHOST_WARD, 200));
        near(h, 7.5, Talismans.afterTalismans(zombie, p, 10), "a ghost-warding talisman: 25% softer");
        p.getInventory().add(new ItemStack(ModVillages.PEACE_CHARM.get()));
        near(h, 10 * 0.75 * 0.85, Talismans.afterTalismans(zombie, p, 10), "and a peace charm carried: 15% softer again");
        p.removeEffect(ModVillages.GHOST_WARD);
        near(h, 8.5, Talismans.afterTalismans(zombie, p, 10), "the charm alone");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void incenseAppeasesTheLesserUnclean(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
        zombie.setTarget(p);
        check(h, zombie.getTarget() == p, "a zombie hunts the player");
        zombie.setTarget(null);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModVillages.INCENSE_CALM, 200));
        zombie.setTarget(p);
        check(h, zombie.getTarget() == null, "but not one who burns incense");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aHouseWardKeepsMonstersFromComingOfThemselves(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        Zombie zombie = EntityType.ZOMBIE.create(level);
        zombie.moveTo(at.getX() + 4.5, at.getY() + 1, at.getZ() + 0.5);
        var before = new MobSpawnEvent.PositionCheck(zombie, level, MobSpawnType.NATURAL, null);
        NeoForge.EVENT_BUS.post(before);
        check(h, before.getResult() != MobSpawnEvent.PositionCheck.Result.FAIL, "no ward yet");
        Wards.of(level).add(at, level.getGameTime() + 100);
        var warded = new MobSpawnEvent.PositionCheck(zombie, level, MobSpawnType.NATURAL, null);
        NeoForge.EVENT_BUS.post(warded);
        eq(h, MobSpawnEvent.PositionCheck.Result.FAIL, warded.getResult(), "warded: no monster comes of itself");
        var spawner = new MobSpawnEvent.PositionCheck(zombie, level, MobSpawnType.SPAWNER, null);
        NeoForge.EVENT_BUS.post(spawner);
        check(h, spawner.getResult() != MobSpawnEvent.PositionCheck.Result.FAIL, "a spawner is not stopped");
        check(h, !Wards.of(level).warded(at.getX() + Wards.RADIUS + 3, at.getY(), at.getZ(), level.getGameTime()), "beyond its reach, nothing");
        check(h, !Wards.of(level).warded(at.getX(), at.getY(), at.getZ(), level.getGameTime() + 101), "and it lapses");
        zombie.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void killingAVillagerCostsKarma(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        TangFolkEntity farmer = folk(h, Calling.FARMER, false);
        eq(h, Morality.KILL_VILLAGER, Morality.killDeed(p, farmer), "killing a villager");
        farmer.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        eq(h, Morality.KILL_VILLAGER, Morality.karma(p), "costs karma");
        TangFolkEntity ghost = folk(h, Calling.GHOST_MERCHANT, false);
        eq(h, Morality.KILL_VILLAGER, Morality.killDeed(p, ghost), "the ghost market's shopkeepers are innocents too");
        h.succeed();
    }

    // ── Demons ───────────────────────────────────────────────────────

    /** A player in the level (mobs see only those), removed at the test's end. */
    private static ServerPlayer present(GameTestHelper h, ReincarnationOutcome outcome, BlockPos at) {
        ServerPlayer p = as(h, outcome);
        BlockPos abs = h.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        h.getLevel().addNewPlayer(p);
        onEnd(h, () -> p.serverLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED));
        return p;
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "villager_flees_demon")
    public static void aTangVillagerFleesAYaoguai(GameTestHelper h) {
        openAir(h);
        TangFolkEntity f = h.spawn(ModVillages.TANG_VILLAGER.get(), new BlockPos(2, 2, 2));
        f.setCalling(Calling.FARMER, false);
        ServerPlayer demon = present(h, ReincarnationOutcome.BULL_DEMON_KING, new BlockPos(2, 2, 4));
        check(h, YaoguaiFear.dreaded(demon) && YaoguaiFear.frightens(f, demon), "a Yaoguai frightens mortals");
        double start = f.distanceTo(demon);
        h.succeedWhen(() -> check(h, f.distanceTo(demon) > start + 3, "the villager runs from the demon: " + f.distanceTo(demon)));
    }

    @GameTest(template = PLATFORM)
    public static void onlyAnUndisguisedYaoguaiFrightens(GameTestHelper h) {
        TangFolkEntity f = folk(h, Calling.FARMER, false);
        ServerPlayer heavenly = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, !YaoguaiFear.dreaded(heavenly) && !YaoguaiFear.frightens(f, heavenly), "no one fears a heavenly warrior");
        ServerPlayer demon = as(h, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS);
        check(h, YaoguaiFear.frightens(f, demon), "a demon is feared");
        check(h, f.refusal(demon) != null, "and refused");
        check(h, f.refusal(heavenly) == null, "while others are served");
        f.interact(demon, InteractionHand.MAIN_HAND);
        check(h, f.getTradingPlayer() == null, "no trading with a demon");
        // a demon in a mortal shape passes
        TransformationManager.transform(demon, EntityType.VILLAGER);
        check(h, YaoguaiFear.disguised(demon) && !YaoguaiFear.dreaded(demon), "disguised as a villager, it passes");
        check(h, f.refusal(demon) == null, "and is served");
        // until it strikes one
        TangFolkEntity victim = folk(h, Calling.SMITH, false);
        victim.hurt(h.getLevel().damageSources().playerAttack(demon), 1);
        check(h, YaoguaiFear.revealed(demon) && YaoguaiFear.dreaded(demon), "striking a villager, it is revealed");
        TransformationManager.demorph(demon);
        // a demon's own true shape, or a monster's, fools no one
        demon.getPersistentData().remove(YaoguaiFear.KEY_REVEALED);
        TransformationManager.transform(demon, EntityType.ZOMBIE);
        check(h, !YaoguaiFear.disguised(demon), "a zombie's shape is no disguise");
        TransformationManager.demorph(demon);
        TransformationManager.transform(demon, com.jttw.engine.registry.ModEntities.GREAT_WHITE_BULL.get());
        check(h, !YaoguaiFear.disguised(demon), "nor the Great White Bull's");
        TransformationManager.demorph(demon);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theSupernaturalWorldTradesWithDemons(GameTestHelper h) {
        ServerPlayer demon = as(h, ReincarnationOutcome.SAURIAN_DEMON_KING);
        check(h, folk(h, Calling.GHOST_MERCHANT, false).refusal(demon) == null, "the ghost market deals with a demon");
        check(h, folk(h, Calling.CELESTIAL_MERCHANT, false).refusal(demon) == null, "and so does Heaven's merchant");
        ServerPlayer outlaw = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        Morality.set(outlaw, Morality.ENEMY);
        check(h, folk(h, Calling.CELESTIAL_MERCHANT, false).refusal(outlaw) != null, "but Heaven does not deal with its outlaws");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "villager_panics_at_demon")
    public static void vanillaVillagersTakeAYaoguaiForAHostile(GameTestHelper h) {
        openAir(h);
        Villager v = h.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        ServerPlayer demon = present(h, ReincarnationOutcome.BULL_DEMON_KING, new BlockPos(2, 2, 4));
        h.succeedWhen(() -> check(h, v.getBrain().getMemory(MemoryModuleType.NEAREST_HOSTILE).filter(e -> e == demon).isPresent(),
                "the villager marks the demon as a hostile, and panics"));
    }

    // ── The villages ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 400, batch = "tang_village_laid")
    public static void aVillageIsLaidWholeEachVillagerWithABed(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TangVillage.Plan plan = TangVillage.plan(TangVillage.Variant.PLAINS, 99L);
        BlockPos centre = new BlockPos(48000, -61, 48000);
        BoundingBox all = new BoundingBox(centre.getX() + TangVillage.WEST, level.getMinBuildHeight(), centre.getZ() + TangVillage.NORTH,
                centre.getX() + TangVillage.EAST, level.getMaxBuildHeight() - 1, centre.getZ() + TangVillage.SOUTH);
        List<ChunkPos> chunks = new ArrayList<>();
        for (int cx = all.minX() >> 4; cx <= all.maxX() >> 4; cx++) for (int cz = all.minZ() >> 4; cz <= all.maxZ() >> 4; cz++) {
            chunks.add(new ChunkPos(cx, cz));
            level.setChunkForced(cx, cz, true);
            level.getChunk(cx, cz);
        }
        TangVillagePiece.lay(level, plan, centre, all, true);
        onEnd(h, () -> {
            level.getEntitiesOfClass(TangFolkEntity.class, AABB.of(all)).forEach(Entity::discard);
            chunks.forEach(c -> level.setChunkForced(c.x, c.z, false));
        });
        int signs = 0, chests = 0;
        for (var s : plan.signs) if (level.getBlockEntity(centre.offset(plan.planX(s.x()), s.y(), s.z())) instanceof SignBlockEntity) signs++;
        for (var c : plan.chests) {
            if (level.getBlockEntity(centre.offset(plan.planX(c.x()), c.y(), c.z())) instanceof ChestBlockEntity box && box.getLootTable() != null) chests++;
        }
        eq(h, plan.signs.size(), signs, "every sign up");
        eq(h, plan.chests.size(), chests, "every chest stocked");
        check(h, level.getBlockState(centre).is(Blocks.WATER), "the well at the crossroads");
        for (TangVillage.Resident r : plan.residents) {
            BlockPos at = centre.offset(plan.planX(r.x()), 1, r.z());
            check(h, !level.getBlockState(at).blocksMotion() && !level.getBlockState(at.above()).blocksMotion(), r.calling().id() + " is put in the open");
        }
        h.succeedWhen(() -> {
            List<TangFolkEntity> folk = level.getEntitiesOfClass(TangFolkEntity.class, AABB.of(all));
            eq(h, plan.residents.size(), folk.size(), "every resident put in place");
            for (Calling c : Calling.values()) {
                if (c.mortal()) check(h, folk.stream().anyMatch(f -> f.calling() == c), "a " + c.id() + " lives there");
            }
            for (TangFolkEntity f : folk) {
                BlockPos bed = f.bed();
                check(h, bed != null, f.calling().id() + " has a bed");
                var state = level.getBlockState(bed);
                check(h, state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD, f.calling().id() + "'s bed is there: " + state);
            }
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void noVillageStandsInAZone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(JTTWEngine.id("tang_village_plains"));
        check(h, structure != null, "the village is a structure");
        long seed = level.getSeed();
        // a place clear of every zone: a village may stand there
        int[] clear = null;
        for (int i = 0; i < 200 && clear == null; i++) {
            int x = 160 * (i % 20) - 1600, z = 160 * (i / 20) - 800;
            if (VillageSites.clearOfZones(seed, x, z)) clear = new int[]{x, z};
        }
        check(h, clear != null, "somewhere clear of the zones");
        check(h, at(level, structure, seed, clear[0], clear[1]).isPresent(), "on clear level ground, a village");
        for (Zones.Zone zone : Zones.MAP.zones()) {
            for (int[] c : zone.centres(seed)) {
                check(h, at(level, structure, seed, c[0], c[1]).isEmpty(), "no village at the heart of " + zone.id());
                check(h, at(level, structure, seed, c[0] + zone.layout().radius(), c[1]).isEmpty(), "nor at the edge of " + zone.id());
            }
        }
        h.succeed();
    }

    /** Where the structure would stand if the chunk holding (x, z) were offered to it. */
    private static Optional<Structure.GenerationStub> at(ServerLevel level, Structure structure, long seed, int x, int z) {
        var gen = level.getChunkSource().getGenerator();
        var ctx = new Structure.GenerationContext(level.registryAccess(), gen, gen.getBiomeSource(), level.getChunkSource().randomState(),
                level.getStructureManager(), seed, new ChunkPos(x >> 4, z >> 4), level, structure.biomes()::contains);
        return structure.findValidGenerationPoint(ctx);
    }

    @GameTest(template = PLATFORM)
    public static void aMapLeadsToItsPlace(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int[] fengdu = com.jttw.engine.village.VillageOffers.whereIs("fengdu", level.getSeed(), Vec3.ZERO);
        int[] wanted = Zones.FENGDU.centres(level.getSeed())[0];
        check(h, fengdu != null && fengdu[0] == wanted[0] && fengdu[1] == wanted[1], "Fengdu's map is of Fengdu");
        int[] kunlun = com.jttw.engine.village.VillageOffers.whereIs("kunlun", level.getSeed(), Vec3.ZERO);
        int[] peak = Zones.KUNLUN.centres(level.getSeed())[0];
        check(h, kunlun != null && kunlun[0] == peak[0] && kunlun[1] == peak[1], "Kunlun's is of Kunlun");
        h.succeed();
    }
}
