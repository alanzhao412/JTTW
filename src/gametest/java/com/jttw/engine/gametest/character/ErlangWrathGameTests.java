package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.erlang.ErlangWrath;
import com.jttw.engine.character.erlang.XiaotianQuanManager;
import com.jttw.engine.entity.XiaotianQuanEntity;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Erlang Shen's Zhàn Shén: Wrath below 40% health, its bonuses by tier, and the berserk hound. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ErlangWrathGameTests {

    private static ServerPlayer erlang(GameTestHelper h, String... nodes) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        qi(p, 100f, 100f);
        grant(p, nodes);
        return p;
    }

    private static void health(ServerPlayer p, float share) {
        p.setHealth(p.getMaxHealth() * share);
    }

    /** A blow of 10 from {@code p} to {@code pig}, through Wrath's damage handler alone. */
    private static float blow(GameTestHelper h, ServerPlayer p, Pig pig) {
        LivingIncomingDamageEvent ev = incoming(pig, h.getLevel().damageSources().playerAttack(p), 10f);
        ErlangWrath.onHurt(ev);
        return ev.getAmount();
    }

    private static XiaotianQuanEntity summon(GameTestHelper h, ServerPlayer p) {
        XiaotianQuanManager.toggle(p);
        XiaotianQuanEntity hound = XiaotianQuanManager.houndOf(p);
        check(h, hound != null, "the hound is out");
        CharTest.onEnd(h, () -> XiaotianQuanManager.dismiss(p));
        return hound;
    }

    @GameTest(template = PLATFORM, batch = "wrathComesBelowFortyPercentAndGoesAtIt")
    public static void wrathComesBelowFortyPercentAndGoesAtIt(GameTestHelper h) {
        ServerPlayer p = erlang(h, "es_wrath_1");
        health(p, 0.41f);
        check(h, !ErlangWrath.inWrath(p), "41%: no Wrath");
        health(p, 0.39f);
        check(h, ErlangWrath.inWrath(p), "39%: Wrath");
        health(p, 0.40f);
        check(h, !ErlangWrath.inWrath(p), "healed to 40%: out of it");

        ServerPlayer untrained = erlang(h);
        health(untrained, 0.2f);
        check(h, !ErlangWrath.inWrath(untrained), "no Wrath without Zhàn Shén");

        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        grant(other, "es_wrath_1", "es_wrath_2", "es_wrath_3");
        health(other, 0.2f);
        check(h, !ErlangWrath.inWrath(other), "Wrath is Erlang Shen's alone");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "wrathAddsFifteenTwentyFiveThirtyFivePercentDamage")
    public static void wrathAddsFifteenTwentyFiveThirtyFivePercentDamage(GameTestHelper h) {
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        ServerPlayer p = erlang(h, "es_wrath_1");
        near(h, 10f, blow(h, p, pig), "healthy: no bonus");
        health(p, 0.3f);
        near(h, 11.5f, blow(h, p, pig), "Zhàn Shén I: +15%");
        grant(p, "es_wrath_2");
        near(h, 12.5f, blow(h, p, pig), "Zhàn Shén II: +25%");
        grant(p, "es_wrath_3");
        near(h, 13.5f, blow(h, p, pig), "Zhàn Shén III: +35%");

        // his abilities too: the Eye's strike is magic from him
        LivingIncomingDamageEvent magic = incoming(pig, h.getLevel().damageSources().indirectMagic(p, p), 10f);
        ErlangWrath.onHurt(magic);
        near(h, 13.5f, magic.getAmount(), "the Eye's strike: +35%");

        ServerPlayer untrained = erlang(h);
        health(untrained, 0.3f);
        near(h, 10f, blow(h, untrained, pig), "no Zhàn Shén: no bonus");
        ServerPlayer other = as(h, ReincarnationOutcome.YUAN_DI);
        grant(other, "es_wrath_1", "es_wrath_2", "es_wrath_3");
        health(other, 0.3f);
        near(h, 10f, blow(h, other, pig), "not Erlang Shen: no bonus");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "wrathSpeedsHimAndAtTwoHeCannotBeKnockedBack")
    public static void wrathSpeedsHimAndAtTwoHeCannotBeKnockedBack(GameTestHelper h) {
        ServerPlayer p = erlang(h, "es_wrath_1");
        double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double attack = p.getAttributeValue(Attributes.ATTACK_SPEED);
        ErlangWrath.refresh(p);
        near(h, speed, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "healthy: no speed");

        health(p, 0.3f);
        ErlangWrath.refresh(p);
        near(h, speed * 1.15, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "I: +15% speed");
        near(h, attack * 1.15, p.getAttributeValue(Attributes.ATTACK_SPEED), "I: +15% attack speed");
        near(h, 0.0, p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), "I: knocked back as ever");

        grant(p, "es_wrath_2");
        ErlangWrath.refresh(p);
        near(h, speed * 1.25, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "II: +25% speed");
        near(h, 1.0, p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), "II: no knockback");

        grant(p, "es_wrath_3");
        ErlangWrath.refresh(p);
        near(h, speed * 1.35, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "III: +35% speed");
        near(h, attack * 1.35, p.getAttributeValue(Attributes.ATTACK_SPEED), "III: +35% attack speed");

        health(p, 0.5f);
        ErlangWrath.refresh(p);
        near(h, speed, p.getAttributeValue(Attributes.MOVEMENT_SPEED), "healed: speed gone");
        near(h, attack, p.getAttributeValue(Attributes.ATTACK_SPEED), "healed: attack speed gone");
        near(h, 0.0, p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), "healed: footing gone");
        check(h, modifier(p, Attributes.MOVEMENT_SPEED, ErlangWrath.SPEED) == null, "no modifier left");

        ServerPlayer other = as(h, ReincarnationOutcome.YUAN_DI);
        grant(other, "es_wrath_1", "es_wrath_2");
        health(other, 0.3f);
        double otherSpeed = other.getAttributeValue(Attributes.MOVEMENT_SPEED);
        ErlangWrath.refresh(other);
        near(h, otherSpeed, other.getAttributeValue(Attributes.MOVEMENT_SPEED), "not Erlang Shen");
        near(h, 0.0, other.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), "not Erlang Shen: no footing");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "atThreeHisWrathfulBlowsHealHimATenth")
    public static void atThreeHisWrathfulBlowsHealHimATenth(GameTestHelper h) {
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        ServerPlayer p = erlang(h, "es_wrath_1", "es_wrath_2");
        var src = h.getLevel().damageSources().playerAttack(p);
        health(p, 0.3f);
        float before = p.getHealth();
        ErlangWrath.onDealt(new LivingDamageEvent.Post(pig, new DamageContainer(src, 10f)));
        near(h, before, p.getHealth(), "II: no lifesteal");

        grant(p, "es_wrath_3");
        ErlangWrath.onDealt(new LivingDamageEvent.Post(pig, new DamageContainer(src, 10f)));
        near(h, before + 1f, p.getHealth(), "III: a tenth of 10 dealt");

        health(p, 0.6f);
        before = p.getHealth();
        ErlangWrath.onDealt(new LivingDamageEvent.Post(pig, new DamageContainer(src, 10f)));
        near(h, before, p.getHealth(), "out of Wrath: no lifesteal");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "enteringWrathIsCuedOnceNotOnEveryCrossing")
    public static void enteringWrathIsCuedOnceNotOnEveryCrossing(GameTestHelper h) {
        ServerPlayer p = erlang(h, "es_wrath_1");
        List<String> heard = new ArrayList<>();
        listen(p, heard);
        tick(p, ErlangWrath::onPlayerTick, 2);
        eq(h, 0, heard.size(), "healthy: nothing said");
        health(p, 0.3f);
        tick(p, ErlangWrath::onPlayerTick, 2);
        eq(h, 1, heard.size(), "entering Wrath: one line");
        check(h, heard.get(0).contains("Zhàn Shén"), "the line names it: " + heard);
        health(p, 0.5f);
        tick(p, ErlangWrath::onPlayerTick, 1);
        health(p, 0.3f);
        tick(p, ErlangWrath::onPlayerTick, 1);
        eq(h, 1, heard.size(), "crossing straight back: no second line");
        h.succeed();
    }

    // ── Xiaotian Quan ───────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "atTwoTheHoundGoesBerserkInHisWrath")
    public static void atTwoTheHoundGoesBerserkInHisWrath(GameTestHelper h) {
        ServerPlayer p = erlang(h, "es_xtq_1", "es_wrath_1");
        online(h, p);
        XiaotianQuanEntity hound = summon(h, p);
        double bite = hound.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double run = hound.getAttributeValue(Attributes.MOVEMENT_SPEED);

        health(p, 0.3f);
        check(h, !ErlangWrath.berserk(hound), "Zhàn Shén I: no berserk");
        XiaotianQuanManager.updateBerserk(hound, ErlangWrath.berserk(hound));
        check(h, !hound.isBerserk(), "not berserk at I");

        grant(p, "es_wrath_2");
        check(h, ErlangWrath.berserk(hound), "Zhàn Shén II in Wrath: berserk");
        XiaotianQuanManager.updateBerserk(hound, ErlangWrath.berserk(hound));
        check(h, hound.isBerserk(), "berserk flag (its look) set");
        near(h, bite * 1.30, hound.getAttributeValue(Attributes.ATTACK_DAMAGE), "+30% bite");
        near(h, run * 1.25, hound.getAttributeValue(Attributes.MOVEMENT_SPEED), "+25% speed");

        health(p, 0.9f);
        XiaotianQuanManager.updateBerserk(hound, ErlangWrath.berserk(hound));
        check(h, !hound.isBerserk(), "he is healed: it calms");
        near(h, bite, hound.getAttributeValue(Attributes.ATTACK_DAMAGE), "bite as before");
        near(h, run, hound.getAttributeValue(Attributes.MOVEMENT_SPEED), "speed as before");

        // and the manager's own tick turns it, with no help
        health(p, 0.3f);
        h.succeedWhen(() -> check(h, hound.isBerserk(), "berserk on the manager's tick"));
    }

    @GameTest(template = PLATFORM, batch = "theBerserkHoundHuntsTheNearestHostile")
    public static void theBerserkHoundHuntsTheNearestHostile(GameTestHelper h) {
        ServerPlayer p = erlang(h, "es_xtq_1", "es_wrath_1", "es_wrath_2");
        online(h, p);
        Husk husk = mob(h, EntityType.HUSK, 4, 4);
        // (not invulnerable: nothing takes an invulnerable mob for an enemy) hardy, so it lasts the test
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        husk.setHealth(1000f);
        XiaotianQuanEntity hound = summon(h, p);
        h.runAfterDelay(20, () -> {
            check(h, hound.getTarget() == null, "calm, it leaves the husk be");
            health(p, 0.3f);
        });
        h.runAfterDelay(25, () -> h.succeedWhen(() -> check(h, hound.getTarget() == husk, "berserk, it hunts the husk")));
    }

    @GameTest(template = PLATFORM, batch = "atThreeTheBerserkHoundsBitesSlowWithSlownessTwo")
    public static void atThreeTheBerserkHoundsBitesSlowWithSlownessTwoForFiveSeconds(GameTestHelper h) {
        ServerPlayer p = erlang(h, "es_xtq_1", "es_wrath_1", "es_wrath_2");
        online(h, p);
        XiaotianQuanEntity hound = summon(h, p);
        var bite = h.getLevel().damageSources().mobAttack(hound);

        health(p, 0.3f);
        Pig two = mob(h, EntityType.PIG, 0, 0);
        XiaotianQuanManager.onXtqAttacks(incoming(two, bite, 2f));
        eq(h, 0, amp(two, MobEffects.MOVEMENT_SLOWDOWN), "Zhàn Shén II, no hound III: the bite's own Slowness I");
        eq(h, 40, duration(two, MobEffects.MOVEMENT_SLOWDOWN), "for 2 s");

        grant(p, "es_wrath_3");
        Pig three = mob(h, EntityType.PIG, 1, 0);
        XiaotianQuanManager.onXtqAttacks(incoming(three, bite, 2f));
        eq(h, 1, amp(three, MobEffects.MOVEMENT_SLOWDOWN), "Zhàn Shén III in Wrath: Slowness II");
        eq(h, 100, duration(three, MobEffects.MOVEMENT_SLOWDOWN), "for 5 s");

        health(p, 0.9f);
        Pig calm = mob(h, EntityType.PIG, 0, 1);
        XiaotianQuanManager.onXtqAttacks(incoming(calm, bite, 2f));
        eq(h, 0, amp(calm, MobEffects.MOVEMENT_SLOWDOWN), "out of Wrath: the hound's own, Slowness I");
        eq(h, 40, duration(calm, MobEffects.MOVEMENT_SLOWDOWN), "for 2 s");

        // on top of Hound Training III's Slowness II, Zhàn Shén III lengthens it
        grant(p, "es_xtq_growth_1", "es_xtq_growth_2", "es_xtq_growth_3");
        Pig trained = mob(h, EntityType.PIG, 1, 1);
        XiaotianQuanManager.onXtqAttacks(incoming(trained, bite, 2f));
        eq(h, 1, amp(trained, MobEffects.MOVEMENT_SLOWDOWN), "Hound Training III: Slowness II");
        eq(h, 60, duration(trained, MobEffects.MOVEMENT_SLOWDOWN), "for 3 s");
        health(p, 0.3f);
        Pig both = mob(h, EntityType.PIG, 2, 1);
        XiaotianQuanManager.onXtqAttacks(incoming(both, bite, 2f));
        eq(h, 1, amp(both, MobEffects.MOVEMENT_SLOWDOWN), "both: still Slowness II");
        eq(h, 100, duration(both, MobEffects.MOVEMENT_SLOWDOWN), "but for 5 s");
        h.succeed();
    }
}
