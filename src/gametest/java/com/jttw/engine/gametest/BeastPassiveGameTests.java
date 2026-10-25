package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.divinebeast.BeastPassives;
import com.jttw.engine.entity.zone.MobLevel;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.AbilityUnlock;
import com.jttw.engine.progression.skilltree.RetiredUnlocks;
import com.jttw.engine.progression.skilltree.SkillPurchase;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.PrivateGlow;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;
import static com.jttw.engine.gametest.character.CharTest.mob;
import static com.jttw.engine.gametest.character.CharTest.tick;

/**
 * The Divine Beast's class passives: bought at their levels, and what each does; its forms as
 * free milestones; and the points given back for class abilities since retired.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class BeastPassiveGameTests {

    /** {@code who}, reincarnated and levelled to {@code level} through XP, as a player gets there. */
    private static ServerPlayer at(GameTestHelper h, ReincarnationOutcome who, int level) {
        ServerPlayer p = as(h, who);
        ProgressionClass cls = PlayerLevel.getProgressionClass(p);
        if (level > 1) LevelManager.grantXp(p, cls.growthRate.totalXpForLevel(level), "test");
        return p;
    }

    /** Further XP, to just reach {@code level}. */
    private static void levelTo(ServerPlayer p, int level) {
        long total = PlayerLevel.getProgressionClass(p).growthRate.totalXpForLevel(level);
        LevelManager.grantXp(p, total - PlayerLevel.getXp(p), "test");
    }

    /** A Divine Beast at {@code level}, with these passives bought (points enough given first). */
    private static ServerPlayer beast(GameTestHelper h, int level, String... passives) {
        ServerPlayer p = at(h, ReincarnationOutcome.DRAGON, level);
        SkillTreeData.setPoints(p, 20);
        for (String id : passives) check(h, SkillPurchase.purchaseClassAbility(p, id), "buy " + id);
        return p;
    }

    /** One second of the beast's passives. */
    private static void second(ServerPlayer p) {
        tick(p, BeastPassives::onPlayerTick, BeastPassives.INTERVAL);
    }

    private static boolean held(LivingEntity e) {
        return e.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && e.hasEffect(MobEffects.WEAKNESS);
    }

    private static boolean untouched(LivingEntity e) {
        return !e.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !e.hasEffect(MobEffects.WEAKNESS);
    }

    // ── Buying ──────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void formsAreMilestonesNotBought(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.PHOENIX, 80);
        SkillTreeData.setPoints(p, 10);
        for (String id : List.of(AbilityUnlock.LESSER_FORM, AbilityUnlock.INTERMEDIATE_FORM, AbilityUnlock.SOVEREIGN_FORM)) {
            check(h, !SkillPurchase.purchaseClassAbility(p, id), id + " cannot be bought");
            check(h, !PlayerLevel.hasAbility(p, id), id + " not held as a bought ability");
            check(h, AbilityUnlock.byId(id).reachedAt(PlayerLevel.getLevel(p)), id + " reached at level 80");
        }
        eq(h, 10, SkillTreeData.getPoints(p), "nothing spent");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eachPassiveIsBoughtAtItsLevel(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.WHITE_TIGER, 19);
        SkillTreeData.setPoints(p, 20);
        check(h, !SkillPurchase.purchaseClassAbility(p, AbilityUnlock.DOMAIN_SENSE), "not at 19");
        levelTo(p, 29);
        SkillTreeData.setPoints(p, 20);
        check(h, SkillPurchase.purchaseClassAbility(p, AbilityUnlock.DOMAIN_SENSE), "Domain Sense at 20+");
        check(h, !SkillPurchase.purchaseClassAbility(p, AbilityUnlock.GUARDIANS_HIDE), "Guardian's Hide not at 29");
        levelTo(p, 100);
        eq(h, 100, PlayerLevel.getLevel(p), "level");
        SkillTreeData.setPoints(p, 20);
        check(h, SkillPurchase.purchaseClassAbility(p, AbilityUnlock.GUARDIANS_HIDE), "Guardian's Hide");
        check(h, !SkillPurchase.purchaseClassAbility(p, AbilityUnlock.APEX_DOMAIN), "Apex Domain needs Domain Authority first");
        eq(h, 19, SkillTreeData.getPoints(p), "nothing spent on the refusal");
        check(h, SkillPurchase.purchaseClassAbility(p, AbilityUnlock.DOMAIN_AUTHORITY), "Domain Authority");
        eq(h, 17, SkillTreeData.getPoints(p), "it costs 2");
        check(h, SkillPurchase.purchaseClassAbility(p, AbilityUnlock.APEX_DOMAIN), "then Apex Domain");
        eq(h, 14, SkillTreeData.getPoints(p), "it costs 3");
        check(h, !SkillPurchase.purchaseClassAbility(p, AbilityUnlock.APEX_DOMAIN), "not twice");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aNonBeastCannotBuyThem(GameTestHelper h) {
        for (ReincarnationOutcome who : List.of(ReincarnationOutcome.EYE_OF_HEAVEN,
                ReincarnationOutcome.BULL_DEMON_KING, ReincarnationOutcome.YUAN_DI)) {
            ServerPlayer p = at(h, who, 100);
            SkillTreeData.setPoints(p, 20);
            for (String id : List.of(AbilityUnlock.DOMAIN_SENSE, AbilityUnlock.GUARDIANS_HIDE,
                    AbilityUnlock.DOMAIN_AUTHORITY, AbilityUnlock.APEX_DOMAIN)) {
                check(h, !SkillPurchase.purchaseClassAbility(p, id), who + " cannot buy " + id);
            }
            eq(h, 20, SkillTreeData.getPoints(p), who + ": nothing spent");
            // and a passive somehow held does nothing for one of another class
            PlayerLevel.grantAbility(p, AbilityUnlock.DOMAIN_AUTHORITY);
            eq(h, 0.0, BeastPassives.domainRadius(p), who + ": no domain");
        }
        h.succeed();
    }

    // ── Guardian's Hide ─────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void guardiansHideAddsATenthToBothResistances(GameTestHelper h) {
        ServerPlayer p = beast(h, 30);
        second(p);
        double pres = pres(p), mres = mres(p);
        check(h, pres > 0 && mres > 0, "the level's resistances: " + pres + ", " + mres);
        check(h, SkillPurchase.purchaseClassAbility(p, AbilityUnlock.GUARDIANS_HIDE), "bought");
        second(p);
        near(h, pres * 1.1, pres(p), "physical resistance +10%");
        near(h, mres * 1.1, mres(p), "magic resistance +10%");
        second(p);
        near(h, pres * 1.1, pres(p), "refreshed, not stacked");
        // given up (another class's): gone on the next refresh
        PlayerLevel.setProgressionClass(p, ProgressionClass.YAOGUAI);
        second(p);
        near(h, pres, pres(p), "gone with the class");
        h.succeed();
    }

    // ── Domain Authority, Apex Domain ───────────────────────────

    @GameTest(template = PLATFORM)
    public static void domainAuthorityHoldsLesserHostilesNear(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = beast(h, 60, AbilityUnlock.DOMAIN_AUTHORITY);
        Zombie zombie = mob(h, EntityType.ZOMBIE, 4, 2);                       // vanilla: no level, so lower
        Mob servant = mob(h, ModEntities.BONE_SERVANT.get(), 2, 4);            // a level-20 zone creature
        Mob imp = mob(h, ModEntities.EMBER_IMP.get(), 0, 2);                   // a level-67 one: above him
        Cow cow = mob(h, EntityType.COW, 2, 0);                                // not hostile
        Zombie far = mob(h, EntityType.ZOMBIE, 2, 13);                         // 10.5 blocks off
        eq(h, 20, MobLevel.of(servant), "the servant's level");
        check(h, MobLevel.of(imp) > 60, "the imp's level: " + MobLevel.of(imp));
        eq(h, 0, MobLevel.of(zombie), "a vanilla mob has none");
        ServerPlayer other = player(h);                                        // another player, beside him
        other.moveTo(p.getX() + 1, p.getY(), p.getZ());
        second(p);
        check(h, held(zombie), "the zombie is slowed and weakened");
        eq(h, 0, zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier(), "Slowness I");
        eq(h, 0, zombie.getEffect(MobEffects.WEAKNESS).getAmplifier(), "Weakness I");
        check(h, zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() <= BeastPassives.HOLD_TICKS, "for a moment");
        check(h, held(servant), "and the lesser zone creature");
        check(h, untouched(imp), "not the one above his level");
        check(h, untouched(cow), "not the cow");
        check(h, untouched(far), "not one out of range");
        check(h, untouched(other), "not another player");
        check(h, !BeastPassives.cowed(zombie), "no Apex Domain: its blows are its own");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void domainAuthorityLeavesTamedCreaturesAlone(GameTestHelper h) {
        ServerPlayer p = beast(h, 60, AbilityUnlock.DOMAIN_AUTHORITY);
        var wolf = mob(h, EntityType.WOLF, 4, 2);
        wolf.tame(player(h));
        wolf.setTarget(p); // even turned on him, it is someone's
        second(p);
        check(h, untouched(wolf), "a tamed wolf is not held");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void apexDomainReachesSixteenAndCutsTheirBlows(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = beast(h, 100, AbilityUnlock.DOMAIN_AUTHORITY, AbilityUnlock.APEX_DOMAIN);
        eq(h, 16.0, BeastPassives.domainRadius(p), "the domain's reach");
        Zombie near = mob(h, EntityType.ZOMBIE, 4, 2);
        Zombie wide = mob(h, EntityType.ZOMBIE, 2, 14);  // 11.5 blocks: past Domain Authority's 8
        Zombie outside = mob(h, EntityType.ZOMBIE, 14, 14); // 17 blocks off, on the diagonal
        Cow cow = mob(h, EntityType.COW, 0, 2);
        second(p);
        check(h, held(near), "near: held");
        check(h, held(wide), "at 11.5 blocks: held");
        check(h, untouched(outside), "at 17 blocks: not");
        check(h, BeastPassives.cowed(near) && BeastPassives.cowed(wide), "both cowed");
        check(h, !BeastPassives.cowed(outside), "the one outside not");

        // its blows, at anyone: a fifth less
        var hit = incoming(cow, h.getLevel().damageSources().mobAttack(wide), 10f);
        BeastPassives.onIncomingDamage(hit);
        near(h, 8.0, hit.getAmount(), "a held zombie strikes the cow 20% softer");
        var free = incoming(cow, h.getLevel().damageSources().mobAttack(outside), 10f);
        BeastPassives.onIncomingDamage(free);
        near(h, 10.0, free.getAmount(), "one outside the domain strikes in full");
        h.succeed();
    }

    // ── Domain Sense ────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 260)
    public static void domainSenseShowsWhoHuntsYouToYouAlone(GameTestHelper h) {
        ServerPlayer p = beast(h, 20, AbilityUnlock.DOMAIN_SENSE);
        ServerPlayer other = beast(h, 20, AbilityUnlock.DOMAIN_SENSE);
        Zombie hunter = mob(h, EntityType.ZOMBIE, 4, 2);
        Zombie idle = mob(h, EntityType.ZOMBIE, 0, 2);
        hunter.setTarget(p);
        second(p);
        second(other);
        check(h, PrivateGlow.glowsFor(p, hunter), "the zombie hunting him glows for him");
        check(h, !PrivateGlow.glowsFor(other, hunter), "not for another (it hunts not them)");
        check(h, !PrivateGlow.glowsFor(p, idle), "not one hunting no one");
        check(h, !hunter.hasGlowingTag() && !hunter.isCurrentlyGlowing(), "no glow for everyone");
        // kept glowing while it hunts: past the 3 s of one showing
        h.runAfterDelay(40, () -> second(p));
        h.runAfterDelay(80, () -> {
            check(h, PrivateGlow.glowsFor(p, hunter), "still glowing at 4 s: renewed while it hunts");
            hunter.setTarget(null);
            second(p);
        });
        h.runAfterDelay(80 + BeastPassives.SENSE_GLOW_TICKS + 5, () -> {
            check(h, !PrivateGlow.glowsFor(p, hunter), "gone once it gave up the hunt");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void withoutDomainSenseNothingGlows(GameTestHelper h) {
        ServerPlayer p = beast(h, 20);
        Zombie hunter = mob(h, EntityType.ZOMBIE, 4, 2);
        hunter.setTarget(p);
        second(p);
        check(h, !PrivateGlow.glowsFor(p, hunter), "not bought, nothing shown");
        h.succeed();
    }

    // ── Retired class abilities ─────────────────────────────────

    /** Writes ids into the player's saved abilities, as a save from before would hold them. */
    private static void saved(ServerPlayer p, String... ids) {
        CompoundTag abilities = p.getPersistentData().getCompound("jttw_abilities");
        for (String id : ids) abilities.putBoolean(id, true);
        p.getPersistentData().put("jttw_abilities", abilities);
    }

    @GameTest(template = PLATFORM)
    public static void retiredAbilitiesArePaidBackOnceAtLogin(GameTestHelper h) {
        ServerPlayer bull = at(h, ReincarnationOutcome.BULL_DEMON_KING, 60);
        saved(bull, "demon_instinct", "first_spike", "second_spike"); // 1 + 1 + 2 bought
        SkillTreeData.setPoints(bull, 8);
        List<String> heard = new ArrayList<>();
        listen(bull, heard);
        LevelManager.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(bull));
        eq(h, 12, SkillTreeData.getPoints(bull), "four points back");
        for (String id : List.of("demon_instinct", "first_spike", "second_spike")) {
            check(h, !bull.getPersistentData().getCompound("jttw_abilities").contains(id), id + " cleared");
        }
        check(h, heard.stream().anyMatch(m -> m.contains("Demon Instinct") && m.contains("Power Spike II")
                && m.contains("4 skill points")), "told what was returned: " + heard);
        heard.clear();
        LevelManager.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(bull));
        eq(h, 12, SkillTreeData.getPoints(bull), "nothing more the second time");
        check(h, heard.stream().noneMatch(m -> m.contains("Retired")), "and nothing said: " + heard);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theHeavenlyKeepsItsCloudAndLosesTheRest(GameTestHelper h) {
        ServerPlayer erlang = at(h, ReincarnationOutcome.EYE_OF_HEAVEN, 80);
        saved(erlang, JTTWFlight.HEAVENLY_MOBILITY, "eye_of_heaven", "divine_perception");
        SkillTreeData.setPoints(erlang, 0);
        eq(h, 3, RetiredUnlocks.refund(erlang), "2 for the Eye of Heaven, 1 for Divine Perception");
        eq(h, 3, SkillTreeData.getPoints(erlang), "given back");
        check(h, PlayerLevel.hasAbility(erlang, JTTWFlight.HEAVENLY_MOBILITY), "the cloud is kept");
        check(h, JTTWFlight.hasHeavenlyCloud(erlang), "and still carries him");
        check(h, !PlayerLevel.hasAbility(erlang, "eye_of_heaven"), "the rest cleared");
        eq(h, 0, RetiredUnlocks.refund(erlang), "once only");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBeastPaidForItsFormsGetsThePointsBack(GameTestHelper h) {
        ServerPlayer p = at(h, ReincarnationOutcome.TURTLE, 80);
        saved(p, AbilityUnlock.LESSER_FORM, AbilityUnlock.INTERMEDIATE_FORM, AbilityUnlock.SOVEREIGN_FORM,
                AbilityUnlock.DOMAIN_SENSE);
        SkillTreeData.setPoints(p, 0);
        eq(h, 4, RetiredUnlocks.refund(p), "1 + 1 + 2 for the three forms");
        check(h, PlayerLevel.hasAbility(p, AbilityUnlock.DOMAIN_SENSE), "a passive still on the layer is kept");
        check(h, !PlayerLevel.hasAbility(p, AbilityUnlock.LESSER_FORM), "the forms are no longer held");
        check(h, AbilityUnlock.byId(AbilityUnlock.SOVEREIGN_FORM).reachedAt(PlayerLevel.getLevel(p)), "but reached, free");
        h.succeed();
    }
}
