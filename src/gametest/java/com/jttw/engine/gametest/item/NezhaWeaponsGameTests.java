package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.QiankunRingItem;
import com.jttw.engine.item.YinYangSwordItem;
import com.jttw.engine.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.item.ItemTestSupport.*;

/** Nezha's Fire-Tipped Spear, Qiankun Ring and Yin-Yang Swords. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
// Line- and area-targeted attacks run in their own batch: in the shared batch a
// neighbouring test's mob can be first in the line.
public class NezhaWeaponsGameTests {

    private static ItemStack fireSpear() {
        return new ItemStack(ModItems.FIRE_TIP_SPEAR.get());
    }

    private static ItemStack ring() {
        return new ItemStack(ModItems.QIANKUN_RING.get());
    }

    private static double atkWith(GameTestHelper h, ItemStack stack, EquipmentSlot slot) {
        ServerPlayer p = player(h);
        setBaseAtk(p, 10.0);
        applyEquipmentModifiers(p, stack, slot);
        return p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    // ── Fire-Tipped Spear ─────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "fireSpearSneakShootsOneFireballEveryTwoSeconds")
    public static void fireSpearAddsSeventyFivePercentAttackInTheMainHand(GameTestHelper h) {
        near(h, 17.5, atkWith(h, fireSpear(), EquipmentSlot.MAINHAND), "main hand");
        near(h, 10.0, atkWith(h, fireSpear(), EquipmentSlot.OFFHAND), "off hand");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "fireSpearShootsSixFireballsWithThreeHeadsSixArms")
    public static void fireSpearSetsTargetsAlightForFiveSeconds(GameTestHelper h) {
        Pig target = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 3));
        ItemStack s = fireSpear();
        check(h, s.getItem().hurtEnemy(s, target, player(h)), "returns true");
        eq(h, 100, target.getRemainingFireTicks(), "fire ticks");
        h.succeed();
    }

    private static List<SmallFireball> fireballsOf(GameTestHelper h, ServerPlayer p) {
        return entitiesNear(h, SmallFireball.class, 4, f -> f.getOwner() == p);
    }

    private static int fireballsLaunched(GameTestHelper h, ServerPlayer p) {
        List<SmallFireball> balls = fireballsOf(h, p);
        balls.forEach(SmallFireball::discard);
        return balls.size();
    }

    @GameTest(template = PLATFORM, batch = "yangSwordStrikesTheTargetInFrontAndMarksItForMagic")
    public static void fireSpearRightClickWithoutSneakingDoesNothing(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, InteractionResult.PASS, use(h, p, fireSpear()), "use result");
        eq(h, 0, fireballsLaunched(h, p), "fireballs");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "yangSwordMissesBeyondFourBlocksButStillCoolsDown", timeoutTicks = 150)
    public static void fireSpearSneakShootsOneFireballEveryTwoSeconds(GameTestHelper h) {
        ServerPlayer p = as(h, com.jttw.engine.reincarnation.ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        p.setShiftKeyDown(true);
        ItemStack s = fireSpear();
        eq(h, InteractionResult.CONSUME, use(h, p, s), "first shot");
        eq(h, 1, fireballsLaunched(h, p), "one fireball");
        eq(h, InteractionResult.FAIL, use(h, p, s), "immediate second shot");
        eq(h, 0, fireballsLaunched(h, p), "no fireball on cooldown");
        h.runAfterDelay(35, () -> {
            eq(h, InteractionResult.FAIL, use(h, p, s), "still cooling at 35 ticks");
            eq(h, 0, fireballsLaunched(h, p), "none at 35 ticks");
        });
        h.runAfterDelay(41, () -> {
            eq(h, InteractionResult.CONSUME, use(h, p, s), "ready after 40 ticks");
            eq(h, 1, fireballsLaunched(h, p), "one more fireball");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void fireSpearShootsSixFireballsWithThreeHeadsSixArms(GameTestHelper h) {
        ServerPlayer p = as(h, com.jttw.engine.reincarnation.ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        p.getPersistentData().putBoolean("jttw_nz_three_heads", true);
        p.setShiftKeyDown(true);
        com.jttw.engine.progression.skilltree.SkillTreeData.grantNode(p, "nz_form_1");
        eq(h, InteractionResult.CONSUME, use(h, p, fireSpear()), "use result");
        eq(h, 1, fireballsLaunched(h, p), "Three Heads I: still one fireball");
        p.getPersistentData().remove("jttw_fire_spear_ready_at");
        com.jttw.engine.progression.skilltree.SkillTreeData.grantNode(p, "nz_form_2");
        eq(h, InteractionResult.CONSUME, use(h, p, fireSpear()), "use result at tier II");
        eq(h, 6, fireballsLaunched(h, p), "Three Heads II: six fireballs");
        h.succeed();
    }

    /** The fireball is Nezha's own art: anyone else just holds a spear. */
    @GameTest(template = PLATFORM)
    public static void fireSpearShootsOnlyForNezha(GameTestHelper h) {
        ServerPlayer erlang = as(h, com.jttw.engine.reincarnation.ReincarnationOutcome.EYE_OF_HEAVEN);
        erlang.setShiftKeyDown(true);
        eq(h, InteractionResult.PASS, use(h, erlang, fireSpear()), "Erlang Shen");
        eq(h, 0, fireballsLaunched(h, erlang), "Erlang Shen's fireballs");
        ServerPlayer nobody = player(h);
        nobody.setShiftKeyDown(true);
        eq(h, InteractionResult.PASS, use(h, nobody, fireSpear()), "no character");
        eq(h, 0, fireballsLaunched(h, nobody), "no character's fireballs");
        h.succeed();
    }

    // ── Qiankun Ring ──────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void ringSizeDefaultsToOneAndStaysWithinOneToFive(GameTestHelper h) {
        ItemStack r = ring();
        eq(h, 1, QiankunRingItem.getSize(r), "default size");
        QiankunRingItem.setSize(r, 3);
        eq(h, 3, QiankunRingItem.getSize(r), "size 3");
        QiankunRingItem.setSize(r, 9);
        eq(h, 5, QiankunRingItem.getSize(r), "set above 5 clamps");
        eq(h, 5, r.getTag().getInt(QiankunRingItem.TAG_SIZE), "stored clamped");
        QiankunRingItem.setSize(r, 0);
        eq(h, 1, QiankunRingItem.getSize(r), "set below 1 clamps");
        r.getOrCreateTag().putInt(QiankunRingItem.TAG_SIZE, 42);
        eq(h, 5, QiankunRingItem.getSize(r), "out-of-range stored value reads as 5");
        r.getOrCreateTag().putInt(QiankunRingItem.TAG_SIZE, -3);
        eq(h, 1, QiankunRingItem.getSize(r), "negative stored value reads as 1");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void ringAddsTwentyFivePercentAttackInTheMainHand(GameTestHelper h) {
        near(h, 12.5, atkWith(h, ring(), EquipmentSlot.MAINHAND), "main hand");
        near(h, 10.0, atkWith(h, ring(), EquipmentSlot.OFFHAND), "off hand");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void ringThrowLaunchesATaggedProjectileKeepsTheRingAndCoolsDownTwoSeconds(GameTestHelper h) {
        ServerPlayer p = player(h);
        com.jttw.engine.reincarnation.AlignmentData.markReincarnated(p, com.jttw.engine.reincarnation.ReincarnationOutcome.THIRD_LOTUS_PRINCE); // only Nezha throws the ring
        ItemStack r = ring();
        QiankunRingItem.setSize(r, 4);
        eq(h, InteractionResult.CONSUME, use(h, p, r), "use result");
        check(h, p.getMainHandItem() == r && r.getCount() == 1, "ring stays in hand");
        List<Snowball> thrown = entitiesNear(h, Snowball.class, 4, s -> s.getOwner() == p);
        eq(h, 1, thrown.size(), "one projectile");
        Snowball proj = thrown.get(0);
        eq(h, 4, proj.getPersistentData().getInt("jttw_qk_size"), "projectile carries the ring size");
        eq(h, p.getUUID(), proj.getPersistentData().getUUID("jttw_qk_owner"), "projectile carries the owner");
        thrown.forEach(Snowball::discard);

        check(h, p.getCooldowns().isOnCooldown(r.getItem()), "on cooldown");
        for (int i = 0; i < 39; i++) p.getCooldowns().tick();
        check(h, p.getCooldowns().isOnCooldown(r.getItem()), "still cooling after 39 ticks");
        p.getCooldowns().tick();
        check(h, !p.getCooldowns().isOnCooldown(r.getItem()), "ready after 40 ticks");
        h.succeed();
    }

    /** The thrown ring deals its tooltip's "size x 100% STR" to what it hits. */
    @GameTest(template = PLATFORM, batch = "qiankun_flight", timeoutTicks = 60)
    public static void thrownRingDamagesWhatItHits(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 0, 0f, 0f);
        com.jttw.engine.reincarnation.AlignmentData.markReincarnated(p, com.jttw.engine.reincarnation.ReincarnationOutcome.THIRD_LOTUS_PRINCE); // only Nezha throws the ring
        setBaseAtk(p, 10.0);
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 3));
        float full = golem.getHealth();
        ItemStack r = ring();
        QiankunRingItem.setSize(r, 3);
        use(h, p, r);
        h.succeedWhen(() -> h.assertTrue(golem.getHealth() < full, "golem not damaged by the ring"));
    }

    // ── Yin-Yang Swords ───────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void yinAndYangSwordsAreTaggedAndNamed(GameTestHelper h) {
        ItemStack yin = YinYangSwordItem.createYin();
        ItemStack yang = YinYangSwordItem.createYang();
        ItemStack plain = new ItemStack(ModItems.YIN_YANG_SWORD.get());
        check(h, YinYangSwordItem.isYin(yin) && !YinYangSwordItem.isYang(yin), "yin");
        check(h, YinYangSwordItem.isYang(yang) && !YinYangSwordItem.isYin(yang), "yang");
        check(h, !YinYangSwordItem.isYin(plain) && !YinYangSwordItem.isYang(plain), "plain");
        eq(h, "Yin Sword", yin.getHoverName().getString(), "yin name");
        eq(h, "Yang Sword", yang.getHoverName().getString(), "yang name");
        eq(h, "Yin-Yang Sword", plain.getHoverName().getString(), "plain name");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eitherSwordAddsFiftyPercentAttackInEitherHand(GameTestHelper h) {
        near(h, 15.0, atkWith(h, YinYangSwordItem.createYin(), EquipmentSlot.MAINHAND), "yin main hand");
        near(h, 15.0, atkWith(h, YinYangSwordItem.createYang(), EquipmentSlot.OFFHAND), "yang off hand");
        near(h, 10.0, atkWith(h, YinYangSwordItem.createYin(), EquipmentSlot.HEAD), "not as a helmet");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void yinSwordHitWeakensTheTarget(GameTestHelper h) {
        Pig target = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 3));
        ItemStack yin = YinYangSwordItem.createYin();
        check(h, yin.getItem().hurtEnemy(yin, target, player(h)), "returns true");
        MobEffectInstance weakness = target.getEffect(MobEffects.WEAKNESS);
        check(h, weakness != null, "weakened");
        eq(h, 100, weakness.getDuration(), "5 seconds");
        eq(h, 0, weakness.getAmplifier(), "Weakness I");

        Pig other = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(0, 2, 3));
        ItemStack yang = YinYangSwordItem.createYang();
        yang.getItem().hurtEnemy(yang, other, player(h));
        check(h, !other.hasEffect(MobEffects.WEAKNESS), "yang hit does not weaken");
        h.succeed();
    }

    private static ServerPlayer yangWielder(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 0, 0f, 0f);
        setBaseAtk(p, 6.0);
        return p;
    }

    private static InteractionResult useOffhand(GameTestHelper h, ServerPlayer p, ItemStack stack) {
        p.setItemInHand(InteractionHand.OFF_HAND, stack);
        return stack.getItem().use(h.getLevel(), p, InteractionHand.OFF_HAND).getResult();
    }

    @GameTest(template = PLATFORM)
    public static void yangSwordStrikesTheTargetInFrontAndMarksItForMagic(GameTestHelper h) {
        ServerPlayer p = yangWielder(h);
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 3));
        ItemStack yang = YinYangSwordItem.createYang();
        HurtLog log = HurtLog.on(golem);
        long now = h.getLevel().getGameTime();
        boolean visible = h.getLevel().getEntities(p, p.getBoundingBox().inflate(6)).contains(golem);
        String around = h.getLevel().getEntities(p, p.getBoundingBox().expandTowards(p.getLookAngle().scale(4.0)).inflate(1.0)).stream()
                .map(e -> e.getType().toShortString() + (e.hasCustomName() ? "[" + e.getCustomName().getString() + "]" : "")
                        + "@" + String.format("%.1f,%.1f,%.1f", e.getX() - p.getX(), e.getY() - p.getY(), e.getZ() - p.getZ()))
                .toList().toString();
        try {
            eq(h, InteractionResult.CONSUME, useOffhand(h, p, yang), "use result");
        } finally {
            log.close();
        }
        eq(h, 1, log.playerAttacks().size(), "one hit (golem visible to entity queries: " + visible + ", golem health " + golem.getHealth() + "/" + golem.getMaxHealth() + ", player atk " + p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) + ", look " + p.getLookAngle() + ", offhand " + p.getOffhandItem() + ", in strike box " + around + ")");
        near(h, 6.0, log.playerAttacks().get(0).amount(), "100% of 6 ATK");
        check(h, log.playerAttacks().get(0).source().is(DamageTypes.PLAYER_ATTACK), "player attack");
        MobEffectInstance fatigue = golem.getEffect(MobEffects.DIG_SLOWDOWN);
        check(h, fatigue != null, "mining fatigue");
        eq(h, 1, fatigue.getAmplifier(), "Mining Fatigue II");
        eq(h, 100, fatigue.getDuration(), "5 seconds");
        eq(h, now + 100, golem.getPersistentData().getLong("jttw_magic_vuln"), "magic-vulnerable until +100 ticks");
        check(h, p.getCooldowns().isOnCooldown(yang.getItem()), "on cooldown");
        for (int i = 0; i < 19; i++) p.getCooldowns().tick();
        check(h, p.getCooldowns().isOnCooldown(yang.getItem()), "still cooling after 19 ticks");
        p.getCooldowns().tick();
        check(h, !p.getCooldowns().isOnCooldown(yang.getItem()), "ready after 20 ticks");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void yangSwordMissesBeyondFourBlocksButStillCoolsDown(GameTestHelper h) {
        ServerPlayer p = yangWielder(h);
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 6));
        golem.setNoGravity(true);
        ItemStack yang = YinYangSwordItem.createYang();
        HurtLog log = HurtLog.on(golem);
        try {
            eq(h, InteractionResult.CONSUME, useOffhand(h, p, yang), "use result");
        } finally {
            log.close();
        }
        eq(h, 0, log.playerAttacks().size(), "out of reach");
        check(h, !golem.hasEffect(MobEffects.DIG_SLOWDOWN), "no fatigue");
        check(h, p.getCooldowns().isOnCooldown(yang.getItem()), "cooldown even on a miss");
        golem.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyTheYangSwordHasARightClick(GameTestHelper h) {
        ServerPlayer p = yangWielder(h);
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 3));
        HurtLog log = HurtLog.on(golem);
        try {
            ItemStack yin = YinYangSwordItem.createYin();
            eq(h, InteractionResult.PASS, useOffhand(h, p, yin), "yin");
            check(h, !p.getCooldowns().isOnCooldown(yin.getItem()), "no cooldown");
            ItemStack plain = new ItemStack(ModItems.YIN_YANG_SWORD.get());
            eq(h, InteractionResult.PASS, useOffhand(h, p, plain), "untagged sword");
        } finally {
            log.close();
        }
        eq(h, 0, log.playerAttacks().size(), "nothing struck");
        h.succeed();
    }

    /**
     * Each hand's sword has its own attribute modifier, so putting one away leaves the other's
     * bonus. Uses a zombie because equipment changes are only noticed while an entity ticks.
     */
    @GameTest(template = PLATFORM, timeoutTicks = 40)
    public static void puttingAwayTheOffhandSwordKeepsTheMainHandBonus(GameTestHelper h) {
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        double base = zombie.getAttributeValue(Attributes.ATTACK_DAMAGE);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, YinYangSwordItem.createYin());
        h.runAfterDelay(3, () -> {
            near(h, base * 1.5, zombie.getAttributeValue(Attributes.ATTACK_DAMAGE), "yin in main hand");
            zombie.setItemSlot(EquipmentSlot.OFFHAND, YinYangSwordItem.createYang());
        });
        h.runAfterDelay(6, () -> zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY));
        h.runAfterDelay(9, () -> {
            near(h, base * 1.5, zombie.getAttributeValue(Attributes.ATTACK_DAMAGE), "yin still in main hand");
            h.succeed();
        });
    }
}
