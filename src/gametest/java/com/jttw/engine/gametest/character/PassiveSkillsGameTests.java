package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.byakko.ByakkoPassives;
import com.jttw.engine.character.dragon.DragonPassives;
import com.jttw.engine.character.erlang.ErlangShenPassives;
import com.jttw.engine.character.nezha.NezhaPassives;
import com.jttw.engine.character.turtle.TurtlePassives;
import com.jttw.engine.character.wukong.WukongPassives;
import com.jttw.engine.character.yuandi.YuanDiPassives;
import com.jttw.engine.item.ImmortalPeachItem;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Consumer;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The passive skills that apply continuously or on hits: one block per character. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class PassiveSkillsGameTests {

    /** One refresh of a once-a-second passive. */
    private static void second(ServerPlayer p, Consumer<TickEvent.PlayerTickEvent> handler) {
        p.tickCount = 19;
        tick(p, handler, 1);
    }

    private static float hurt(Consumer<LivingHurtEvent> handler, net.minecraft.world.entity.LivingEntity target,
                              net.minecraft.world.damagesource.DamageSource src, float amount) {
        LivingHurtEvent e = new LivingHurtEvent(target, src, amount);
        handler.accept(e);
        return e.getAmount();
    }

    // ── Turtle ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void bedrockConstitutionRegeneratesAndHardens(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.TURTLE);
        grant(p, "tu_bedrock_1");
        p.setHealth(10f);
        second(p, TurtlePassives::onPlayerTick);
        eq(h, 16.0, modifier(p, PHYSICAL_RESISTANCE(), "tu_bedrock_pres"), "tier 1 resistance");
        near(h, 10.5, p.getHealth(), "+0.5 HP a second");
        eq(h, null, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "tu_bedrock_kb"), "knockback at tier 1");

        grant(p, "tu_bedrock_2");
        second(p, TurtlePassives::onPlayerTick);
        eq(h, 32.0, modifier(p, PHYSICAL_RESISTANCE(), "tu_bedrock_pres"), "tier 2 resistance");
        near(h, 11.5, p.getHealth(), "+1 HP a second");
        eq(h, 1.0, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "tu_bedrock_kb"), "immune to knockback");

        ServerPlayer other = as(h, ReincarnationOutcome.WHITE_TIGER);
        grant(other, "tu_bedrock_1");
        second(other, TurtlePassives::onPlayerTick);
        eq(h, null, modifier(other, PHYSICAL_RESISTANCE(), "tu_bedrock_pres"), "the Turtle's alone");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void earthenRootsProtectsAfterStandingStillForASecond(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.TURTLE);
        grant(p, "tu_roots_1");
        var cactus = p.damageSources().cactus();
        tick(p, TurtlePassives::onPlayerTick, 1); // first tick records where he stands
        tick(p, TurtlePassives::onPlayerTick, 19);
        near(h, 10.0, hurt(TurtlePassives::onHurt, p, cactus, 10f), "not rooted before 1 s");
        tick(p, TurtlePassives::onPlayerTick, 1);
        check(h, TurtlePassives.isRooted(p), "rooted after 20 still ticks");
        near(h, 8.5, hurt(TurtlePassives::onHurt, p, cactus, 10f), "-15%");
        p.setPos(p.getX() + 1, p.getY(), p.getZ());
        tick(p, TurtlePassives::onPlayerTick, 1);
        near(h, 10.0, hurt(TurtlePassives::onHurt, p, cactus, 10f), "moving breaks the roots");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void earthenRootsTwoReturnsAQuarterOfTheHit(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.TURTLE);
        grant(p, "tu_roots_1", "tu_roots_2");
        tick(p, TurtlePassives::onPlayerTick, 21);
        Zombie zombie = mob(h, EntityType.ZOMBIE, 0, 0);
        List<Hit> hits = watch(zombie);
        near(h, 7.0, hurt(TurtlePassives::onHurt, p, p.damageSources().mobAttack(zombie), 10f), "-30%");
        near(h, 2.5, firstHit(h, hits, DamageTypes.THORNS, zombie).amount(), "a quarter of 10 back");
        h.succeed();
    }

    // ── Nezha ───────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void lotusBodyAndHeavenlyWarriorApplyTheirBonuses(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        grant(p, "nz_fort_1");
        second(p, NezhaPassives::onPlayerTick);
        eq(h, 20.0, modifier(p, Attributes.MAX_HEALTH, "nz_fort_hp"), "Lotus Body I");
        grant(p, "nz_fort_2", "nz_combat_1");
        second(p, NezhaPassives::onPlayerTick);
        eq(h, 40.0, modifier(p, Attributes.MAX_HEALTH, "nz_fort_hp"), "Lotus Body II");
        eq(h, 0.15, modifier(p, Attributes.MOVEMENT_SPEED, "nz_combat_speed"), "Warrior I: speed");
        eq(h, null, modifier(p, Attributes.ATTACK_DAMAGE, "nz_combat_dmg"), "no damage bonus yet");
        PlayerLevel.setLevel(p, 20);
        LevelManager.reapplyStats(p);
        second(p, NezhaPassives::onPlayerTick);
        near(h, 40.0 * LevelPower.of(p), modifier(p, Attributes.MAX_HEALTH, "nz_fort_hp"), "Lotus Body grows with level");
        double attack = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        grant(p, "nz_combat_2", "nz_combat_3");
        second(p, NezhaPassives::onPlayerTick);
        eq(h, 0.20, modifier(p, Attributes.ATTACK_DAMAGE, "nz_combat_dmg"), "Warrior II: damage");
        near(h, attack * 1.2, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "+20% of his whole attack");
        eq(h, 24.0, modifier(p, PHYSICAL_RESISTANCE(), "nz_combat_pres"), "Warrior III: resistance");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heavenlyWarriorThreeIsImmuneToFire(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        grant(p, "nz_combat_1", "nz_combat_2");
        LivingAttackEvent before = new LivingAttackEvent(p, p.damageSources().onFire(), 1f);
        NezhaPassives.onAttacked(before);
        check(h, !before.isCanceled(), "fire still burns at tier 2");
        grant(p, "nz_combat_3");
        for (var src : List.of(p.damageSources().onFire(), p.damageSources().inFire(), p.damageSources().lava())) {
            LivingAttackEvent e = new LivingAttackEvent(p, src, 1f);
            NezhaPassives.onAttacked(e);
            check(h, e.isCanceled(), src.getMsgId() + " blocked at tier 3");
        }
        LivingAttackEvent cactus = new LivingAttackEvent(p, p.damageSources().cactus(), 1f);
        NezhaPassives.onAttacked(cactus);
        check(h, !cactus.isCanceled(), "other damage still lands");
        h.succeed();
    }

    // ── Yuan Di ─────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void ironFleshAddsHealthResistanceAndKnockbackResistance(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.YUAN_DI);
        grant(p, "yd_body_1");
        second(p, YuanDiPassives::onPlayerTick);
        eq(h, 20.0, modifier(p, Attributes.MAX_HEALTH, "yd_body_hp"), "I: health");
        eq(h, 8.0, modifier(p, PHYSICAL_RESISTANCE(), "yd_body_pres"), "I: resistance");
        eq(h, 0.10, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "yd_body_kb"), "I: knockback");
        grant(p, "yd_body_2");
        second(p, YuanDiPassives::onPlayerTick);
        eq(h, 40.0, modifier(p, Attributes.MAX_HEALTH, "yd_body_hp"), "II: health");
        eq(h, 16.0, modifier(p, PHYSICAL_RESISTANCE(), "yd_body_pres"), "II: resistance");
        eq(h, 0.25, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "yd_body_kb"), "II: knockback");
        h.succeed();
    }

    // ── Qi passives ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void qiRegenNodesSpeedUpRegenForEveryoneWhoHasThem(GameTestHelper h) {
        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        near(h, 1.0, QiManager.regenMultiplier(nezha), "no nodes");
        grant(nezha, "nz_qi_1", "nz_qi_2");
        near(h, 1.30, QiManager.regenMultiplier(nezha), "Heavenly Qi I+II");
        ServerPlayer yuanDi = as(h, ReincarnationOutcome.YUAN_DI);
        grant(yuanDi, "yd_qi_1");
        near(h, 1.15, QiManager.regenMultiplier(yuanDi), "Qi Flow I");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void qiFlowRaisesMaxQiAndItSticks(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.YUAN_DI);
        grant(p, "yd_qi_1", "yd_qi_2");
        QiManager.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        double base = StatBlock.compute(PlayerLevel.getProgressionClass(p), PlayerLevel.getLevel(p)).qi();
        near(h, base + 100, QiData.getMax(p), "+50 max Qi per node");
        h.succeed();
    }

    /** The peach's +4700 max Qi used to be reset to the level value on the very next tick. */
    @GameTest(template = PLATFORM)
    public static void peachMaxQiSurvivesTheQiTick(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.getPersistentData().putBoolean(ImmortalPeachItem.KEY_PEACH_CONSUMED, true);
        QiManager.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        double base = StatBlock.compute(PlayerLevel.getProgressionClass(p), PlayerLevel.getLevel(p)).qi();
        near(h, base + ImmortalPeachItem.QI_POOL_BONUS, QiData.getMax(p), "max Qi with the peach");
        h.succeed();
    }

    // ── Byakko ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void scentOfBloodHitsWoundedTargetsHarder(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.WHITE_TIGER);
        Pig healthy = mob(h, EntityType.PIG, 1, 4);
        Pig wounded = mob(h, EntityType.PIG, 3, 4);
        wounded.setHealth(4f);
        var src = p.damageSources().playerAttack(p);
        near(h, 10.0, hurt(ByakkoPassives::onHurt, wounded, src, 10f), "no node");
        grant(p, "bk_scent_1");
        near(h, 10.0, hurt(ByakkoPassives::onHurt, healthy, src, 10f), "healthy target");
        near(h, 11.5, hurt(ByakkoPassives::onHurt, wounded, src, 10f), "I: +15% below half health");
        grant(p, "bk_scent_2");
        near(h, 13.0, hurt(ByakkoPassives::onHurt, wounded, src, 10f), "II: +30%");
        h.succeed();
    }

    // ── Dragon ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "draconicPressureSlowsThenWeakensNearbyHostiles")
    public static void draconicPressureSlowsThenWeakensNearbyHostiles(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.DRAGON);
        Zombie zombie = mob(h, EntityType.ZOMBIE, 2, 4);
        Pig pig = mob(h, EntityType.PIG, 0, 4);
        grant(p, "dr_pressure_1");
        second(p, DragonPassives::onPlayerTick);
        eq(h, 0, amp(zombie, MobEffects.MOVEMENT_SLOWDOWN), "zombie slowed");
        eq(h, -1, amp(zombie, MobEffects.WEAKNESS), "not weakened at tier 1");
        eq(h, -1, amp(pig, MobEffects.MOVEMENT_SLOWDOWN), "the pig is no enemy");
        grant(p, "dr_pressure_2");
        second(p, DragonPassives::onPlayerTick);
        eq(h, 0, amp(zombie, MobEffects.WEAKNESS), "weakened at tier 2");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void tidalRecoveryRegenerates(GameTestHelper h) {
        near(h, 0.5, DragonPassives.tidalRegenPerSecond(1, true), "I in water");
        near(h, 0.0, DragonPassives.tidalRegenPerSecond(1, false), "I on land");
        near(h, 1.0, DragonPassives.tidalRegenPerSecond(2, true), "II in water");
        near(h, 0.5, DragonPassives.tidalRegenPerSecond(2, false), "II always regenerates");
        ServerPlayer p = as(h, ReincarnationOutcome.DRAGON);
        grant(p, "dr_tidal_1", "dr_tidal_2");
        p.setHealth(10f);
        second(p, DragonPassives::onPlayerTick);
        near(h, 10.5, p.getHealth(), "II on dry land");
        h.succeed();
    }

    // ── Erlang Shen ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void celestialHuntMarksAndPunishesTheMarked(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        grant(p, "es_hunt_1");
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        var src = p.damageSources().playerAttack(p);
        near(h, 10.0, hurt(ErlangShenPassives::onHurt, pig, src, 10f), "first hit only marks");
        check(h, ErlangShenPassives.isMarkedBy(pig, p), "marked");
        near(h, 11.5, hurt(ErlangShenPassives::onHurt, pig, src, 10f), "I: +15% on the marked");
        check(h, !pig.hasEffect(MobEffects.GLOWING), "no glow at tier 1");
        grant(p, "es_hunt_2");
        near(h, 12.5, hurt(ErlangShenPassives::onHurt, pig, src, 10f), "II: +25%");
        check(h, pig.hasEffect(MobEffects.GLOWING), "marked targets glow at tier 2");
        pig.getPersistentData().putLong(ErlangShenPassives.KEY_MARKED_UNTIL, h.getLevel().getGameTime());
        check(h, !ErlangShenPassives.isMarkedBy(pig, p), "the mark wears off");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void celestialHuntCountsXiaotianQuansBites(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        online(h, p);
        grant(p, "es_hunt_1");
        var hound = h.spawnWithNoFreeWill(com.jttw.engine.registry.ModEntities.XIAOTIAN_QUAN.get(),
                new net.minecraft.core.BlockPos(0, 2, 0));
        hound.getPersistentData().putUUID(com.jttw.engine.character.erlang.XiaotianQuanManager.XTQ_OWNER_TAG, p.getUUID());
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        var bite = p.damageSources().mobAttack(hound);
        near(h, 10.0, hurt(ErlangShenPassives::onHurt, pig, bite, 10f), "unmarked");
        check(h, !ErlangShenPassives.isMarkedBy(pig, p), "the hound's bite does not mark");
        hurt(ErlangShenPassives::onHurt, pig, p.damageSources().playerAttack(p), 10f);
        near(h, 11.5, hurt(ErlangShenPassives::onHurt, pig, bite, 10f), "the hound hits the marked harder");
        h.succeed();
    }

    // ── Wukong ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void yaoWangBuildsFuryWithEachHit(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        grant(p, "wu_combat_1");
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        var src = p.damageSources().playerAttack(p);
        near(h, 11.0, hurt(WukongPassives::onHurt, pig, src, 10f), "+10%, no fury yet");
        eq(h, 1, WukongPassives.fury(p), "one stack");
        near(h, 11.3, hurt(WukongPassives::onHurt, pig, src, 10f), "+10% and +3% for the stack");
        for (int i = 0; i < 10; i++) hurt(WukongPassives::onHurt, pig, src, 10f);
        eq(h, 5, WukongPassives.fury(p), "tier 1 caps at 5");
        second(p, WukongPassives::onPlayerTick);
        near(h, 0.15, modifier(p, Attributes.MOVEMENT_SPEED, "wu_fury_speed"), "+3% speed a stack");
        p.getPersistentData().putLong(WukongPassives.KEY_FURY_AT, h.getLevel().getGameTime() - 161);
        second(p, WukongPassives::onPlayerTick);
        eq(h, 0, WukongPassives.fury(p), "lost after 8 s out of combat");
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "wu_fury_speed"), "speed gone with it");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void yaoWangTwoStealsLife(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        grant(p, "wu_combat_1", "wu_combat_2");
        p.setHealth(10f);
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        near(h, 12.0, hurt(WukongPassives::onHurt, pig, p.damageSources().playerAttack(p), 10f), "+20%");
        near(h, 10.6, p.getHealth(), "5% of 12 back");
        p.getPersistentData().putLong(WukongPassives.KEY_FURY_AT, h.getLevel().getGameTime() - 200);
        second(p, WukongPassives::onPlayerTick);
        eq(h, 1, WukongPassives.fury(p), "stacks last 12 s at tier 2");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "yaoWangThreeStandsFirmAndWeakensFoes")
    public static void yaoWangThreeStandsFirmAndWeakensFoes(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        grant(p, "wu_combat_1", "wu_combat_2", "wu_combat_3");
        Zombie zombie = mob(h, EntityType.ZOMBIE, 2, 4);
        p.getPersistentData().putInt(WukongPassives.KEY_FURY, 9);
        p.getPersistentData().putLong(WukongPassives.KEY_FURY_AT, h.getLevel().getGameTime());
        second(p, WukongPassives::onPlayerTick);
        eq(h, null, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "wu_fury_kb"), "9 stacks: still pushed");
        eq(h, 0, amp(zombie, MobEffects.WEAKNESS), "weakness aura while furious");
        p.getPersistentData().putInt(WukongPassives.KEY_FURY, 10);
        second(p, WukongPassives::onPlayerTick);
        eq(h, 1.0, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "wu_fury_kb"), "10 stacks: no knockback");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void passivesAreTheirOwnCharactersOnly(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        grant(erlang, "wu_combat_1", "bk_scent_1", "nz_fort_1", "yd_body_1");
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        pig.setHealth(2f);
        var src = erlang.damageSources().playerAttack(erlang);
        near(h, 10.0, hurt(WukongPassives::onHurt, pig, src, 10f), "Yao Wang");
        near(h, 10.0, hurt(ByakkoPassives::onHurt, pig, src, 10f), "Scent of Blood");
        second(erlang, NezhaPassives::onPlayerTick);
        second(erlang, YuanDiPassives::onPlayerTick);
        eq(h, null, modifier(erlang, Attributes.MAX_HEALTH, "nz_fort_hp"), "Lotus Body");
        eq(h, null, modifier(erlang, Attributes.MAX_HEALTH, "yd_body_hp"), "Iron Flesh");
        h.succeed();
    }
}
