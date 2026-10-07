package com.jttw.engine.gametest.item;

import com.jttw.engine.gametest.GT;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Shared set-up for the item GameTests. */
final class ItemTestSupport {

    private ItemTestSupport() {}

    /** Player base attack damage in vanilla. */
    static final double PLAYER_BASE_ATK = 1.0;

    /**
     * A fresh player standing on the platform floor (the floor is relative y=1) at (x, 2, z), facing {@code yRot} (0 = south, +z) and
     * pitched {@code xRot} degrees (positive looks down).
     */
    static ServerPlayer playerAt(GameTestHelper h, int x, int z, float yRot, float xRot) {
        ServerPlayer p = GT.player(h);
        BlockPos abs = h.absolutePos(new BlockPos(x, 2, z));
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, yRot, xRot);
        p.setYRot(yRot);
        p.setXRot(xRot);
        p.setYHeadRot(yRot);
        return p;
    }

    static ServerPlayer as(GameTestHelper h, ReincarnationOutcome outcome) {
        ServerPlayer p = GT.player(h);
        AlignmentData.markReincarnated(p, outcome);
        return p;
    }

    static void setOutcome(ServerPlayer p, ReincarnationOutcome outcome) {
        AlignmentData.markReincarnated(p, outcome);
    }

    static void setBaseAtk(ServerPlayer p, double atk) {
        p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
    }

    /**
     * Applies the stack's modifiers for {@code slot} the way vanilla does when it notices an
     * equipment change during an entity tick. A fake player never ticks, so tests do it.
     */
    static void applyEquipmentModifiers(ServerPlayer p, ItemStack stack, EquipmentSlot slot) {
        p.getAttributes().addTransientAttributeModifiers(stack.getAttributeModifiers(slot));
    }

    static void removeEquipmentModifiers(ServerPlayer p, ItemStack stack, EquipmentSlot slot) {
        p.getAttributes().removeAttributeModifiers(stack.getAttributeModifiers(slot));
    }

    static InteractionResult use(GameTestHelper h, ServerPlayer p, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult();
    }

    /** The test's structure plus a margin, in world coordinates. */
    static AABB area(GameTestHelper h, double margin) {
        BlockPos min = h.absolutePos(BlockPos.ZERO);
        BlockPos max = h.absolutePos(new BlockPos(5, 6, 5));
        return new AABB(min, max).inflate(margin);
    }

    static <T extends Entity> List<T> entitiesNear(GameTestHelper h, Class<T> type, double margin,
                                                   Predicate<? super T> filter) {
        return h.getLevel().getEntitiesOfClass(type, area(h, margin), filter::test);
    }

    /**
     * Records every hurt that reaches one entity, after all of the mod's damage handlers have
     * adjusted it (LOWEST priority) and before armour. Close it at the end of the test.
     */
    static final class HurtLog implements Consumer<LivingHurtEvent> {
        record Hit(DamageSource source, float amount) {}

        private final Entity target;
        final List<Hit> hits = new ArrayList<>();

        private HurtLog(Entity target) {
            this.target = target;
        }

        static HurtLog on(Entity target) {
            HurtLog log = new HurtLog(target);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingHurtEvent.class, log);
            return log;
        }

        @Override
        public void accept(LivingHurtEvent event) {
            if (event.getEntity() == target) hits.add(new Hit(event.getSource(), event.getAmount()));
        }

        List<Hit> matching(Predicate<DamageSource> which) {
            return hits.stream().filter(hit -> which.test(hit.source())).toList();
        }

        /** Hits from a player's melee damage source (the source every weapon strike here uses). */
        List<Hit> playerAttacks() {
            return matching(src -> src.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK));
        }

        String describe() {
            return hits.stream().map(hit -> hit.source().getMsgId() + "=" + hit.amount()).toList().toString();
        }

        void close() {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    static void near(GameTestHelper h, double expected, double actual, double tolerance, String what) {
        h.assertTrue(Math.abs(expected - actual) <= tolerance,
                what + ": expected " + expected + " (+/-" + tolerance + ") but was " + actual);
    }
}
