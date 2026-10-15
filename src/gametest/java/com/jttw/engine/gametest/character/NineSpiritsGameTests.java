package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.ninespirits.NineSpiritsAbilityHandler;
import com.jttw.engine.character.ninespirits.NineSpiritsCharacter;
import com.jttw.engine.item.BellOfTaiyiItem;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The Grand Saint of Nine Spirits: his heads, roar, mane, true form, nine lives and bell. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class NineSpiritsGameTests {

    /** The Grand Saint at (2,2,2) facing south (+z), with Qi to spend. */
    private static ServerPlayer saint(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS);
        p.setYRot(0f); p.setXRot(0f); p.yHeadRot = 0f;
        qi(p, 200f, 200f);
        return p;
    }

    private static void key(ServerPlayer p, int slot) {
        NineSpiritsCharacter.INSTANCE.activate(p, slot, AbilityAction.USE);
    }

    private static void ready(ServerPlayer p) {
        for (String k : List.of("jttw_ns_heads_ready_at", "jttw_ns_roar_ready_at", "jttw_ns_flare_ready_at")) {
            p.getPersistentData().remove(k);
        }
    }

    @GameTest(template = PLATFORM)
    public static void theOutcomeIsTheGrandSaint(GameTestHelper h) {
        ServerPlayer p = saint(h);
        eq(h, NineSpiritsCharacter.INSTANCE, Characters.of(p), "character");
        check(h, NineSpiritsCharacter.is(p), "is()");
        eq(h, 4L, NineSpiritsCharacter.INSTANCE.skillBar(java.util.Set.of("ns_heads_1", "ns_roar_1", "ns_flare_1", "ns_form_1"))
                .stream().filter(s -> !s.isEmpty()).count(), "four keys once unlocked");
        h.succeed();
    }

    // ── V: heads ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void eachTierWakesAnotherHeadAndSneakVChangesTheLeader(GameTestHelper h) {
        ServerPlayer p = saint(h);
        key(p, 0);
        eq(h, 200f, QiData.getCurrent(p), "no node: nothing fires");
        grant(p, "ns_heads_1");
        p.setShiftKeyDown(true);
        key(p, 0);
        eq(h, NineSpiritsAbilityHandler.FIRE, NineSpiritsAbilityHandler.leadHead(p), "one head: fire stays");
        grant(p, "ns_heads_2");
        key(p, 0);
        eq(h, NineSpiritsAbilityHandler.THUNDER, NineSpiritsAbilityHandler.leadHead(p), "thunder leads");
        key(p, 0);
        eq(h, NineSpiritsAbilityHandler.FIRE, NineSpiritsAbilityHandler.leadHead(p), "two heads: back to fire");
        grant(p, "ns_heads_3");
        key(p, 0);
        key(p, 0);
        eq(h, NineSpiritsAbilityHandler.FROST, NineSpiritsAbilityHandler.leadHead(p), "frost leads at III");
        eq(h, 200f, QiData.getCurrent(p), "changing heads is free");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFireHeadsFireballBurstsAndSetsFoesAlight(GameTestHelper h) {
        ServerPlayer p = saint(h);
        grant(p, "ns_heads_1");
        float atk = (float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        key(p, 0);
        near(h, 200 - NineSpiritsAbilityHandler.HEADS_QI, QiData.getCurrent(p), "6 Qi");
        Snowball shot = near(p, Snowball.class, 4).stream().filter(s -> s.getOwner() == p).findFirst().orElse(null);
        check(h, shot != null && shot.getPersistentData().getInt(NineSpiritsAbilityHandler.TAG_ELEMENT) == 0, "a fire shot flies");
        check(h, shot.isNoGravity(), "straight, not lobbed");
        near(h, NineSpiritsAbilityHandler.headDamage(atk, 1), shot.getPersistentData().getFloat(NineSpiritsAbilityHandler.TAG_DAMAGE), "carries ATK x1.0");
        key(p, 0);
        eq(h, 1L, near(p, Snowball.class, 4).stream().filter(s -> s.getOwner() == p).count(), "1 s cooldown");

        Pig hit = mob(h, EntityType.PIG, 2, 5);
        Pig beside = mob(h, EntityType.PIG, 3, 5);
        Pig far = mob(h, EntityType.PIG, 2, 0);
        List<Hit> hitLog = watch(hit), besideLog = watch(beside), farLog = watch(far);
        NineSpiritsAbilityHandler.onHeadShotLands(new ProjectileImpactEvent(shot, new EntityHitResult(hit)));
        check(h, shot.isRemoved(), "the fireball is spent");
        eq(h, 1, of(hitLog, DamageTypes.INDIRECT_MAGIC).size(), "the struck pig burns");
        eq(h, 1, of(besideLog, DamageTypes.INDIRECT_MAGIC).size(), "the burst catches its neighbour");
        eq(h, 0, of(farLog, DamageTypes.INDIRECT_MAGIC).size(), "but not one far off");
        check(h, hit.isOnFire() && beside.isOnFire(), "set alight");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFrostHeadsShardSlowsAndFreezes(GameTestHelper h) {
        ServerPlayer p = saint(h);
        grant(p, "ns_heads_1", "ns_heads_2", "ns_heads_3");
        p.getPersistentData().putInt(NineSpiritsAbilityHandler.KEY_HEAD, NineSpiritsAbilityHandler.FROST);
        key(p, 0);
        Snowball shot = near(p, Snowball.class, 4).stream().filter(s -> s.getOwner() == p).findFirst().orElseThrow();
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 5);
        NineSpiritsAbilityHandler.onHeadShotLands(new ProjectileImpactEvent(shot, new EntityHitResult(z)));
        check(h, z.getHealth() < z.getMaxHealth(), "hurt");
        eq(h, 9, amp(z, MobEffects.MOVEMENT_SLOWDOWN), "frozen in place");
        check(h, z.isFullyFrozen(), "rimed with frost");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theThunderHeadsBoltJumpsToTheNearestFoes(GameTestHelper h) {
        ServerPlayer p = saint(h);
        Zombie first = mob(h, EntityType.ZOMBIE, 2, 5);
        Zombie second = mob(h, EntityType.ZOMBIE, 4, 5);
        Zombie third = mob(h, EntityType.ZOMBIE, 0, 5);
        Zombie fourth = mob(h, EntityType.ZOMBIE, 4, 3);
        List<LivingEntity> struck = NineSpiritsAbilityHandler.thunderBolt(p, 10f, 2);
        eq(h, first, struck.isEmpty() ? null : struck.get(0), "the bolt strikes what is under the crosshair first");
        eq(h, 3, struck.size(), "then jumps twice");
        check(h, first.getHealth() < second.getHealth() || struck.indexOf(second) > 0, "weaker with each jump");
        // four jumps reach all four (a neighbouring test's creature in reach may be struck too)
        List<LivingEntity> far = NineSpiritsAbilityHandler.thunderBolt(p, 10f, 4);
        check(h, far.containsAll(List.of(first, second, third, fourth)), "four jumps reach all four: " + far.size() + " struck");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void bellRungFoesTakeMoreFromTheHeads(GameTestHelper h) {
        ServerPlayer p = saint(h);
        Zombie plain = mob(h, EntityType.ZOMBIE, 2, 5);
        List<Hit> plainLog = watch(plain);
        NineSpiritsAbilityHandler.thunderBolt(p, 10f, 0);
        plain.discard();
        Zombie rung = mob(h, EntityType.ZOMBIE, 2, 5);
        List<Hit> rungLog = watch(rung);
        rung.getPersistentData().putLong(NineSpiritsAbilityHandler.KEY_BELL_MARK, h.getLevel().getGameTime() + 60);
        NineSpiritsAbilityHandler.thunderBolt(p, 10f, 0);
        near(h, 10.0, firstHit(h, plainLog, DamageTypes.INDIRECT_MAGIC, plain).amount(), "plain");
        near(h, 13.0, firstHit(h, rungLog, DamageTypes.INDIRECT_MAGIC, rung).amount(), "rung: x1.3");
        h.succeed();
    }

    // ── B: roar ────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "nineSpiritsRoar")
    public static void theRoarDrivesBackWhatIsInFrontOnly(GameTestHelper h) {
        ServerPlayer p = saint(h);
        grant(p, "ns_roar_1");
        Zombie front = mob(h, EntityType.ZOMBIE, 2, 5);
        Zombie behind = mob(h, EntityType.ZOMBIE, 2, 0);
        key(p, 1);
        near(h, 200 - NineSpiritsAbilityHandler.ROAR_QI, QiData.getCurrent(p), "15 Qi");
        eq(h, 1, amp(front, MobEffects.MOVEMENT_SLOWDOWN), "slowed");
        check(h, front.getDeltaMovement().z > 1.0, "thrown back, away from the roar");
        eq(h, -1, amp(behind, MobEffects.MOVEMENT_SLOWDOWN), "behind it, untouched");
        eq(h, -1, amp(front, MobEffects.CONFUSION), "no dizziness at I");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "nineSpiritsRoar3")
    public static void theFullRoarDizziesGroundsFlyersAndShattersShots(GameTestHelper h) {
        ServerPlayer p = saint(h);
        grant(p, "ns_roar_1", "ns_roar_2", "ns_roar_3");
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 5);
        var phantom = h.spawnWithNoFreeWill(EntityType.PHANTOM, new BlockPos(2, 4, 6));
        phantom.setOnGround(false);
        Arrow arrow = new Arrow(h.getLevel(), h.absoluteVec(new Vec3(2.5, 3, 6)).x, h.absoluteVec(new Vec3(2.5, 3, 6)).y,
                h.absoluteVec(new Vec3(2.5, 3, 6)).z);
        h.getLevel().addFreshEntity(arrow);
        key(p, 1);
        eq(h, 0, amp(z, MobEffects.CONFUSION), "dizzy");
        eq(h, 9, amp(z, MobEffects.WEAKNESS), "cannot fight back");
        check(h, phantom.getDeltaMovement().y < -1.0, "the phantom is driven down: " + phantom.getDeltaMovement());
        check(h, arrow.isRemoved(), "the arrow shatters");
        h.succeed();
    }

    // ── N: mane flare ──────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "nineSpiritsFlare")
    public static void theManeBlindsOnlyThoseLookingAtHim(GameTestHelper h) {
        ServerPlayer p = saint(h);
        grant(p, "ns_flare_1");
        Zombie watching = mob(h, EntityType.ZOMBIE, 2, 5);
        watching.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        watching.setYHeadRot(watching.getYRot());
        Zombie away = mob(h, EntityType.ZOMBIE, 0, 5);
        away.lookAt(EntityAnchorArgument.Anchor.EYES, away.getEyePosition().add(0, 0, 5));
        away.setYHeadRot(away.getYRot());
        key(p, 2);
        check(h, watching.hasEffect(MobEffects.BLINDNESS), "the one looking is blinded");
        check(h, !away.hasEffect(MobEffects.BLINDNESS), "the one looking away is spared");
        h.succeed();
    }

    // ── M: true form ───────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theTrueFormHitsWithEveryHeadAndDrainsQi(GameTestHelper h) {
        ServerPlayer p = saint(h);
        grant(p, "ns_form_1", "ns_form_2");
        key(p, 3);
        check(h, NineSpiritsAbilityHandler.inTrueForm(p), "in true form");
        eq(h, 30.0, modifier(p, PHYSICAL_RESISTANCE(), "ns_form_pres"), "tougher (II)");
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        LivingIncomingDamageEvent hit = incoming(pig, p.damageSources().playerAttack(p), 10f);
        NineSpiritsAbilityHandler.onHit(hit);
        near(h, 20.0, hit.getAmount(), "three heads: 10 x 2.0");
        tick(p, NineSpiritsAbilityHandler::onPlayerTick, 20);
        near(h, 200 - 20 * NineSpiritsAbilityHandler.FORM_QI_PER_TICK, QiData.getCurrent(p), "drains Qi");
        QiData.setCurrent(p, 0.1f);
        tick(p, NineSpiritsAbilityHandler::onPlayerTick, 1);
        check(h, !NineSpiritsAbilityHandler.inTrueForm(p), "ends without Qi");
        eq(h, null, modifier(p, PHYSICAL_RESISTANCE(), "ns_form_pres"), "and its toughness goes");
        h.succeed();
    }

    // ── nine lives ─────────────────────────────────────────────────

    private static LivingDeathEvent killed(ServerPlayer p) {
        LivingDeathEvent e = new LivingDeathEvent(p, p.damageSources().generic());
        NineSpiritsAbilityHandler.onDeath(e);
        return e;
    }

    @GameTest(template = PLATFORM)
    public static void nineLivesSurviveKillingBlowsThenComeBack(GameTestHelper h) {
        ServerPlayer p = saint(h);
        check(h, !killed(p).isCanceled(), "no lives without the node");
        grant(p, "ns_lives_1", "ns_lives_2");
        eq(h, 2, NineSpiritsAbilityHandler.livesLeft(p), "two lives");
        check(h, killed(p).isCanceled(), "first death cheated");
        near(h, NineSpiritsAbilityHandler.LIFE_SAVED_HEALTH, p.getHealth(), "back at 4 hearts");
        PlayerLevel.setLevel(p, 20);
        LevelManager.reapplyStats(p);
        check(h, killed(p).isCanceled(), "second death cheated");
        near(h, NineSpiritsAbilityHandler.LIFE_SAVED_HEALTH * LevelPower.of(p), p.getHealth(),
                "at level 20, 4 hearts at his power");
        check(h, !killed(p).isCanceled(), "the third is real");
        LivingDeathEvent kill = new LivingDeathEvent(p, p.damageSources().genericKill());
        NineSpiritsAbilityHandler.onDeath(kill);
        check(h, !kill.isCanceled(), "/kill is never cheated");
        p.getPersistentData().putLong(NineSpiritsAbilityHandler.KEY_LIFE_BACK_AT, h.getLevel().getGameTime());
        tick(p, NineSpiritsAbilityHandler::onPlayerTick, 1);
        eq(h, 1, NineSpiritsAbilityHandler.livesLeft(p), "a life comes back");
        h.succeed();
    }

    // ── the bell ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "bellOfTaiyi")
    public static void theBellFreezesNearbyFoesAndRingsFarForTheSaint(GameTestHelper h) {
        ServerPlayer anyone = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        Zombie near = mob(h, EntityType.ZOMBIE, 2, 4);
        eq(h, 1, BellOfTaiyiItem.ring(anyone), "one in reach");
        eq(h, 9, amp(near, MobEffects.MOVEMENT_SLOWDOWN), "held still");
        eq(h, 0L, near.getPersistentData().getLong(NineSpiritsAbilityHandler.KEY_BELL_MARK), "no mark from anyone else");

        ServerPlayer p = saint(h);
        BellOfTaiyiItem.ring(p);
        check(h, near.getPersistentData().getLong(NineSpiritsAbilityHandler.KEY_BELL_MARK) > h.getLevel().getGameTime(),
                "the Saint's bell marks them for his heads");
        h.succeed();
    }
}
