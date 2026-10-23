package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.lion.AzureLionEntity;
import com.jttw.engine.entity.lion.DemonKingEntity;
import com.jttw.engine.entity.lion.ElephantDemonEntity;
import com.jttw.engine.entity.lion.LionCamelMonster;
import com.jttw.engine.entity.lion.LionDemonEntity;
import com.jttw.engine.entity.lion.LittleWindDrillerEntity;
import com.jttw.engine.entity.lion.PengDemonEntity;
import com.jttw.engine.entity.lion.WhiteElephantEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionCamelCity;
import com.jttw.engine.world.lion.LionCamelRidge;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Supplier;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/**
 * Lion-Camel Ridge: the Peng rule (nothing of the zone, its two demon kings included, targets a
 * Peng player or fights him back, even struck; they fight others), the Azure Lion swallowing a foe
 * and the foe fighting his way out or being spat out, the White Elephant's trunk, his and the
 * Lion's phases, the Little Wind-Driller's alarm, the ridge's creatures and captains, what they
 * leave (and that the Peng gets nothing of theirs), the band's XP at the levels' cap, karma, and
 * Lion-Camel City's blueprint.
 *
 * <p>Each test runs in a batch of its own: the ridge's creatures call others up (an alarm, a king's
 * kin) and rouse whatever of the ridge is near, and its kings are monsters a Wukong clone goes after,
 * so nothing of a test here may share the world with another test.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class LionCamelGameTests {

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-3 + 1e-3, what + ": expected " + expected + " but was " + actual);
    }

    /**
     * Removes, when the test ends, what this test brought and anything of the ridge's within its own
     * bounds (never wider: the tests round it are running too). What a test calls up further off (an
     * alarm's answer, a king's kin) it removes itself ({@link #discardAtEnd}).
     */
    private static void sweepAround(GameTestHelper h, Entity... held) {
        AABB bounds = h.getBounds().inflate(1);
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, bounds, x -> x instanceof LionCamelMonster || x instanceof DemonKingEntity
                    || x instanceof ItemEntity || x instanceof ExperienceOrb).forEach(Entity::discard);
            for (Entity e : held) {
                e.stopRiding();
                e.discard();
            }
        });
    }

    private static void discardAtEnd(GameTestHelper h, List<? extends Entity> called) {
        onEnd(h, () -> called.forEach(Entity::discard));
    }

    private static ServerPlayer survivor(GameTestHelper h, ReincarnationOutcome outcome) {
        ServerPlayer p = as(h, outcome);
        p.setGameMode(GameType.SURVIVAL); // a creative player is no one's enemy
        return p;
    }

    private static ServerPlayer peng(GameTestHelper h) {
        return survivor(h, ReincarnationOutcome.GOLD_WINGED_PENG);
    }

    private static ServerPlayer other(GameTestHelper h) {
        return survivor(h, ReincarnationOutcome.EYE_OF_HEAVEN);
    }

    private static <T extends ZoneMonster> T demon(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        sweepAround(h, m);
        return m;
    }

    private static <T extends DemonKingEntity> T king(GameTestHelper h, EntityType<T> type, BlockPos at) {
        openAir(h); // towering: under the framework's roof they would choke
        T k = h.spawn(type, at);
        sweepAround(h, k);
        return k;
    }

    /** A still iron golem, its health raised so no level-100 blow ends it: a target to measure blows on. */
    private static LivingEntity golem(GameTestHelper h, BlockPos at) {
        var g = h.spawn(EntityType.IRON_GOLEM, at);
        g.goalSelector.removeAllGoals(x -> true);
        g.targetSelector.removeAllGoals(x -> true);
        g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
        g.setHealth(g.getMaxHealth());
        sweepAround(h, g);
        return g;
    }

    /** Every kind of the ridge's creatures, the two kings among them. */
    private static List<Supplier<? extends EntityType<? extends Mob>>> everyKind() {
        return List.of(ModEntities.LION_DEMON, ModEntities.ELEPHANT_DEMON, ModEntities.PENG_DEMON, ModEntities.LITTLE_WIND_DRILLER,
                ModEntities.AZURE_LION, ModEntities.WHITE_ELEPHANT);
    }

    private static Mob spawnKind(GameTestHelper h, EntityType<? extends Mob> type, BlockPos at) {
        Mob m = h.spawn(type, at);
        if (m instanceof ZoneMonster z) z.become(z.regularLevel(), false);
        sweepAround(h, m);
        return m;
    }

    // ── The Peng rule ───────────────────────────────────────────────────

    @GameTest(batch = "lion_camel_nothingOfTheRidgeTargetsThePeng", template = PLATFORM)
    public static void nothingOfTheRidgeTargetsThePeng(GameTestHelper h) {
        openAir(h);
        ServerPlayer peng = peng(h);
        ServerPlayer other = other(h);
        for (var kind : everyKind()) {
            Mob m = spawnKind(h, kind.get(), new BlockPos(2, 2, 2));
            m.setNoAi(true);
            m.tickCount = 40; // a hurt at its first tick looks to HurtByTargetGoal like none at all
            String who = EntityType.getKey(m.getType()).getPath();
            check(h, m.isAlliedTo(peng), who + " is allied to the Peng (his area attacks pass it over)");
            check(h, !m.isAlliedTo(other), who + " not to anyone else");
            TargetingConditions seek = TargetingConditions.forCombat().range(32);
            check(h, !seek.test(m, peng), who + " never seeks out the Peng");
            check(h, seek.test(m, other), who + " seeks out others");
            m.setLastHurtByMob(peng);
            check(h, !new HurtByTargetGoal((net.minecraft.world.entity.PathfinderMob) m).canUse(), who + ", struck by the Peng, does not fight back");
            m.setLastHurtByMob(other);
            check(h, new HurtByTargetGoal((net.minecraft.world.entity.PathfinderMob) m).canUse(), who + ", struck by another, does");
            m.setTarget(peng);
            check(h, m.getTarget() == null, who + " is never made the Peng's foe");
            m.setTarget(other);
            check(h, m.getTarget() == other, who + " is made another's");
            m.discard();
        }
        h.succeed();
    }

    /** Two of a kind on the platform, one struck by the Peng and one by another player: the first lets him be, the second turns. */
    private static void struck(GameTestHelper h, EntityType<? extends Mob> type) {
        openAir(h);
        ServerPlayer peng = peng(h);
        ServerPlayer other = other(h);
        Mob spared = spawnKind(h, type, new BlockPos(0, 2, 2));
        Mob angered = spawnKind(h, type, new BlockPos(4, 2, 2));
        // whatever the struck one called up (a Wind-Driller's alarm) is set on this test's other player alone
        AABB wide = h.getBounds().inflate(16, 32, 16);
        onEnd(h, () -> h.getLevel().getEntitiesOfClass(LionCamelMonster.class, wide, m -> m.getTarget() == other).forEach(Entity::discard));
        boolean[] hurt = new boolean[2];
        h.runAfterDelay(5, () -> {
            hurt[0] = spared.hurt(h.getLevel().damageSources().playerAttack(peng), 1);
            hurt[1] = angered.hurt(h.getLevel().damageSources().playerAttack(other), 1);
        });
        h.runAfterDelay(25, () -> {
            check(h, hurt[0], "the Peng's blow lands");
            // (a pack set on another by its fellow's call is not his doing: only never on him)
            check(h, spared.getTarget() != peng, "struck by the Peng, it lets him be: " + spared.getTarget());
            check(h, angered.getTarget() == other, "struck by another, it turns on them: " + angered.getTarget() + " (hurt: " + hurt[1] + ")");
            if (spared instanceof LittleWindDrillerEntity d) {
                check(h, d.alarmReady(), "the Peng's blow raises no alarm");
                check(h, !((LittleWindDrillerEntity) angered).alarmReady(), "another's does");
            }
            h.succeed();
        });
    }

    @GameTest(batch = "lion_camel_struckByThePengALionDemonLetsHimBe", template = PLATFORM, timeoutTicks = 100)
    public static void struckByThePengALionDemonLetsHimBe(GameTestHelper h) {
        struck(h, ModEntities.LION_DEMON.get());
    }

    @GameTest(batch = "lion_camel_struckByThePengAnElephantDemonLetsHimBe", template = PLATFORM, timeoutTicks = 100)
    public static void struckByThePengAnElephantDemonLetsHimBe(GameTestHelper h) {
        struck(h, ModEntities.ELEPHANT_DEMON.get());
    }

    @GameTest(batch = "lion_camel_struckByThePengAPengDemonLetsHimBe", template = PLATFORM, timeoutTicks = 100)
    public static void struckByThePengAPengDemonLetsHimBe(GameTestHelper h) {
        struck(h, ModEntities.PENG_DEMON.get());
    }

    @GameTest(batch = "lion_camel_struckByThePengTheLittleWindDrillerRaisesNoAlarm", template = PLATFORM, timeoutTicks = 100)
    public static void struckByThePengTheLittleWindDrillerRaisesNoAlarm(GameTestHelper h) {
        struck(h, ModEntities.LITTLE_WIND_DRILLER.get());
    }

    @GameTest(batch = "lion_camel_struckByThePengTheAzureLionLetsHimBe", template = PLATFORM, timeoutTicks = 100)
    public static void struckByThePengTheAzureLionLetsHimBe(GameTestHelper h) {
        struck(h, ModEntities.AZURE_LION.get());
    }

    @GameTest(batch = "lion_camel_struckByThePengTheWhiteElephantLetsHimBe", template = PLATFORM, timeoutTicks = 100)
    public static void struckByThePengTheWhiteElephantLetsHimBe(GameTestHelper h) {
        struck(h, ModEntities.WHITE_ELEPHANT.get());
    }

    @GameTest(batch = "lion_camel_theKingsNeverSwallowNorGrabNorRoarAtThePeng", template = PLATFORM)
    public static void theKingsNeverSwallowNorGrabNorRoarAtThePeng(GameTestHelper h) {
        ServerPlayer peng = peng(h);
        AzureLionEntity lion = king(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 0));
        lion.setNoAi(true);
        lion.setYRot(0);
        lion.setYHeadRot(0);
        lion.setYBodyRot(0);
        peng.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 3))));
        LivingEntity g = golem(h, new BlockPos(3, 2, 3));
        check(h, !lion.foe(peng) && lion.foe(g), "the Peng is no foe of his; the golem is");
        check(h, !lion.swallow(peng), "he will not swallow his brother");
        List<LivingEntity> roared = lion.roar(g);
        check(h, roared.contains(g) && !roared.contains(peng), "his roar passes over the Peng: " + roared);
        lion.discard();
        WhiteElephantEntity elephant = king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(2, 2, 0));
        elephant.setNoAi(true);
        check(h, !elephant.grab(peng), "nor will the Elephant's trunk take him");
        List<LivingEntity> stamped = elephant.stamp();
        check(h, !stamped.contains(peng), "nor his stamping touch him");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_whatThePengStrikesDownLeavesHimNothing", template = PLATFORM)
    public static void whatThePengStrikesDownLeavesHimNothing(GameTestHelper h) {
        ServerPlayer peng = peng(h);
        LevelManager.initClass(peng, ReincarnationOutcome.GOLD_WINGED_PENG);
        PlayerLevel.setLevel(peng, 89);
        PlayerLevel.setXp(peng, 0);
        LionDemonEntity m = demon(h, ModEntities.LION_DEMON.get(), 89, true, new BlockPos(2, 2, 2));
        eq(h, 0, Morality.killDeed(peng, m), "no deed: it never fought him");
        LevelManager.onEntityKilled(new LivingDeathEvent(m, h.getLevel().damageSources().playerAttack(peng)));
        eq(h, 0L, PlayerLevel.getXp(peng), "no XP");
        m.invulnerableTime = 0;
        m.hurt(h.getLevel().damageSources().playerAttack(peng), Float.MAX_VALUE);
        check(h, !m.isAlive(), "struck down");
        AABB around = new AABB(m.blockPosition()).inflate(4);
        check(h, h.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "it left him nothing");
        check(h, h.getLevel().getEntitiesOfClass(ExperienceOrb.class, around).isEmpty(), "not even experience");
        eq(h, 0, Morality.killDeed(peng, king(h, ModEntities.AZURE_LION.get(), new BlockPos(0, 2, 0))), "his brother's death, no deed either");
        // another's kill leaves its loot
        ServerPlayer other = other(h);
        LionDemonEntity m2 = demon(h, ModEntities.LION_DEMON.get(), 89, false, new BlockPos(4, 2, 4));
        m2.hurt(h.getLevel().damageSources().playerAttack(other), Float.MAX_VALUE);
        check(h, h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(m2.blockPosition()).inflate(4),
                i -> i.getItem().is(ModItems.MANE_TUFT.get())).size() > 0, "another's kill leaves its loot");
        h.succeed();
    }

    // ── The Azure Lion ──────────────────────────────────────────────────

    @GameTest(batch = "lion_camel_theAzureLionIsAKingAtTheCap", template = PLATFORM)
    public static void theAzureLionIsAKingAtTheCap(GameTestHelper h) {
        AzureLionEntity lion = king(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 2));
        check(h, lion.getDisplayName().getString().contains("Lv 100"), lion.getDisplayName().getString());
        close(h, AzureLionEntity.HEALTH, lion.getMaxHealth(), "ninety ordinary hits of a level-100 character");
        eq(h, EnemyTier.BOSS.name(), lion.getPersistentData().getString(ZoneMonster.KEY_TIER), "a Boss");
        eq(h, Zones.LION_CAMEL.id(), lion.getPersistentData().getString(ZoneMonster.KEY_ZONE), "of the ridge");
        eq(h, BossEvent.BossBarColor.BLUE, lion.bossBar().getColor(), "an azure bar");
        lion.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(lion.blockPosition()), net.minecraft.world.entity.MobSpawnType.STRUCTURE, null);
        check(h, lion.hasRestriction(), "he keeps to his court");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_swallowedWholeAFoeFightsHisWayOut", template = PLATFORM, timeoutTicks = 120)
    public static void swallowedWholeAFoeFightsHisWayOut(GameTestHelper h) {
        AzureLionEntity lion = king(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 1));
        lion.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(2, 2, 3));
        float hp = g.getHealth();
        check(h, lion.swallow(g), "he swallows it");
        check(h, lion.holds(g) && g.getVehicle() == lion && lion.isHolding(), "it is inside him");
        check(h, g.hasEffect(MobEffects.INVISIBILITY) && g.hasEffect(MobEffects.DARKNESS), "unseen, and in the dark");
        g.stopRiding();
        check(h, g.getVehicle() == lion, "it cannot get out by itself");
        h.runAfterDelay(25, () -> {
            check(h, lion.holds(g), "still inside");
            check(h, hp - g.getHealth() >= lion.bellyDamage() * 0.99f, "his belly works on it: " + (hp - g.getHealth()));
            // it strikes from inside: half again, and enough of it gets it out
            lion.invulnerableTime = 0;
            float need = lion.escapeShare() * lion.getMaxHealth() / AzureLionEntity.WITHIN;
            float before = lion.getHealth();
            lion.hurt(h.getLevel().damageSources().indirectMagic(g, g), need * 0.5f);
            close(h, need * 0.5f * AzureLionEntity.WITHIN, before - lion.getHealth(), "a blow from inside counts half again");
            check(h, lion.holds(g), "not yet enough");
            lion.invulnerableTime = 0;
            lion.hurt(h.getLevel().damageSources().indirectMagic(g, g), need * 0.6f);
            check(h, !lion.holds(g) && g.getVehicle() == null && !lion.isHolding(), "he retches it up");
            check(h, lion.isReeling(), "and reels");
            check(h, !g.hasEffect(MobEffects.INVISIBILITY), "out in the light again");
            h.succeed();
        });
    }

    @GameTest(batch = "lion_camel_aSwallowedPlayerCanOnlyFightHisWayOut", template = PLATFORM)
    public static void aSwallowedPlayerCanOnlyFightHisWayOut(GameTestHelper h) {
        ServerPlayer p = other(h);
        AzureLionEntity lion = king(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 1));
        lion.setNoAi(true);
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 3))));
        check(h, lion.swallow(p), "he swallows the player whole");
        check(h, p.getVehicle() == lion && lion.holds(p), "inside him");
        p.stopRiding();
        check(h, p.getVehicle() == lion, "a swallowed player cannot simply get down");
        check(h, lion.canRiderInteract(), "he can strike the Lion from where he is");
        lion.invulnerableTime = 0;
        lion.hurt(h.getLevel().damageSources().playerAttack(p), lion.escapeShare() * lion.getMaxHealth());
        check(h, p.getVehicle() == null && !lion.holds(p), "his blows get him out");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_whatTheLionCannotStomachHeSpitsOut", template = PLATFORM, timeoutTicks = 260)
    public static void whatTheLionCannotStomachHeSpitsOut(GameTestHelper h) {
        AzureLionEntity lion = king(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 1));
        lion.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(2, 2, 3));
        check(h, lion.swallow(g), "swallowed");
        h.runAfterDelay(AzureLionEntity.MAX_HOLD + 5, () -> {
            check(h, !lion.holds(g) && g.getVehicle() == null, "spat out after " + AzureLionEntity.MAX_HOLD + " ticks");
            check(h, !lion.isReeling(), "no reeling: it did not fight its way out");
            h.succeed();
        });
    }

    @GameTest(batch = "lion_camel_theLionsRoarStrikesWhatIsBeforeHim", template = PLATFORM)
    public static void theLionsRoarStrikesWhatIsBeforeHim(GameTestHelper h) {
        AzureLionEntity lion = king(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 0));
        lion.setNoAi(true);
        LivingEntity before = golem(h, new BlockPos(2, 2, 4));
        LivingEntity aside = golem(h, new BlockPos(0, 2, 0));
        float hp = before.getHealth(), asideHp = aside.getHealth();
        List<LivingEntity> caught = lion.roar(before);
        check(h, caught.contains(before) && !caught.contains(aside), "the one before him, not the one at his side");
        close(h, lion.roarDamage(), hp - before.getHealth(), "its blow");
        close(h, AzureLionEntity.ROAR * MobScaling.typicalHealth(100), lion.roarDamage(), "12% of a level-100 character's health");
        check(h, before.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && before.hasEffect(MobEffects.WEAKNESS), "slowed and weakened");
        eq(h, asideHp, aside.getHealth(), "untouched at his side");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_atTwoThirdsTheLionCallsHisKinAndAtAThirdHeGrows", template = PLATFORM, timeoutTicks = 60)
    public static void atTwoThirdsTheLionCallsHisKinAndAtAThirdHeGrows(GameTestHelper h) {
        AzureLionEntity lion = king(h, ModEntities.AZURE_LION.get(), new BlockPos(2, 2, 2));
        lion.setNoAi(true);
        LivingEntity foe = golem(h, new BlockPos(4, 2, 4));
        lion.setTarget(foe); // his kin are set on his foe: so this test knows its own
        float scale = lion.getScale();
        lion.setHealth(lion.getMaxHealth() * 0.6f);
        h.runAfterDelay(3, () -> {
            eq(h, 1, lion.phase(), "his second phase");
            List<LionDemonEntity> kin = h.getLevel().getEntitiesOfClass(LionDemonEntity.class, lion.getBoundingBox().inflate(12, 64, 12), m -> m.getTarget() == foe);
            discardAtEnd(h, kin);
            eq(h, AzureLionEntity.KIN, kin.size(), "lion-demons come running, set on his foe");
            lion.setHealth(lion.getMaxHealth() * 0.3f);
        });
        h.runAfterDelay(6, () -> {
            eq(h, 2, lion.phase(), "his last");
            close(h, scale * (1 + AzureLionEntity.GROW_PERCENT / 100.0), lion.getScale(), "he swells huge");
            h.succeed();
        });
    }

    // ── The White Elephant ──────────────────────────────────────────────

    @GameTest(batch = "lion_camel_theWhiteElephantIsAKing", template = PLATFORM)
    public static void theWhiteElephantIsAKing(GameTestHelper h) {
        WhiteElephantEntity e = king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(2, 2, 2));
        check(h, e.getDisplayName().getString().contains("Lv 98"), e.getDisplayName().getString());
        close(h, WhiteElephantEntity.HEALTH, e.getMaxHealth(), "eighty-five ordinary hits of a level-98 character");
        eq(h, EnemyTier.BOSS.name(), e.getPersistentData().getString(ZoneMonster.KEY_TIER), "a Boss");
        eq(h, BossEvent.BossBarColor.WHITE, e.bossBar().getColor(), "a white bar");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_theTrunkLiftsCrushesAndHurls", template = PLATFORM, timeoutTicks = 120)
    public static void theTrunkLiftsCrushesAndHurls(GameTestHelper h) {
        WhiteElephantEntity e = king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(2, 2, 1));
        e.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(2, 2, 3));
        float hp = g.getHealth();
        check(h, e.grab(g), "his trunk takes it");
        eq(h, WhiteElephantEntity.TRUNK_HOLDING, e.trunk(), "holding");
        g.stopRiding();
        check(h, g.getVehicle() == e, "it cannot get down by itself");
        h.runAfterDelay(3, () -> check(h, g.getY() > e.getY() + 2, "lifted high: " + (g.getY() - e.getY())));
        h.runAfterDelay(WhiteElephantEntity.SQUEEZE_EVERY + 2, () ->
                check(h, hp - g.getHealth() >= e.crushDamage() * 0.99f, "crushed: " + (hp - g.getHealth())));
        h.runAfterDelay(WhiteElephantEntity.MAX_HOLD + 2, () -> {
            check(h, !e.holds(g) && g.getVehicle() == null, "let go");
            eq(h, WhiteElephantEntity.TRUNK_REST, e.trunk(), "his trunk at rest");
            check(h, !e.isRecoiling(), "hurled, not broken free from");
            h.succeed();
        });
        h.runAfterDelay(WhiteElephantEntity.MAX_HOLD + 1, () ->
                check(h, g.getDeltaMovement().horizontalDistance() > 0.5 || g.getVehicle() == null, "hurled away: " + g.getDeltaMovement()));
    }

    @GameTest(batch = "lion_camel_blowsOnTheTrunkMakeHimDropHisCatch", template = PLATFORM)
    public static void blowsOnTheTrunkMakeHimDropHisCatch(GameTestHelper h) {
        WhiteElephantEntity e = king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(2, 2, 1));
        e.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(2, 2, 3));
        check(h, e.grab(g), "taken");
        e.invulnerableTime = 0;
        e.hurt(h.getLevel().damageSources().indirectMagic(g, g), e.escapeShare() * e.getMaxHealth() * 1.05f);
        check(h, !e.holds(g) && g.getVehicle() == null, "dropped");
        check(h, e.isRecoiling(), "he recoils");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_hisStampingAndHisFollowers", template = PLATFORM, timeoutTicks = 60)
    public static void hisStampingAndHisFollowers(GameTestHelper h) {
        WhiteElephantEntity e = king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(2, 2, 2));
        e.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(4, 2, 4));
        float hp = g.getHealth();
        List<LivingEntity> caught = e.stamp();
        check(h, caught.contains(g), "his stamping catches what is near");
        close(h, e.stampDamage(), hp - g.getHealth(), "for its blow");
        e.setTarget(g); // his followers are set on his foe: so this test knows its own
        e.setHealth(e.getMaxHealth() * 0.6f);
        h.runAfterDelay(3, () -> {
            eq(h, 1, e.phase(), "his second phase");
            List<ElephantDemonEntity> come = h.getLevel().getEntitiesOfClass(ElephantDemonEntity.class, e.getBoundingBox().inflate(12, 64, 12), m -> m.getTarget() == g);
            discardAtEnd(h, come);
            eq(h, WhiteElephantEntity.FOLLOWERS, come.size(), "his followers come at his trumpeting");
            h.succeed();
        });
    }

    @GameTest(batch = "lion_camel_hisChargeGoresWhatHeMeets", template = PLATFORM)
    public static void hisChargeGoresWhatHeMeets(GameTestHelper h) {
        WhiteElephantEntity e = king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(2, 2, 0));
        e.setNoAi(true);
        LivingEntity g = golem(h, new BlockPos(2, 2, 2));
        float hp = g.getHealth();
        check(h, e.charge(g), "he charges");
        List<LivingEntity> gored = List.of();
        for (int i = 0; i < 5 && gored.isEmpty(); i++) gored = e.chargeTick();
        check(h, gored.contains(g), "and gores it");
        close(h, e.goreDamage(), hp - g.getHealth(), "for his gore");
        h.succeed();
    }

    // ── The Little Wind-Driller ─────────────────────────────────────────

    @GameTest(batch = "lion_camel_theLittleWindDrillerRaisesTheAlarm", template = PLATFORM)
    public static void theLittleWindDrillerRaisesTheAlarm(GameTestHelper h) {
        openAir(h);
        ServerPlayer peng = peng(h);
        ServerPlayer other = other(h);
        LittleWindDrillerEntity d = demon(h, ModEntities.LITTLE_WIND_DRILLER.get(), 89, false, new BlockPos(2, 2, 2));
        d.setNoAi(true);
        LionDemonEntity a = demon(h, ModEntities.LION_DEMON.get(), 89, false, new BlockPos(0, 2, 0));
        ElephantDemonEntity b = demon(h, ModEntities.ELEPHANT_DEMON.get(), 89, false, new BlockPos(4, 2, 4));
        a.setNoAi(true);
        b.setNoAi(true);
        check(h, d.raiseAlarm(peng).isEmpty() && d.alarmReady(), "never against the Great Peng");
        check(h, a.getTarget() == null, "no one set on him");
        List<LionCamelMonster> roused = d.raiseAlarm(other);
        discardAtEnd(h, roused);
        check(h, roused.contains(a) && roused.contains(b), "every demon near is set on the foe");
        check(h, a.getTarget() == other && b.getTarget() == other, "on him");
        eq(h, 2 + LittleWindDrillerEntity.ANSWER, roused.size(), "and the ridge's host answers the bell");
        check(h, !d.alarmReady(), "then he must catch his breath");
        check(h, d.raiseAlarm(other).isEmpty(), "before he can raise it again");
        check(h, d.isRinging(), "ringing his bell");
        eq(h, 0f, d.captainChance(), "a scout is never a captain");
        eq(h, 0, d.escorts(), "nor brings escorts");
        h.succeed();
    }

    // ── The creatures ───────────────────────────────────────────────────

    @GameTest(batch = "lion_camel_theRidgesDemonsAndTheirTricks", template = PLATFORM)
    public static void theRidgesDemonsAndTheirTricks(GameTestHelper h) {
        LivingEntity g = golem(h, new BlockPos(2, 2, 4));
        LionDemonEntity lion = demon(h, ModEntities.LION_DEMON.get(), 89, false, new BlockPos(2, 2, 2));
        lion.setNoAi(true);
        float hp = g.getHealth();
        check(h, lion.landOn(g), "a lion-demon's pounce lands");
        close(h, lion.pounceDamage(), hp - g.getHealth(), "for its blow");
        close(h, LionDemonEntity.POUNCE * MobScaling.typicalHealth(89), lion.pounceDamage(), "26% of a level-89 character's health");
        check(h, g.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "staggering");
        lion.discard();
        PengDemonEntity peng = demon(h, ModEntities.PENG_DEMON.get(), 89, false, new BlockPos(2, 2, 2));
        peng.setNoAi(true);
        g.invulnerableTime = 0;
        hp = g.getHealth();
        check(h, peng.strike(g), "a peng-demon's swoop strikes");
        close(h, peng.swoopDamage(), hp - g.getHealth(), "for its blow");
        check(h, !peng.causeFallDamage(20, 1, h.getLevel().damageSources().fall()), "no fall harms it");
        peng.discard();
        ElephantDemonEntity el = demon(h, ModEntities.ELEPHANT_DEMON.get(), 89, false, new BlockPos(2, 2, 2)); // (still: it does not move)
        el.setNoAi(true);
        g.invulnerableTime = 0;
        hp = g.getHealth();
        check(h, el.charge(g), "an elephant-demon charges");
        List<LivingEntity> gored = List.of();
        for (int i = 0; i < 6 && gored.isEmpty(); i++) gored = el.chargeTick();
        check(h, gored.contains(g), "and gores");
        close(h, el.goreDamage(), hp - g.getHealth(), "for its gore");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_aRidgeCaptainIsPurpleLargerAndBringsItsKind", template = PLATFORM)
    public static void aRidgeCaptainIsPurpleLargerAndBringsItsKind(GameTestHelper h) {
        openAir(h);
        LionDemonEntity regular = demon(h, ModEntities.LION_DEMON.get(), 89, false, new BlockPos(1, 2, 1));
        LionDemonEntity captain = demon(h, ModEntities.LION_DEMON.get(), 96, true, new BlockPos(3, 2, 3));
        eq(h, ZoneMonster.PURPLE, captain.getDisplayName().getStyle().getColor().getValue(), "a captain's name is purple");
        check(h, captain.getDisplayName().getString().contains("Captain") && captain.getDisplayName().getString().contains("Lv 96"), captain.getDisplayName().getString());
        check(h, regular.getDisplayName().getString().contains("Lv 89"), regular.getDisplayName().getString());
        close(h, LionDemonEntity.HITS * MobScaling.typicalHit(96) * ZoneMonster.CAPTAIN_HEALTH, captain.getMaxHealth(), "a captain's health");
        eq(h, LionCamelMonster.LEVEL + ZoneMonster.CAPTAIN_OVER, captain.captainLevel(), "captains about 96");
        LivingEntity foe = golem(h, new BlockPos(0, 2, 4));
        captain.setTarget(foe); // its escorts are set on its foe: so this test knows its own
        captain.bringEscorts(h.getLevel());
        List<LionDemonEntity> escorts = h.getLevel().getEntitiesOfClass(LionDemonEntity.class, captain.getBoundingBox().inflate(8), e -> e != captain && e.getTarget() == foe);
        discardAtEnd(h, escorts);
        eq(h, captain.escorts(), escorts.size(), "escorts");
        for (LionDemonEntity e : escorts) check(h, !e.isCaptain() && Math.abs(e.mobLevel() - 89) <= 1, "regulars about 89");
        eq(h, 2, new ElephantDemonEntity(ModEntities.ELEPHANT_DEMON.get(), h.getLevel()).escorts(), "an elephant captain brings two");
        eq(h, 4, new PengDemonEntity(ModEntities.PENG_DEMON.get(), h.getLevel()).escorts(), "a peng captain four");
        h.succeed();
    }

    // ── XP and karma ────────────────────────────────────────────────────

    private static void killedBy(ServerPlayer p, LivingEntity victim) {
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, p.level().damageSources().playerAttack(p)));
    }

    @GameTest(batch = "lion_camel_theRidgePaysTheLastBandToTheCap", template = PLATFORM)
    public static void theRidgePaysTheLastBandToTheCap(GameTestHelper h) {
        ServerPlayer p = other(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 89);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.LION_CAMEL_RIDGE_BIOME.location());
        check(h, band != null && band.minLevel() == 89 && band.maxLevel() == 100, "the ridge's band is 89-100");
        eq(h, LevelConstants.MAX_LEVEL, band.maxLevel(), "up to the cap");
        eq(h, 1248L, band.xpForTier(EnemyTier.GRUNT), "a grunt pays 160 x 7.8");
        killedBy(p, demon(h, ModEntities.PENG_DEMON.get(), 89, false, new BlockPos(1, 2, 1)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a regular");
        killedBy(p, demon(h, ModEntities.ELEPHANT_DEMON.get(), 96, true, new BlockPos(3, 2, 3)));
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.GENERAL), PlayerLevel.getXp(p), "plus a captain");
        long xp = PlayerLevel.getXp(p);
        killedBy(p, king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(2, 2, 0)));
        eq(h, xp + band.xpForTier(EnemyTier.BOSS), PlayerLevel.getXp(p), "plus the White Elephant, a boss");
        // one level short of the cap it still pays; at the cap nothing more is earned
        PlayerLevel.setLevel(p, 99);
        xp = PlayerLevel.getXp(p);
        killedBy(p, demon(h, ModEntities.LION_DEMON.get(), 89, false, new BlockPos(4, 2, 1)));
        check(h, PlayerLevel.getXp(p) > xp || PlayerLevel.getLevel(p) == 100, "at 99 a kill still pays");
        PlayerLevel.setLevel(p, LevelConstants.MAX_LEVEL);
        xp = PlayerLevel.getXp(p);
        killedBy(p, demon(h, ModEntities.LION_DEMON.get(), 89, false, new BlockPos(0, 2, 4)));
        eq(h, xp, PlayerLevel.getXp(p), "at the cap, 100, there is no more to earn");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_slayingTheKingsWhoAteAKingdomIsAGoodDeed", template = PLATFORM)
    public static void slayingTheKingsWhoAteAKingdomIsAGoodDeed(GameTestHelper h) {
        ServerPlayer p = other(h);
        eq(h, Morality.SLAY_AZURE_LION, Morality.killDeed(p, king(h, ModEntities.AZURE_LION.get(), new BlockPos(1, 2, 1))), "the Azure Lion");
        eq(h, Morality.SLAY_WHITE_ELEPHANT, Morality.killDeed(p, king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(3, 2, 3))), "the White Elephant");
        check(h, Morality.SLAY_AZURE_LION == Morality.SLAY_LADY && Morality.SLAY_WHITE_ELEPHANT == Morality.SLAY_LADY && Morality.SLAY_LADY > 0,
                "as good a deed as destroying Lady White Bone");
        eq(h, Morality.SLAY_CAPTAIN, Morality.killDeed(p, demon(h, ModEntities.LION_DEMON.get(), 96, true, new BlockPos(0, 2, 4))), "a captain");
        eq(h, Morality.SLAY_MONSTER, Morality.killDeed(p, demon(h, ModEntities.LITTLE_WIND_DRILLER.get(), 89, false, new BlockPos(4, 2, 0))), "a little demon");
        h.succeed();
    }

    @GameTest(batch = "lion_camel_thePengIsHailedButNotTooOften", template = PLATFORM)
    public static void thePengIsHailedButNotTooOften(GameTestHelper h) {
        ServerPlayer peng = peng(h);
        LionDemonEntity m = demon(h, ModEntities.LION_DEMON.get(), 89, false, new BlockPos(1, 2, 1));
        check(h, LionCamelRidge.isPeng(peng) && !LionCamelRidge.isPeng(other(h)), "the ridge knows its Third Great King");
        check(h, LionCamelRidge.greet(m, peng), "hailed");
        check(h, !LionCamelRidge.greet(m, peng), "not again at once");
        h.succeed();
    }

    // ── What they leave ─────────────────────────────────────────────────

    private static List<ItemStack> loot(GameTestHelper h, Mob mob, ServerPlayer p) {
        var source = h.getLevel().damageSources().playerAttack(p);
        LootParams params = new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, p)
                .create(LootContextParamSets.ENTITY);
        return h.getLevel().getServer().reloadableRegistries().getLootTable(mob.getLootTable()).getRandomItems(params);
    }

    private static int count(List<ItemStack> items, Item item) {
        return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(batch = "lion_camel_theKingsLeaveTheirWeaponsAndTheirHoard", template = PLATFORM)
    public static void theKingsLeaveTheirWeaponsAndTheirHoard(GameTestHelper h) {
        ServerPlayer p = other(h);
        AzureLionEntity lion = king(h, ModEntities.AZURE_LION.get(), new BlockPos(1, 2, 1));
        WhiteElephantEntity elephant = king(h, ModEntities.WHITE_ELEPHANT.get(), new BlockPos(3, 2, 3));
        int sabres = 0, lances = 0, rolls = 40;
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> l = loot(h, lion, p);
            int mane = count(l, ModItems.AZURE_LION_MANE.get());
            check(h, mane >= 2 && mane <= 4, "two or three locks of his mane: " + l);
            check(h, count(l, net.minecraft.world.item.Items.DIAMOND) >= 3 && count(l, net.minecraft.world.item.Items.GOLD_BLOCK) >= 2, "the hoard: " + l);
            check(h, count(l, ModItems.TRUE_SAMADHI_FIRE.get()) >= 1, "the furnace's fuel");
            sabres += count(l, ModItems.AZURE_LION_SABRE.get());
            List<ItemStack> e = loot(h, elephant, p);
            int tusks = count(e, ModItems.WHITE_TUSK.get());
            check(h, tusks >= 2 && tusks <= 4, "two or three of his tusks: " + e);
            lances += count(e, ModItems.WHITE_ELEPHANT_LANCE.get());
        }
        // a quarter of the time: in forty kills, between one and twenty-four
        check(h, sabres >= 1 && sabres <= 24, "his broadsword now and then: " + sabres + " in " + rolls);
        check(h, lances >= 1 && lances <= 24, "his lance now and then: " + lances + " in " + rolls);
        h.succeed();
    }

    @GameTest(batch = "lion_camel_theRidgesCreaturesLeaveItsMaterials", template = PLATFORM)
    public static void theRidgesCreaturesLeaveItsMaterials(GameTestHelper h) {
        ServerPlayer p = other(h);
        check(h, count(loot(h, demon(h, ModEntities.LION_DEMON.get(), 89, false, new BlockPos(1, 2, 1)), p), ModItems.MANE_TUFT.get()) >= 1, "lion-demons: mane");
        check(h, count(loot(h, demon(h, ModEntities.ELEPHANT_DEMON.get(), 89, false, new BlockPos(3, 2, 3)), p), ModItems.IVORY_SHARD.get()) >= 1, "elephant-demons: ivory");
        check(h, count(loot(h, demon(h, ModEntities.PENG_DEMON.get(), 89, false, new BlockPos(1, 2, 3)), p), ModItems.STORM_FEATHER.get()) >= 1, "peng-demons: feathers");
        LittleWindDrillerEntity d = demon(h, ModEntities.LITTLE_WIND_DRILLER.get(), 89, false, new BlockPos(3, 2, 1));
        int tokens = 0;
        for (int i = 0; i < 40; i++) tokens += count(loot(h, d, p), ModItems.PATROL_TOKEN.get());
        check(h, tokens >= 5 && tokens <= 35, "the Wind-Driller's token, half the time: " + tokens + " in 40");
        var recipe = h.getLevel().getRecipeManager().byKey(JTTWEngine.id("true_samadhi_fire_from_the_ridge"));
        check(h, recipe.isPresent() && recipe.get().value().getResultItem(h.getLevel().registryAccess()).is(ModItems.TRUE_SAMADHI_FIRE.get()),
                "the furnace's fuel is forged from them too");
        h.succeed();
    }

    // ── The city ────────────────────────────────────────────────────────

    private static boolean open(int x, int y, int z) {
        BlockState s = LionCamelCity.at(x, y, z);
        return s == null || !s.blocksMotion();
    }

    @GameTest(batch = "lion_camel_lionCamelCitysBlueprint", template = PLATFORM)
    public static void lionCamelCitysBlueprint(GameTestHelper h) {
        // the Lion's place on the terrace, the Elephant's in his court
        check(h, LionCamelCity.at(0, LionCamelCity.TERRACE_H, 0).blocksMotion(), "the terrace under the Lion");
        for (int y = 1; y <= 4; y++) check(h, open(0, LionCamelCity.TERRACE_H + y, 0), "room for him at " + y);
        check(h, LionCamelCity.at(0, 0, LionCamelCity.COURT_Z).blocksMotion(), "the court's floor");
        for (int y = 1; y <= 4; y++) check(h, open(0, y, LionCamelCity.COURT_Z), "room for the Elephant at " + y);
        // the main gate open, its plaque over it, the walls standing either side
        for (int y = 1; y < LionCamelCity.GATE_HIGH; y++) check(h, open(0, y, LionCamelCity.WALL_OUT), "the gate at " + y);
        check(h, LionCamelCity.at(0, 10, LionCamelCity.WALL_OUT + 2).is(Blocks.GILDED_BLACKSTONE), "the plaque: 狮驼国");
        check(h, LionCamelCity.at(30, 5, LionCamelCity.WALL_OUT).blocksMotion() || LionCamelCity.at(31, 5, LionCamelCity.WALL_OUT).blocksMotion(), "the wall");
        // the way in: down the avenue from the gate to the palace's gate and up its steps
        for (int z = LionCamelCity.WALL_IN - 1; z > LionCamelCity.PALACE; z--) check(h, open(0, 1, z) && open(0, 2, z), "the avenue at " + z);
        for (int y = 1; y <= 3; y++) check(h, open(0, y, LionCamelCity.PALACE), "the palace gate at " + y);
        // three thrones, the Peng's empty at the Lion's left
        int thrones = 0;
        for (int t : LionCamelCity.THRONES) {
            if (LionCamelCity.at(t, LionCamelCity.TERRACE_H + 2, LionCamelCity.THRONE_Z).is(Blocks.POLISHED_BLACKSTONE_STAIRS)) thrones++;
        }
        eq(h, 3, thrones, "three thrones");
        // the court's tusks of ivory, skulls and bones in the streets
        int bone = 0, skulls = 0;
        for (int x = -LionCamelCity.WALL_IN; x <= LionCamelCity.WALL_IN; x++) for (int z = -LionCamelCity.WALL_IN; z <= LionCamelCity.WALL_IN; z++) {
            BlockState s = LionCamelCity.at(x, 1, z);
            if (s == null) continue;
            if (s.is(Blocks.BONE_BLOCK)) bone++;
            if (s.is(Blocks.SKELETON_SKULL)) skulls++;
        }
        check(h, bone > 40 && skulls > 40, "a city of bones: " + bone + " bones, " + skulls + " skulls");
        // the garrison's posts stand on ground, with room over them
        for (var post : com.jttw.engine.world.feature.LionCamelCityFeature.POSTS) {
            BlockState under = LionCamelCity.at(post.x(), post.y() - 1, post.z());
            check(h, under != null && under.blocksMotion(), "ground under the post at " + post.x() + " " + post.z() + ": " + under);
            check(h, open(post.x(), post.y(), post.z()) && open(post.x(), post.y() + 1, post.z()), "room at the post at " + post.x() + " " + post.z());
        }
        h.succeed();
    }
}
