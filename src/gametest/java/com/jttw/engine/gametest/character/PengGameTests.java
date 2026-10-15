package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.peng.PengAbilityHandler;
import com.jttw.engine.character.peng.PengCharacter;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.item.StackData;
import com.jttw.engine.item.YinYangVaseItem;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The Gold-Winged Peng: flight, soaring, wingstrike, feathers, eclipse, and the vase. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class PengGameTests {

    /** The Peng at (2,2,2) facing south (+z), able to fly, with Qi to spend. */
    private static ServerPlayer peng(GameTestHelper h, String... nodes) {
        ServerPlayer p = as(h, ReincarnationOutcome.GOLD_WINGED_PENG);
        p.setYRot(0f); p.setXRot(0f); p.yHeadRot = 0f;
        p.getAbilities().mayfly = true;
        qi(p, 200f, 200f);
        grant(p, nodes);
        return p;
    }

    private static Vec3 rel(GameTestHelper h, double x, double z) {
        return h.absoluteVec(new Vec3(x, 2, z));
    }

    private static ItemStack vase(ServerPlayer p) {
        ItemStack vase = new ItemStack(ModItems.YIN_YANG_VASE.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, vase);
        return vase;
    }

    private static void releaseOn(GameTestHelper h, ServerPlayer p, ItemStack vase, int x, int z) {
        BlockPos floor = h.absolutePos(new BlockPos(x, 1, z));
        vase.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
    }

    @GameTest(template = PLATFORM)
    public static void theOutcomeIsThePengAndHeFliesFromTheStart(GameTestHelper h) {
        ServerPlayer p = peng(h);
        eq(h, PengCharacter.INSTANCE, Characters.of(p), "character");
        check(h, JTTWFlight.canFly(p), "flies, no item, no node");
        check(h, !JTTWFlight.hasUniversalFlight(p), "without touching the flag others grant and revoke");
        near(h, com.jttw.engine.flight.FlightSteering.CRUISE_SPEED, JTTWFlight.profile(p).speed(), "at full-flight speed");

        p.getPersistentData().putString(AlignmentData.KEY_REINCARNATION_OUTCOME, ReincarnationOutcome.CELESTIAL_PRIMATE.id);
        check(h, !JTTWFlight.canFly(p), "the flight goes with the Peng");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void flightGrantedAfterBeingThePengIsKept(GameTestHelper h) {
        // Found by the client play-test: leaving the Peng used to revoke the shared flight flag
        ServerPlayer p = peng(h);
        tick(p, PengAbilityHandler::onPlayerTick, 2);
        p.getPersistentData().putString(AlignmentData.KEY_REINCARNATION_OUTCOME, ReincarnationOutcome.CELESTIAL_PRIMATE.id);
        JTTWFlight.setUniversalFlight(p, true); // Wukong's own flight
        tick(p, PengAbilityHandler::onPlayerTick, 20);
        check(h, JTTWFlight.hasUniversalFlight(p), "Wukong's flight survives the Peng's leaving");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void someoneElsesFlightIsNotTakenAway(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        JTTWFlight.setUniversalFlight(p, true); // granted by Wukong's own tree
        tick(p, PengAbilityHandler::onPlayerTick, 20);
        check(h, JTTWFlight.hasUniversalFlight(p), "still flies");
        h.succeed();
    }

    // ── V: Ninety-Thousand-Li Flight ───────────────────────────────

    @GameTest(template = PLATFORM)
    public static void holdingVTakesOffAndAcceleratesLettingGoSlows(GameTestHelper h) {
        ServerPlayer p = peng(h, "gp_soar_1");
        PengCharacter.INSTANCE.use(p, "gp_soar", AbilityAction.START);
        check(h, p.getAbilities().flying, "takes off");
        qi(p, 1000f, 1000f);
        tick(p, PengAbilityHandler::onPlayerTick, 100);
        near(h, 1.5, PengAbilityHandler.boost(p), "halfway after 5 seconds");
        tick(p, PengAbilityHandler::onPlayerTick, 99);
        check(h, PengAbilityHandler.boost(p) < 2.0, "not quite there before 10: " + PengAbilityHandler.boost(p));
        tick(p, PengAbilityHandler::onPlayerTick, 1);
        near(h, 2.0, PengAbilityHandler.boost(p), "I tops out at 2x after 10 seconds");
        check(h, QiData.getCurrent(p) < 1000f - 199 * PengAbilityHandler.SOAR_QI_PER_TICK + 0.01f, "drains Qi");

        PengCharacter.INSTANCE.use(p, "gp_soar", AbilityAction.STOP);
        tick(p, PengAbilityHandler::onPlayerTick, 20);
        near(h, 1.0, PengAbilityHandler.boost(p), "let go: back to cruising");
        near(h, 3.6, PengAbilityHandler.maxBoost(3), "III tops out at 3.6x");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void soaringStopsWhenQiRunsOut(GameTestHelper h) {
        ServerPlayer p = peng(h, "gp_soar_1");
        qi(p, 2f, 200f);
        PengCharacter.INSTANCE.use(p, "gp_soar", AbilityAction.START);
        tick(p, PengAbilityHandler::onPlayerTick, 20);
        check(h, !p.getPersistentData().getBoolean(PengAbilityHandler.KEY_SOARING), "wings tire without Qi");
        check(h, PengAbilityHandler.boost(p) < 1.3, "and he slows: " + PengAbilityHandler.boost(p));
        h.succeed();
    }

    // ── Wingstrike ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void wingstrikeHitsWhatHeFliesThroughHarderWhenFaster(GameTestHelper h) {
        ServerPlayer p = peng(h, "gp_strike_1");
        p.getAbilities().flying = true;
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 2);
        List<Hit> log = watch(z);

        p.moveTo(rel(h, 2.5, 4.5));
        eq(h, 1, PengAbilityHandler.wingstrike(p, rel(h, 2.5, 0.5)), "flew through the zombie");
        near(h, PengAbilityHandler.wingstrikeDamage((float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), 4.0, 1),
                firstHit(h, log, DamageTypes.PLAYER_ATTACK, z).amount(), "ATK x 4 blocks/tick x 0.5");
        eq(h, 0, PengAbilityHandler.wingstrike(p, rel(h, 2.5, 0.5)), "not twice within half a second");
        near(h, 2 * PengAbilityHandler.wingstrikeDamage(10f, 1.0, 1), PengAbilityHandler.wingstrikeDamage(10f, 2.0, 1),
                "twice as fast, twice as hard");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wingstrikeNeedsFlightAndSpeed(GameTestHelper h) {
        ServerPlayer p = peng(h, "gp_strike_1");
        mob(h, EntityType.ZOMBIE, 2, 2);
        p.moveTo(rel(h, 2.5, 4.5));
        eq(h, 0, PengAbilityHandler.wingstrike(p, rel(h, 2.5, 0.5)), "walking is no wingstrike");
        p.getAbilities().flying = true;
        p.moveTo(rel(h, 2.5, 2.7));
        eq(h, 0, PengAbilityHandler.wingstrike(p, rel(h, 2.5, 2.4)), "hovering is no wingstrike");
        h.succeed();
    }

    // ── B: Feather Storm ───────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void featherStormFansFeathersOnTheGround(GameTestHelper h) {
        ServerPlayer p = peng(h, "gp_feathers_1");
        Snowball[] feathers = PengAbilityHandler.featherStorm(p);
        eq(h, 5, feathers.length, "I: five feathers");
        check(h, feathers[0].getDeltaMovement().x > 0.1 && feathers[4].getDeltaMovement().x < -0.1,
                "fanned left and right: " + feathers[0].getDeltaMovement() + " " + feathers[4].getDeltaMovement());
        eq(h, 0, PengAbilityHandler.featherStorm(p).length, "cooling down");
        eq(h, 9, PengAbilityHandler.featherCount(3), "III: nine");
        for (Snowball f : feathers) f.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aFeatherHurtsAndOnlyThirdTierFeathersPierce(GameTestHelper h) {
        ServerPlayer p = peng(h, "gp_feathers_1", "gp_feathers_2", "gp_feathers_3");
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 4);
        List<Hit> log = watch(z);
        Snowball[] feathers = PengAbilityHandler.featherStorm(p);
        Snowball f = feathers[4];
        PengAbilityHandler.onFeatherLands(new ProjectileImpactEvent(f, new EntityHitResult(z)));
        check(h, !of(log, DamageTypes.THROWN).isEmpty(), "the feather cuts");
        check(h, f.isAlive(), "III: pierces on");
        int hits = log.size();
        PengAbilityHandler.onFeatherLands(new ProjectileImpactEvent(f, new EntityHitResult(z)));
        eq(h, hits, log.size(), "but strikes each only once");
        for (Snowball s : feathers) s.discard();

        ServerPlayer one = peng(h, "gp_feathers_1");
        Snowball[] plain = PengAbilityHandler.featherStorm(one);
        PengAbilityHandler.onFeatherLands(new ProjectileImpactEvent(plain[2], new EntityHitResult(z)));
        check(h, !plain[2].isAlive(), "I: stops in the first it strikes");
        for (Snowball s : plain) s.discard();
        h.succeed();
    }

    // ── N: Eclipse Wings ───────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "pengEclipse") // 16 blocks reaches the next platforms
    public static void eclipseWeakensAndSlowsAllAroundII(GameTestHelper h) {
        ServerPlayer p = peng(h, "gp_eclipse_1", "gp_eclipse_2");
        Zombie front = mob(h, EntityType.ZOMBIE, 2, 4);
        Zombie behind = mob(h, EntityType.ZOMBIE, 2, 0);
        front.setTarget(p);
        eq(h, 2, PengAbilityHandler.eclipse(p), "everything around, front and back");
        for (Zombie z : List.of(front, behind)) {
            eq(h, 1, amp(z, MobEffects.WEAKNESS), "weakened");
            eq(h, 1, amp(z, MobEffects.MOVEMENT_SLOWDOWN), "slowed");
            check(h, com.jttw.engine.world.PrivateGlow.glowsFor(p, z) && !z.hasEffect(MobEffects.GLOWING), "II: glows for him alone");
        }
        check(h, front.getTarget() == null, "II: loses its prey");
        near(h, 10.0, PengAbilityHandler.eclipseRadius(1), "I reach");
        near(h, 16.0, PengAbilityHandler.eclipseRadius(2), "II reach");
        eq(h, -1, PengAbilityHandler.eclipse(p), "cooling down");
        h.succeed();
    }

    // ── Yin-Yang Two-Qi Vase ───────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void anyoneSealsACreatureAndLetsItOut(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        ItemStack vase = vase(p);
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        pig.setHealth(7f);
        eq(h, InteractionResult.CONSUME, vase.interactLivingEntity(p, pig, InteractionHand.MAIN_HAND), "sealed");
        check(h, pig.isRemoved(), "the pig is in the vase");
        check(h, YinYangVaseItem.holds(vase), "and the vase holds it");
        eq(h, InteractionResult.FAIL, vase.interactLivingEntity(p, mob(h, EntityType.PIG, 1, 4), InteractionHand.MAIN_HAND),
                "one at a time");

        releaseOn(h, p, vase, 3, 3);
        check(h, !YinYangVaseItem.holds(vase), "empty again");
        List<Pig> out = near(p, Pig.class, 4).stream().filter(x -> x.getUUID().equals(pig.getUUID())).toList();
        eq(h, 1, out.size(), "the same pig is out");
        near(h, 7f, out.get(0).getHealth(), "unharmed by anyone else's vase");
        eq(h, h.absolutePos(new BlockPos(3, 2, 3)), out.get(0).blockPosition(), "where the vase was emptied");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void noOneButThePengCanHoldAPlayer(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        ServerPlayer other = player(h);
        eq(h, InteractionResult.FAIL, vase(p).interactLivingEntity(p, other, InteractionHand.MAIN_HAND), "too great");
        check(h, !PengAbilityHandler.isJailed(other), "not held");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void whatThePengSealsDissolvesAndBurstsOut(GameTestHelper h) {
        ServerPlayer p = peng(h);
        ItemStack vase = vase(p);
        Zombie sealed = mob(h, EntityType.ZOMBIE, 2, 4);
        vase.interactLivingEntity(p, sealed, InteractionHand.MAIN_HAND);
        // five seconds inside: 5% of 20 health a second, 5 health gone
        StackData.putLong(vase, YinYangVaseItem.TAG_SEALED_AT, p.level().getGameTime() - 100);
        Zombie bystander = mob(h, EntityType.ZOMBIE, 4, 3);
        List<Hit> log = watch(bystander);
        releaseOn(h, p, vase, 3, 3);
        Zombie out = near(p, Zombie.class, 5).stream().filter(x -> x.getUUID().equals(sealed.getUUID())).findFirst().orElse(null);
        check(h, out != null, "released");
        check(h, out.getHealth() <= 15f, "dissolved inside: " + out.getHealth());
        check(h, !of(log, DamageTypes.INDIRECT_MAGIC).isEmpty(), "and the release bursts on those near");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void leftLongEnoughInThePengsVaseItIsGone(GameTestHelper h) {
        ServerPlayer p = peng(h);
        ItemStack vase = vase(p);
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        vase.interactLivingEntity(p, pig, InteractionHand.MAIN_HAND);
        StackData.putLong(vase, YinYangVaseItem.TAG_SEALED_AT, p.level().getGameTime() - 400); // 20 s: all of it
        releaseOn(h, p, vase, 3, 3);
        check(h, near(p, Pig.class, 5).stream().noneMatch(x -> x.getUUID().equals(pig.getUUID())), "dissolved away");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thePengHoldsAPlayerInPlaceUnableToFight(GameTestHelper h) {
        ServerPlayer p = peng(h);
        online(h, p);
        ServerPlayer held = player(h);
        held.moveTo(rel(h, 2.5, 4.5));
        eq(h, InteractionResult.CONSUME, vase(p).interactLivingEntity(p, held, InteractionHand.MAIN_HAND), "caught");
        check(h, PengAbilityHandler.isJailed(held), "held");
        held.moveTo(rel(h, 0.5, 0.5));
        PengAbilityHandler.onLivingTick(new EntityTickEvent.Pre(held));
        near(h, rel(h, 2.5, 4.5).z, held.getZ(), "pulled back where caught");
        AttackEntityEvent swing = new AttackEntityEvent(held, p);
        PengAbilityHandler.onJailedAttack(swing);
        check(h, swing.isCanceled(), "cannot fight back");
        check(h, p.getCooldowns().isOnCooldown(ModItems.YIN_YANG_VASE.get()), "the vase rests after");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heldThingsDissolveThenBurstFree(GameTestHelper h) {
        ServerPlayer p = peng(h);
        online(h, p);
        Zombie held = mob(h, EntityType.ZOMBIE, 2, 4);
        Zombie beside = mob(h, EntityType.ZOMBIE, 3, 4);
        List<Hit> heldLog = watch(held), besideLog = watch(beside);
        PengAbilityHandler.jail(p, held);
        PengAbilityHandler.onLivingTick(new EntityTickEvent.Pre(held)); // on the second
        check(h, !of(heldLog, DamageTypes.INDIRECT_MAGIC).isEmpty(), "dissolving");
        held.getPersistentData().putLong(PengAbilityHandler.KEY_JAIL_UNTIL, held.level().getGameTime());
        PengAbilityHandler.onLivingTick(new EntityTickEvent.Pre(held));
        check(h, !PengAbilityHandler.isJailed(held), "free after five seconds");
        check(h, !of(besideLog, DamageTypes.INDIRECT_MAGIC).isEmpty(), "bursting on those beside");
        h.succeed();
    }
}
