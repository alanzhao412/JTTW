package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.erlang.ErlangShenAbilityHandler;
import com.jttw.engine.character.erlang.XiaotianQuanManager;
import com.jttw.engine.entity.XiaotianQuanEntity;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Erlang Shen: Eye of Heaven, Xiaotian Quan, Celestial Tactics and the spear bonus. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ErlangShenGameTests {

    private static ServerPlayer erlang(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        qi(p, 100f, 100f);
        return p;
    }

    private static PlayableCharacter character() {
        return Characters.byOutcome(ReincarnationOutcome.EYE_OF_HEAVEN.id);
    }

    private static List<XiaotianQuanEntity> hounds(ServerPlayer p) {
        return near(p, XiaotianQuanEntity.class, 8).stream()
                .filter(x -> x.getPersistentData().hasUUID(XiaotianQuanManager.XTQ_OWNER_TAG)
                        && x.getPersistentData().getUUID(XiaotianQuanManager.XTQ_OWNER_TAG).equals(p.getUUID()))
                .toList();
    }

    // ── Eye of Heaven (slot 2, toggle) ──────────────────────────────

    @GameTest(template = PLATFORM, batch = "eyeOfHeavenNeedsTierOne")
    public static void eyeOfHeavenNeedsTierOne(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        character().activate(p, 2, AbilityAction.USE);
        check(h, !ErlangShenAbilityHandler.isActive(p.getUUID()), "eye stays closed");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "eyeOfHeavenTogglesOnUseOnly")
    public static void eyeOfHeavenTogglesOnUseOnly(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        grant(p, "es_eye_1");
        character().activate(p, 2, AbilityAction.START);
        check(h, !ErlangShenAbilityHandler.isActive(p.getUUID()), "START is not a toggle press");
        character().activate(p, 2, AbilityAction.USE);
        check(h, ErlangShenAbilityHandler.isActive(p.getUUID()), "open");
        character().activate(p, 2, AbilityAction.USE);
        check(h, !ErlangShenAbilityHandler.isActive(p.getUUID()), "closed again");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "eyeOfHeavenDrainsPointZeroEightQiPerTickBelowTierThree")
    public static void eyeOfHeavenDrainsPointZeroEightQiPerTickBelowTierThree(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_eye_1", "es_eye_2");
        long start = h.getTick();
        ErlangShenAbilityHandler.toggleEye(p);
        h.runAfterDelay(10, () -> {
            long ticks = h.getTick() - start;
            close(h, 100.0 - 0.08 * ticks, QiData.getCurrent(p), 0.09, "Qi after " + ticks + " ticks");
            ErlangShenAbilityHandler.toggleEye(p);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "eyeOfHeavenTierThreeDrainsOnlyPointZeroFive")
    public static void eyeOfHeavenTierThreeDrainsOnlyPointZeroFive(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_eye_1", "es_eye_2", "es_eye_3");
        long start = h.getTick();
        ErlangShenAbilityHandler.toggleEye(p);
        h.runAfterDelay(10, () -> {
            long ticks = h.getTick() - start;
            close(h, 100.0 - 0.05 * ticks, QiData.getCurrent(p), 0.06, "Qi after " + ticks + " ticks");
            ErlangShenAbilityHandler.toggleEye(p);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "eyeOfHeavenClosesWhenQiRunsOut")
    public static void eyeOfHeavenClosesWhenQiRunsOut(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_eye_1");
        QiData.setCurrent(p, 0.2f);
        ErlangShenAbilityHandler.toggleEye(p);
        h.succeedWhen(() -> check(h, !ErlangShenAbilityHandler.isActive(p.getUUID()), "eye closed"));
    }

    // ── Xiaotian Quan (slot 1, toggle) ──────────────────────────────

    @GameTest(template = PLATFORM, batch = "houndNeedsTheXiaotianQuanNode")
    public static void houndNeedsTheXiaotianQuanNode(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        character().activate(p, 1, AbilityAction.USE);
        eq(h, 0, hounds(p).size(), "hounds");
        check(h, !XiaotianQuanManager.isActive(p.getUUID()), "not active");
        near(h, 100f, QiData.getCurrent(p), "Qi");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "summoningTheHoundCosts15QiAndASecondPressDismissesIt")
    public static void summoningTheHoundCosts15QiAndASecondPressDismissesIt(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        grant(p, "es_xtq_1");
        character().activate(p, 1, AbilityAction.USE);
        List<XiaotianQuanEntity> one = hounds(p);
        eq(h, 1, one.size(), "one hound");
        near(h, 85f, QiData.getCurrent(p), "Qi after summoning");
        check(h, XiaotianQuanManager.isActive(p.getUUID()), "active");
        // no Hound Training: the tier-0 row of the stat table
        near(h, 30.0, one.get(0).getMaxHealth(), "hp");
        near(h, 4.0, one.get(0).getAttributeBaseValue(Attributes.ATTACK_DAMAGE), "attack");
        near(h, 0.35, one.get(0).getAttributeBaseValue(Attributes.MOVEMENT_SPEED), "speed");
        near(h, 2.0, one.get(0).getAttributeBaseValue(Attributes.ARMOR), "armor");

        character().activate(p, 1, AbilityAction.USE);
        check(h, one.get(0).isRemoved(), "hound dismissed");
        check(h, !XiaotianQuanManager.isActive(p.getUUID()), "not active");
        near(h, 85f, QiData.getCurrent(p), "dismissing costs nothing");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "houndStatsGrowWithHoundTraining")
    public static void houndStatsGrowWithHoundTraining(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        grant(p, "es_xtq_1", "es_xtq_growth_1");
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity trained = hounds(p).get(0);
        near(h, 50.0, trained.getMaxHealth(), "tier 1 hp");
        near(h, 7.0, trained.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), "tier 1 attack");
        near(h, 5.0, trained.getAttributeBaseValue(Attributes.ARMOR), "tier 1 armor");
        XiaotianQuanManager.toggle(p);

        grant(p, "es_xtq_growth_2", "es_xtq_growth_3");
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity peak = hounds(p).get(0);
        near(h, 80.0, peak.getMaxHealth(), "tier 3 hp");
        near(h, 12.0, peak.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), "tier 3 attack");
        near(h, 0.42, peak.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), "tier 3 speed");
        near(h, 8.0, peak.getAttributeBaseValue(Attributes.ARMOR), "tier 3 armor");

        // Its health and attack follow its owner's level while it is out; armor and speed do not
        PlayerLevel.setLevel(p, 20);
        LevelManager.reapplyStats(p);
        XiaotianQuanManager.refreshStats(p, peak);
        near(h, 80.0 * LevelPower.of(p), peak.getMaxHealth(), "level 20 hp");
        near(h, peak.getMaxHealth(), peak.getHealth(), "still at full health");
        near(h, 12.0 * LevelPower.of(p), peak.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), "level 20 attack");
        near(h, 8.0, peak.getAttributeBaseValue(Attributes.ARMOR), "armor as before");
        XiaotianQuanManager.dismiss(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "houndSummonWithoutFifteenQiIsRefused")
    public static void houndSummonWithoutFifteenQiIsRefused(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        grant(p, "es_xtq_1");
        QiData.setCurrent(p, 14f);
        XiaotianQuanManager.toggle(p);
        eq(h, 0, hounds(p).size(), "hounds");
        near(h, 14f, QiData.getCurrent(p), "Qi");
        check(h, !XiaotianQuanManager.isActive(p.getUUID()), "not active");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "houndDrainsQiAndLeavesWhenItRunsOut")
    public static void houndDrainsQiAndLeavesWhenItRunsOut(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_xtq_1");
        QiData.setCurrent(p, 15.3f); // 0.3 left after summoning: about ten ticks at 0.03
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity hound = hounds(p).get(0);
        h.succeedWhen(() -> {
            check(h, !XiaotianQuanManager.isActive(p.getUUID()), "manager let it go");
            check(h, hound.isRemoved(), "hound entity removed");
        });
    }

    @GameTest(template = PLATFORM, batch = "ownerCannotHurtTheirHound")
    public static void ownerCannotHurtTheirHound(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        grant(p, "es_xtq_1");
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity hound = hounds(p).get(0);
        AttackEntityEvent own = new AttackEntityEvent(p, hound);
        XiaotianQuanManager.onPlayerAttacks(own);
        check(h, own.isCanceled(), "owner's attack cancelled");
        AttackEntityEvent stranger = new AttackEntityEvent(erlang(h), hound);
        XiaotianQuanManager.onPlayerAttacks(stranger);
        check(h, !stranger.isCanceled(), "a stranger can hit it");
        XiaotianQuanManager.dismiss(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "houndBitesSlowTheTarget")
    public static void houndBitesSlowTheTarget(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_xtq_1");
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity hound = hounds(p).get(0);
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        XiaotianQuanManager.onXtqAttacks(new LivingHurtEvent(pig, h.getLevel().damageSources().mobAttack(hound), 2f));
        eq(h, 0, amp(pig, MobEffects.MOVEMENT_SLOWDOWN), "Slowness I");
        eq(h, 60, duration(pig, MobEffects.MOVEMENT_SLOWDOWN), "for 3 s");
        XiaotianQuanManager.dismiss(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "trainedHoundMakesTheNearestHostileGlow")
    public static void trainedHoundMakesTheNearestHostileGlow(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_xtq_1", "es_xtq_growth_1", "es_xtq_growth_2");
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        XiaotianQuanManager.toggle(p);
        h.succeedWhen(() -> {
            check(h, com.jttw.engine.world.PrivateGlow.glowsFor(p, husk), "husk tracked, for him");
            XiaotianQuanManager.dismiss(p);
        });
    }

    @GameTest(template = PLATFORM, batch = "untrainedHoundDoesNotTrack")
    public static void untrainedHoundDoesNotTrack(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_xtq_1", "es_xtq_growth_1");
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        XiaotianQuanManager.toggle(p);
        h.runAfterDelay(25, () -> {
            check(h, !com.jttw.engine.world.PrivateGlow.glowsFor(p, husk), "no tracking below Hound Training II");
            XiaotianQuanManager.dismiss(p);
            h.succeed();
        });
    }

    // ── Celestial Tactics (passive) ─────────────────────────────────

    private static float hit(GameTestHelper h, Pig target, net.minecraft.world.damagesource.DamageSource src) {
        LivingHurtEvent ev = new LivingHurtEvent(target, src, 10f);
        ErlangShenAbilityHandler.onCelestialTacticsHit(ev);
        return ev.getAmount();
    }

    @GameTest(template = PLATFORM, batch = "tacticsGiveThirtyPercentToTheCoordinatedSecondHit")
    public static void tacticsGiveThirtyPercentToTheCoordinatedSecondHit(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_xtq_1", "es_tactics_1");
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity hound = hounds(p).get(0);
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        var ds = h.getLevel().damageSources();
        near(h, 10f, hit(h, pig, ds.playerAttack(p)), "first hit is plain");
        near(h, 13f, hit(h, pig, ds.mobAttack(hound)), "hound follows up: +30%");
        near(h, 10f, hit(h, pig, ds.mobAttack(hound)), "pair resets after a coordinated hit");
        eq(h, -1, amp(pig, MobEffects.WEAKNESS), "no debuff at tier 1");
        XiaotianQuanManager.dismiss(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tacticsTierTwoAlsoWeakensAndSlows")
    public static void tacticsTierTwoAlsoWeakensAndSlows(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_xtq_1", "es_tactics_1", "es_tactics_2");
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity hound = hounds(p).get(0);
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        var ds = h.getLevel().damageSources();
        hit(h, pig, ds.mobAttack(hound));
        near(h, 13f, hit(h, pig, ds.playerAttack(p)), "player follows the hound: +30%");
        eq(h, 1, amp(pig, MobEffects.WEAKNESS), "Weakness II");
        eq(h, 1, amp(pig, MobEffects.MOVEMENT_SLOWDOWN), "Slowness II");
        eq(h, 40, duration(pig, MobEffects.WEAKNESS), "for 2 s");
        XiaotianQuanManager.dismiss(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "noTacticsNodeNoBonus")
    public static void noTacticsNodeNoBonus(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        online(h, p);
        grant(p, "es_xtq_1");
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity hound = hounds(p).get(0);
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        var ds = h.getLevel().damageSources();
        hit(h, pig, ds.playerAttack(p));
        near(h, 10f, hit(h, pig, ds.mobAttack(hound)), "no bonus");
        XiaotianQuanManager.dismiss(p);
        h.succeed();
    }

    // ── Weapon bonus ────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "erlangShenHitsHarderWithTheSpearAndAxe")
    public static void erlangShenHitsHarderWithTheSpearAndAxe(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        var ds = h.getLevel().damageSources();

        LivingHurtEvent bare = new LivingHurtEvent(pig, ds.playerAttack(p), 10f);
        ErlangShenAbilityHandler.onWeaponDamageBonus(bare);
        near(h, 10f, bare.getAmount(), "bare hand");

        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.THREE_POINTED_SPEAR.get()));
        LivingHurtEvent spear = new LivingHurtEvent(pig, ds.playerAttack(p), 10f);
        ErlangShenAbilityHandler.onWeaponDamageBonus(spear);
        near(h, 16.67f, spear.getAmount(), "spear x1.667");

        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ERLANG_AXE.get()));
        LivingHurtEvent axe = new LivingHurtEvent(pig, ds.playerAttack(p), 10f);
        ErlangShenAbilityHandler.onWeaponDamageBonus(axe);
        near(h, 16.67f, axe.getAmount(), "axe x1.667");

        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        wukong.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.THREE_POINTED_SPEAR.get()));
        LivingHurtEvent other = new LivingHurtEvent(pig, ds.playerAttack(wukong), 10f);
        ErlangShenAbilityHandler.onWeaponDamageBonus(other);
        near(h, 10f, other.getAmount(), "no bonus for other characters");
        h.succeed();
    }

    // ── Key routing ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "erlangKeysReachTheirAbilitiesOnlyOnUse")
    public static void erlangKeysReachTheirAbilitiesOnlyOnUse(GameTestHelper h) {
        ServerPlayer p = erlang(h);
        grant(p, "es_xtq_1", "es_eye_1", "es_transform_1");
        PlayableCharacter c = character();
        c.activate(p, 1, AbilityAction.START);
        c.activate(p, 1, AbilityAction.STOP);
        check(h, !XiaotianQuanManager.isActive(p.getUUID()), "hound key ignores START/STOP");
        c.activate(p, 0, AbilityAction.USE); // 72 Transformations: client-side menu
        c.activate(p, 3, AbilityAction.USE); // no fourth slot
        near(h, 100f, QiData.getCurrent(p), "slots 0 and 3 do nothing server-side");
        check(h, !ErlangShenAbilityHandler.isActive(p.getUUID()), "eye untouched");
        c.activate(p, 1, AbilityAction.USE);
        check(h, XiaotianQuanManager.isActive(p.getUUID()), "slot 1 summons the hound");
        c.activate(p, 2, AbilityAction.USE);
        check(h, ErlangShenAbilityHandler.isActive(p.getUUID()), "slot 2 opens the eye");
        ErlangShenAbilityHandler.toggleEye(p);
        XiaotianQuanManager.dismiss(p);
        h.succeed();
    }
}
