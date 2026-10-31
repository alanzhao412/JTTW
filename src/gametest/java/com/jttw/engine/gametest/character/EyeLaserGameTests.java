package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.wukong.EyeLaser;
import com.jttw.engine.character.wukong.FightingBuddha;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.gametest.GT;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.network.s2c.EyeLaserBeamPacket;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/**
 * The Fiery Golden Eyes' laser at its five outputs: what each costs, how far and how wide it
 * cuts, how hard it hits; refused without the Qi for the chosen output; the output kept (through
 * death and saving), stepped by the wheel and told on the action bar; what stops it (obsidian
 * below the fifth output, bedrock always, nothing for the Fighting Buddha); and the players near
 * it told, so they see it.
 *
 * <p>The player stands at the platform's (2, 2, 2) looking straight up, so the beam's axis is the
 * column through the middle of that block, from his eyes 1.62 over his feet.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class EyeLaserGameTests {

    private static final double EYE = 2 + 1.62; // his eyes' relative height

    private static ServerPlayer wukong(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        qi(p, 200f, 200f);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        p.setXRot(-90f);
        return p;
    }

    /** Ready to fire again at once, with this much Qi. */
    private static void ready(ServerPlayer p, float qi) {
        p.getPersistentData().remove(EyeLaser.KEY_READY_AT);
        QiData.setCurrent(p, qi);
    }

    /** Sets blocks above the platform, and takes them (and whatever they dropped) away when the test ends. */
    private static void place(GameTestHelper h, List<BlockPos> placed, BlockPos rel, Block block) {
        h.setBlock(rel, block);
        if (placed.isEmpty()) GT.onEnd(h, () -> {
            for (BlockPos b : placed) h.getLevel().setBlock(h.absolutePos(b), Blocks.AIR.defaultBlockState(), 3);
            AABB column = new AABB(h.absolutePos(new BlockPos(-6, 0, -6))).expandTowards(14, 90, 14);
            h.getLevel().getEntitiesOfClass(ItemEntity.class, column).forEach(ItemEntity::discard);
        });
        placed.add(rel);
    }

    private static boolean gone(GameTestHelper h, BlockPos rel) {
        return h.getLevel().getBlockState(h.absolutePos(rel)).isAir();
    }

    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserEachOutputCostsItsQi")
    public static void eyeLaserEachOutputCostsItsQi(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        float[] costs = {10, 20, 35, 55, 80};
        for (int lv = 1; lv <= 5; lv++) {
            EyeLaser.setOutput(p, lv);
            ready(p, 200f);
            check(h, EyeLaser.fire(p) != null, "output " + lv + " fires");
            near(h, 200f - costs[lv - 1], QiData.getCurrent(p), "Qi after a shot at output " + lv);
            eq(h, (long) EyeLaser.output(lv).cooldownTicks(),
                    p.getPersistentData().getLong(EyeLaser.KEY_READY_AT) - h.getLevel().getGameTime(), "output " + lv + "'s cooldown");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserEachOutputCooldownIsLonger")
    public static void eyeLaserEachOutputCooldownIsLonger(GameTestHelper h) {
        int[] seconds = {3, 4, 5, 7, 10};
        for (int lv = 1; lv <= 5; lv++) eq(h, seconds[lv - 1] * 20, EyeLaser.output(lv).cooldownTicks(), "output " + lv + "'s cooldown");
        h.succeed();
    }

    /** Each output reaches its range (a block just inside it goes, one past its end stays). */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, timeoutTicks = 200, batch = "eyeLaserEachOutputCutsToItsRange")
    public static void eyeLaserEachOutputCutsToItsRange(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        List<BlockPos> placed = new ArrayList<>();
        double[] ranges = {20, 30, 40, 52, 64};
        for (int lv = 1; lv <= 5; lv++) {
            EyeLaser.Output o = EyeLaser.output(lv);
            near(h, ranges[lv - 1], o.range(), "output " + lv + "'s range");
            BlockPos inside = new BlockPos(2, (int) Math.floor(EYE + o.range() - 0.7), 2);
            BlockPos past = new BlockPos(2, (int) Math.floor(EYE + o.range() + o.radius() + 1.2), 2);
            place(h, placed, inside, Blocks.STONE);
            place(h, placed, past, Blocks.STONE);
            EyeLaser.setOutput(p, lv);
            ready(p, 200f);
            EyeLaser.Shot shot = EyeLaser.fire(p);
            check(h, shot != null && !shot.stopped(), "output " + lv + " fires, nothing stopping it");
            near(h, o.range(), shot.length(), "output " + lv + " runs its range");
            check(h, gone(h, inside), "output " + lv + " cuts a block " + (int) o.range() + " blocks off");
            check(h, !gone(h, past), "output " + lv + " leaves a block past its end");
        }
        h.succeed();
    }

    /**
     * Each output's width: a slab across the beam, ten blocks up, loses a 1-block hole at the
     * first output, a cross of 5 at the second, a 3x3 square at the third, 21 (a 5-wide disc) at
     * the fourth and 37 (7 wide) at the fifth.
     */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, timeoutTicks = 200, batch = "eyeLaserEachOutputCutsItsWidth")
    public static void eyeLaserEachOutputCutsItsWidth(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        List<BlockPos> placed = new ArrayList<>();
        int[] holes = {1, 5, 9, 21, 37};
        int[] across = {1, 3, 3, 5, 7};
        int y = (int) Math.floor(EYE + 10);
        for (int lv = 1; lv <= 5; lv++) {
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) place(h, placed, new BlockPos(2 + dx, y, 2 + dz), Blocks.STONE);
            EyeLaser.setOutput(p, lv);
            ready(p, 200f);
            check(h, EyeLaser.fire(p) != null, "output " + lv + " fires");
            int hole = 0, row = 0;
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                if (gone(h, new BlockPos(2 + dx, y, 2 + dz))) {
                    hole++;
                    if (dz == 0) row++;
                }
            }
            eq(h, holes[lv - 1], hole, "blocks output " + lv + " cut from the slab");
            eq(h, across[lv - 1], row, "output " + lv + "'s cut, across");
        }
        h.succeed();
    }

    /** Each output's hit, by his power: 8, 13, 20, 32, 50 at level 1; and it sets what it hits burning longer. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserEachOutputHitsHarder")
    public static void eyeLaserEachOutputHitsHarder(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        PlayerLevel.setLevel(p, 20);
        float[] damage = {8, 13, 20, 32, 50};
        int[] burn = {60, 80, 100, 140, 200};
        for (int lv = 1; lv <= 5; lv++) {
            Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 5, 2));
            pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            pig.setHealth(1000);
            List<Hit> hits = watch(pig);
            EyeLaser.setOutput(p, lv);
            ready(p, 200f);
            EyeLaser.Shot shot = EyeLaser.fire(p);
            List<Hit> magic = of(hits, DamageTypes.MAGIC);
            eq(h, 1, magic.size(), "output " + lv + " strikes the pig once");
            near(h, damage[lv - 1] * LevelPower.of(p), magic.get(0).amount(), "output " + lv + "'s hit at level 20, by his power");
            eq(h, burn[lv - 1], pig.getRemainingFireTicks(), "output " + lv + " sets it burning");
            check(h, shot.struck().contains(pig), "the shot counts the pig");
            pig.discard();
        }
        h.succeed();
    }

    /** Without the Qi for his chosen output it does not fire, and nothing is spent; with it, it does. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserRefusedWithoutTheQiForItsOutput")
    public static void eyeLaserRefusedWithoutTheQiForItsOutput(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 5, 2));
        List<Hit> hits = watch(pig);
        EyeLaser.setOutput(p, 4); // 55 Qi
        ready(p, 54f);
        check(h, EyeLaser.fire(p) == null, "refused at 54 Qi");
        near(h, 54f, QiData.getCurrent(p), "nothing spent");
        check(h, hits.isEmpty(), "no beam");
        eq(h, 0L, p.getPersistentData().getLong(EyeLaser.KEY_READY_AT), "no cooldown started");
        QiData.setCurrent(p, 55f);
        check(h, EyeLaser.fire(p) != null, "fires at 55 Qi");
        near(h, 0f, QiData.getCurrent(p), "all of it spent");
        h.succeed();
    }

    /** The output is his to keep: in his saved data, through death; it starts at the first. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserOutputIsKept")
    public static void eyeLaserOutputIsKept(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        eq(h, 1, EyeLaser.level(p), "the first output to begin with");
        EyeLaser.setOutput(p, 3);
        CompoundTag saved = p.saveWithoutId(new CompoundTag());
        ServerPlayer loaded = GT.player(h);
        loaded.load(saved);
        eq(h, 3, EyeLaser.level(loaded), "saved and loaded");
        ServerPlayer reborn = GT.player(h);
        reborn.restoreFrom(p, false);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(reborn, p, true));
        eq(h, 3, EyeLaser.level(reborn), "after death");
        h.succeed();
    }

    /** The wheel steps it one at a time, within the five, saying each on the action bar. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserWheelStepsTheOutput")
    public static void eyeLaserWheelStepsTheOutput(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        List<String> heard = new ArrayList<>();
        listen(p, heard);
        EyeLaser.changeOutput(p, 1);
        eq(h, 2, EyeLaser.level(p), "up one");
        check(h, heard.stream().anyMatch(s -> s.contains("Output II") && s.contains("20 Qi")), "told on the action bar: " + heard);
        for (int i = 0; i < 6; i++) EyeLaser.changeOutput(p, 1);
        eq(h, 5, EyeLaser.level(p), "no further than the fifth");
        EyeLaser.changeOutput(p, -1);
        eq(h, 4, EyeLaser.level(p), "down one");
        for (int i = 0; i < 6; i++) EyeLaser.changeOutput(p, -1);
        eq(h, 1, EyeLaser.level(p), "no lower than the first");
        ServerPlayer stranger = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        EyeLaser.changeOutput(stranger, 1);
        eq(h, 1, EyeLaser.level(stranger), "nothing without the eyes");
        h.succeed();
    }

    /** Through the skill bar's N (not sneaking), it fires at his chosen output. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserSlotFiresAtTheChosenOutput")
    public static void eyeLaserSlotFiresAtTheChosenOutput(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        EyeLaser.setOutput(p, 3);
        ready(p, 100f);
        WukongCharacter.INSTANCE.use(p, EyeLaser.ABILITY_ID, AbilityAction.USE);
        near(h, 65f, QiData.getCurrent(p), "35 Qi, output III's");
        h.succeed();
    }

    /** Obsidian stops it below the fifth output (what is past it stands); the fifth burns through it; bedrock stops even that. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserStopsAtWhatItCannotBreak")
    public static void eyeLaserStopsAtWhatItCannotBreak(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        List<BlockPos> placed = new ArrayList<>();
        BlockPos obsidian = new BlockPos(2, 10, 2), behind = new BlockPos(2, 12, 2), bedrock = new BlockPos(2, 15, 2), beyond = new BlockPos(2, 17, 2);
        place(h, placed, obsidian, Blocks.OBSIDIAN);
        place(h, placed, behind, Blocks.STONE);
        place(h, placed, bedrock, Blocks.BEDROCK);
        place(h, placed, beyond, Blocks.STONE);
        EyeLaser.setOutput(p, 4);
        ready(p, 200f);
        EyeLaser.Shot shot = EyeLaser.fire(p);
        check(h, shot.stopped(), "output IV is stopped");
        near(h, h.absolutePos(obsidian).getY(), shot.to().y, "at the obsidian's face");
        check(h, !gone(h, obsidian) && !gone(h, behind), "the obsidian and what is behind it stand");
        EyeLaser.setOutput(p, 5);
        ready(p, 200f);
        shot = EyeLaser.fire(p);
        check(h, gone(h, obsidian) && gone(h, behind), "output V burns through obsidian");
        check(h, shot.stopped(), "but is stopped");
        near(h, h.absolutePos(bedrock).getY(), shot.to().y, "at the bedrock's face");
        check(h, !gone(h, bedrock) && !gone(h, beyond), "the bedrock and what is past it stand");
        h.succeed();
    }

    /** The Fighting Buddha's laser breaks anything at every output, even the first. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserOfTheFightingBuddhaBreaksAnything")
    public static void eyeLaserOfTheFightingBuddhaBreaksAnything(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        PlayerLevel.setLevel(p, 100);
        p.getPersistentData().putBoolean(FightingBuddha.KEY_ANNOUNCED, true);
        check(h, FightingBuddha.is(p), "the Fighting Buddha");
        List<BlockPos> placed = new ArrayList<>();
        for (int lv = 1; lv <= 5; lv++) {
            BlockPos bedrock = new BlockPos(2, 6 + 2 * lv, 2), obsidian = new BlockPos(2, 7 + 2 * lv, 2);
            place(h, placed, bedrock, Blocks.BEDROCK);
            place(h, placed, obsidian, Blocks.OBSIDIAN);
            place(h, placed, new BlockPos(2, 8 + 2 * lv, 2), Blocks.BARRIER);
        }
        for (int lv = 1; lv <= 5; lv++) {
            EyeLaser.setOutput(p, lv);
            ready(p, 200f);
            EyeLaser.Shot shot = EyeLaser.fire(p);
            check(h, !shot.stopped(), "nothing stops output " + lv);
            for (BlockPos b : placed) check(h, gone(h, b), "output " + lv + " broke " + b);
            for (BlockPos b : placed) h.setBlock(b, Blocks.BEDROCK); // and again, for the next output
        }
        h.succeed();
    }

    /** The players near it are told of the beam (so their games draw it); those far off are not. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserIsShownToThePlayersNearIt")
    public static void eyeLaserIsShownToThePlayersNearIt(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        online(h, p);
        ServerPlayer nearby = as(h, ReincarnationOutcome.EYE_OF_HEAVEN), far = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        online(h, nearby);
        online(h, far);
        nearby.moveTo(p.getX() + 20, p.getY(), p.getZ());
        far.moveTo(p.getX() + EyeLaser.TELL_RADIUS + 60, p.getY(), p.getZ());
        List<CustomPacketPayload> mine = new ArrayList<>(), nearGot = new ArrayList<>(), farGot = new ArrayList<>();
        listenPayloads(p, mine);
        listenPayloads(nearby, nearGot);
        listenPayloads(far, farGot);
        EyeLaser.setOutput(p, 2);
        ready(p, 100f);
        EyeLaser.Shot shot = EyeLaser.fire(p);
        List<EyeLaserBeamPacket> beams = nearGot.stream().filter(EyeLaserBeamPacket.class::isInstance).map(EyeLaserBeamPacket.class::cast).toList();
        eq(h, 1, beams.size(), "the player 20 blocks off is told once");
        EyeLaserBeamPacket b = beams.get(0);
        eq(h, p.getId(), b.owner, "from his eyes");
        eq(h, 2, b.output, "at his output");
        near(h, 0, b.from.distanceTo(shot.from()), "from where it began");
        near(h, 0, b.to.distanceTo(shot.to()), "to where it ended");
        check(h, mine.stream().anyMatch(EyeLaserBeamPacket.class::isInstance), "he is told too");
        check(h, farGot.stream().noneMatch(EyeLaserBeamPacket.class::isInstance), "the far player is not told");
        h.succeed();
    }

    // ── its fire, and furnaces ────────────────────────────────────────────

    /** What it strikes burns on, at the output's beat by his power, with Slowness by the output; water, Fire Resistance and milk do nothing to it. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, timeoutTicks = 200, batch = "eyeLaserSetsWhatItHitsBurningForGood")
    public static void eyeLaserSetsWhatItHitsBurningForGood(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        PlayerLevel.setLevel(p, 20);
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 5, 2));
        pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        pig.setHealth(1000);
        EyeLaser.setOutput(p, 3);
        ready(p, 200f);
        EyeLaser.fire(p);
        var burn = pig.getEffect(com.jttw.engine.registry.ModEffects.FIERY_GAZE);
        check(h, burn != null, "it burns");
        check(h, burn.isInfiniteDuration(), "for good");
        eq(h, 2, burn.getAmplifier(), "at output III");
        near(h, com.jttw.engine.character.wukong.FieryGaze.BEAT[2] * LevelPower.of(p), com.jttw.engine.character.wukong.FieryGaze.beatOf(pig), "a beat of 1.2 at level 1, by his power");
        var slow = pig.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        check(h, slow != null && slow.getAmplifier() == 1, "Slowness II at output III");
        // nothing puts it out
        pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1000));
        pig.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        List<BlockPos> placed = new ArrayList<>();
        place(h, placed, new BlockPos(2, 5, 2), Blocks.WATER);
        place(h, placed, new BlockPos(2, 6, 2), Blocks.WATER);
        float after = pig.getHealth();
        float beat = com.jttw.engine.character.wukong.FieryGaze.beatOf(pig);
        h.runAfterDelay(50, () -> {
            check(h, pig.isInWaterOrBubble(), "in water");
            check(h, com.jttw.engine.character.wukong.FieryGaze.burning(pig), "still burning, in water, under Fire Resistance, after milk");
            check(h, shownBurning(pig), "and seen burning (the flag the clients draw fire by)");
            check(h, pig.getHealth() <= after - 2 * beat + 0.01f, "two beats or more taken: " + (after - pig.getHealth()) + " of " + beat + " each");
            check(h, pig.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "still slowed");
            h.succeed();
        });
    }

    /** It burns until the burning one dies. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, timeoutTicks = 400, batch = "eyeLaserFireBurnsUntilDeath")
    public static void eyeLaserFireBurnsUntilDeath(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 5, 2));
        pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 10000));
        EyeLaser.setOutput(p, 1);
        ready(p, 200f);
        EyeLaser.fire(p); // 8 of its 10 health
        check(h, pig.isAlive(), "the shot leaves it alive");
        h.succeedWhen(() -> h.assertTrue(!pig.isAlive(), "burnt to death"));
    }

    /** Each output's beat and Slowness. */
    @GameTest(template = PLATFORM, setupTicks = 100, batch = "eyeLaserFireByOutput")
    public static void eyeLaserFireByOutput(GameTestHelper h) {
        float[] beat = {0.5f, 0.8f, 1.2f, 1.8f, 2.5f};
        int[] slow = {0, 0, 1, 1, 2};
        for (int lv = 1; lv <= 5; lv++) {
            near(h, beat[lv - 1], com.jttw.engine.character.wukong.FieryGaze.BEAT[lv - 1], "output " + lv + " beat");
            eq(h, slow[lv - 1], com.jttw.engine.character.wukong.FieryGaze.slowness(lv), "output " + lv + " Slowness");
        }
        h.succeed();
    }

    /** Whether clients are told it is on fire (Entity's shared flag 0, which they draw the flames by). */
    private static boolean shownBurning(net.minecraft.world.entity.Entity e) {
        try {
            var m = net.neoforged.fml.util.ObfuscationReflectionHelper.findMethod(net.minecraft.world.entity.Entity.class, "getSharedFlag", int.class);
            return (boolean) m.invoke(e, 0);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int burnTime(GameTestHelper h, BlockPos rel) {
        var be = h.getLevel().getBlockEntity(h.absolutePos(rel));
        return be == null ? -1 : be.saveWithoutMetadata(h.getLevel().registryAccess()).getShort("BurnTime");
    }

    /** At outputs I and II a furnace, blast furnace or smoker is lit, a lava bucket's worth, not cut; and the beam stops there. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserLightsFurnacesAtLowOutput")
    public static void eyeLaserLightsFurnacesAtLowOutput(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        List<BlockPos> placed = new ArrayList<>();
        BlockPos at = new BlockPos(2, 8, 2), above = new BlockPos(2, 10, 2);
        Object[][] kinds = {{Blocks.FURNACE, 20000}, {Blocks.BLAST_FURNACE, 10000}, {Blocks.SMOKER, 10000}};
        for (int lv = 1; lv <= 2; lv++) for (Object[] k : kinds) {
            place(h, placed, at, (Block) k[0]);
            place(h, placed, above, Blocks.STONE);
            EyeLaser.setOutput(p, lv);
            ready(p, 200f);
            EyeLaser.Shot shot = EyeLaser.fire(p);
            String what = k[0] + " at output " + lv;
            check(h, h.getBlockState(at).is((Block) k[0]), what + " stands");
            check(h, h.getBlockState(at).getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT), what + " is lit");
            eq(h, (int) k[1], burnTime(h, at), what + ": a lava bucket of burn");
            check(h, shot.stopped() && shot.lit().contains(h.absolutePos(at)), what + " stops the beam");
            check(h, !gone(h, above), "past " + what + ", the stone stands");
        }
        h.succeed();
    }

    /** From output III a furnace is cut as any block. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserCutsFurnacesAtHighOutput")
    public static void eyeLaserCutsFurnacesAtHighOutput(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        List<BlockPos> placed = new ArrayList<>();
        BlockPos at = new BlockPos(2, 8, 2);
        for (int lv = 3; lv <= 5; lv++) {
            place(h, placed, at, Blocks.FURNACE);
            EyeLaser.setOutput(p, lv);
            ready(p, 200f);
            EyeLaser.fire(p);
            check(h, gone(h, at), "output " + lv + " cuts the furnace");
        }
        h.succeed();
    }

    /** At outputs I and II the Eight Trigrams Furnace is lit, as true fire lights it; from III it is cut. */
    @GameTest(template = PLATFORM, setupTicks = 100, skyAccess = true, batch = "eyeLaserLightsTheEightTrigramsFurnace")
    public static void eyeLaserLightsTheEightTrigramsFurnace(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        BlockPos core = new BlockPos(2, 12, 2);
        var level = h.getLevel();
        GT.onEnd(h, () -> {
            for (int x = -1; x <= 1; x++) for (int y = 0; y <= 4; y++) for (int z = -1; z <= 1; z++)
                level.setBlock(h.absolutePos(core.offset(x, y, z)), Blocks.AIR.defaultBlockState(), 3);
            level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(core)).inflate(10)).forEach(ItemEntity::discard);
        });
        for (int lv = 1; lv <= 3; lv++) {
            for (int x = -1; x <= 1; x++) for (int y = 0; y <= 4; y++) for (int z = -1; z <= 1; z++)
                level.setBlock(h.absolutePos(core.offset(x, y, z)), Blocks.AIR.defaultBlockState(), 3);
            var furnace = com.jttw.engine.world.furnace.TrigramsFurnaceBlock.build(level, h.absolutePos(core));
            check(h, !furnace.isLit(), "cold to begin with");
            EyeLaser.setOutput(p, lv);
            ready(p, 200f);
            EyeLaser.fire(p);
            if (lv <= 2) {
                check(h, furnace.isLit(), "output " + lv + " lights it");
                check(h, !gone(h, core), "and leaves it standing");
            } else {
                check(h, !(level.getBlockEntity(h.absolutePos(core)) instanceof com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity), "output III cuts it");
            }
        }
        h.succeed();
    }
}
