package com.jttw.engine.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jttw.engine.reincarnation.AlignmentData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Erlang Shen's divine axe — raw power weapon.
 * High attack damage, slower but devastating hits.
 * Erlang Shen gets +150% damage, others get +50%.
 */
public class ErlangAxeItem extends Item {

    private static final UUID UUID_ATK = UUID.fromString("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6f");
    private static final UUID UUID_SPEED = UUID.fromString("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d70");

    public ErlangAxeItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of();

        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        // Base +50% for everyone. Erlang Shen bonus applied via handler event.
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID_ATK, "jttw:axe_atk",
                0.50, AttributeModifier.Operation.MULTIPLY_TOTAL));
        // Slower attack speed (-20%)
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(UUID_SPEED, "jttw:axe_speed",
                -0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
        return b.build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player)) return true;

        // Cleave: deal 50% splash damage to all entities within 2 blocks of the target
        if (!player.level().isClientSide() && isErlangShen(player)) {
            float atk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double radius = 2.0;
            List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    target.getBoundingBox().inflate(radius),
                    e -> e != player && e != target && e.isAlive());
            for (LivingEntity e : nearby) {
                int oldInvuln = e.invulnerableTime;
                e.invulnerableTime = 0;
                e.hurt(player.damageSources().playerAttack(player), atk * 0.5f);
                e.invulnerableTime = oldInvuln;
            }

            // Visual: sweep particles in a ring around the target showing cleave radius
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                net.minecraft.server.level.ServerLevel sl = sp.serverLevel();
                double tx = target.getX();
                double ty = target.getY() + 0.5;
                double tz = target.getZ();
                int particleCount = 12;
                for (int p = 0; p < particleCount; p++) {
                    double angle = (Math.PI * 2.0 / particleCount) * p;
                    double px = tx + Math.cos(angle) * radius;
                    double pz = tz + Math.sin(angle) * radius;
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                            px, ty, pz, 1, 0, 0, 0, 0);
                }
            }
        }

        return true;
    }

    // ── Ground Cleave (hold right-click) ─────────────────────────────

    /** Game time the cleave is ready again (on the player; absent = ready). */
    private static final String KEY_CLEAVE_READY_AT = "jttw_axe_cleave_ready_at";
    private static final long CLEAVE_COOLDOWN_TICKS = 60L; // 3 seconds

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // hold indefinitely like a bow
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.BOW;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return net.minecraft.world.InteractionResultHolder.consume(stack);
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp))
            return net.minecraft.world.InteractionResultHolder.pass(stack);

        if (level.getGameTime() < sp.getPersistentData().getLong(KEY_CLEAVE_READY_AT))
            return net.minecraft.world.InteractionResultHolder.fail(stack);

        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || !(entity instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        int chargeTime = getUseDuration(stack) - timeLeft;
        if (chargeTime < 10) return;

        sp.getPersistentData().putLong(KEY_CLEAVE_READY_AT, level.getGameTime() + CLEAVE_COOLDOWN_TICKS);

        float atk = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float chargePower = Math.min(1.0f, (float) (chargeTime - 10) / 30f);
        float cleaveDamage = atk * (1.0f + chargePower * 1.5f);

        int playerLevel = com.jttw.engine.progression.level.PlayerLevel.getLevel(sp);
        double levelFrac = Math.min(playerLevel, 100) / 100.0;

        // Range: 10 at lv1 → 70 at lv100, scaled by charge
        double maxRange = 10.0 + 60.0 * levelFrac;
        double cleaveRange = Math.max(5.0, maxRange * chargePower);

        // Cross-section: height 10→20, half-width 1→3 (diameter 3→7)
        int cleaveHeight = 10 + (int) (10.0 * levelFrac);
        int cleaveHalfW = 1 + (int) (2.0 * levelFrac); // 1→3, so diameter 3→7

        // Hardness budget: harder blocks eat more budget. Soft=1, stone=2, obsidian=10
        double totalBudget = 500.0 + 2000.0 * levelFrac; // 500 at lv1, 2500 at lv100
        double budgetRemaining = totalBudget;

        // 3D direction — follows look exactly (can aim up, down, diagonal)
        net.minecraft.world.phys.Vec3 look = sp.getLookAngle().normalize();
        net.minecraft.world.phys.Vec3 eyeStart = sp.getEyePosition();
        net.minecraft.server.level.ServerLevel serverLevel = sp.serverLevel();

        // Build perpendicular axes for the cross-section disc
        net.minecraft.world.phys.Vec3 up = new net.minecraft.world.phys.Vec3(0, 1, 0);
        net.minecraft.world.phys.Vec3 right = look.cross(up).normalize();
        if (right.lengthSqr() < 1.0e-6) {
            right = new net.minecraft.world.phys.Vec3(1, 0, 0);
        }
        net.minecraft.world.phys.Vec3 localUp = right.cross(look).normalize();

        java.util.Random rng = new java.util.Random(sp.level().getGameTime());
        java.util.List<net.minecraft.core.BlockPos> cleaved = new java.util.ArrayList<>();

        int steps = (int) cleaveRange;
        for (int i = 0; i < steps && budgetRemaining > 0; i++) {
            double dist = i + 1;

            // Path wobbles for organic trajectory
            double wobbleR = (rng.nextDouble() - 0.5) * 1.2;
            double wobbleU = (rng.nextDouble() - 0.5) * 1.2;
            double cx = eyeStart.x + look.x * dist + right.x * wobbleR + localUp.x * wobbleU;
            double cy = eyeStart.y + look.y * dist + right.y * wobbleR + localUp.y * wobbleU;
            double cz = eyeStart.z + look.z * dist + right.z * wobbleR + localUp.z * wobbleU;

            // Cross-section varies per step + taper
            int localHalfW = cleaveHalfW + rng.nextInt(2);
            int localHalfH = (cleaveHeight / 2) + rng.nextInt(3) - 1;
            double taper = 1.0 - Math.abs((double) i / steps - 0.4) * 0.5;
            localHalfW = Math.max(1, (int) (localHalfW * taper));
            localHalfH = Math.max(2, (int) (localHalfH * taper));

            // Jagged outer edges only — interior is always clean
            for (int u = -localHalfH; u <= localHalfH && budgetRemaining > 0; u++) {
                for (int r = -localHalfW; r <= localHalfW && budgetRemaining > 0; r++) {
                    // Only skip blocks on the very outermost edge for jagged silhouette
                    if ((Math.abs(r) == localHalfW || Math.abs(u) == localHalfH) && rng.nextFloat() < 0.3f) continue;

                    double bx = cx + right.x * r + localUp.x * u;
                    double by = cy + right.y * r + localUp.y * u;
                    double bz = cz + right.z * r + localUp.z * u;

                    net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(
                            (int) Math.floor(bx), (int) Math.floor(by), (int) Math.floor(bz));
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(bp);
                    if (state.isAir()) continue;

                    // Fluids: free to clear
                    if (!state.getFluidState().isEmpty()) {
                        cleaved.add(bp);
                        continue;
                    }

                    // Bedrock: impassable
                    float hardness = state.getDestroySpeed(level, bp);
                    if (hardness < 0) continue;

                    // Budget cost: harder blocks cost more
                    double cost = 1.0 + hardness * 0.5;
                    if (cost > budgetRemaining) {
                        budgetRemaining = 0;
                        break;
                    }
                    budgetRemaining -= cost;

                    cleaved.add(bp);
                }
            }

            // Sparse particles
            if (i % 5 == 0) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        cx, cy, cz, 1, 0.3, 0.3, 0.3, 0.0);
            }
        }

        // The cut, spread over the next ticks rather than all in this one
        com.jttw.engine.world.TerrainBreaker.breakBlocks(serverLevel, cleaved);

        // Explosion at end point
        net.minecraft.world.phys.Vec3 endPos = eyeStart.add(look.scale(cleaveRange));
        level.explode(sp, endPos.x, endPos.y, endPos.z,
                2.0f + chargePower * 2.0f, Level.ExplosionInteraction.NONE);

        // Damage entities along the path
        net.minecraft.world.phys.Vec3 endVec = eyeStart.add(look.scale(cleaveRange));
        double searchRadius = Math.max(cleaveHeight, cleaveHalfW * 2) + 2;
        net.minecraft.world.phys.AABB cleaveBox = new net.minecraft.world.phys.AABB(
                Math.min(eyeStart.x, endVec.x) - searchRadius,
                Math.min(eyeStart.y, endVec.y) - searchRadius,
                Math.min(eyeStart.z, endVec.z) - searchRadius,
                Math.max(eyeStart.x, endVec.x) + searchRadius,
                Math.max(eyeStart.y, endVec.y) + searchRadius,
                Math.max(eyeStart.z, endVec.z) + searchRadius
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, cleaveBox, e -> e != sp && e.isAlive());

        for (LivingEntity target : targets) {
            // Project target onto cleave line, check distance
            net.minecraft.world.phys.Vec3 toTarget = target.position().subtract(eyeStart);
            double alongPath = toTarget.dot(look);
            if (alongPath < 0 || alongPath > cleaveRange) continue;

            net.minecraft.world.phys.Vec3 closest = eyeStart.add(look.scale(alongPath));
            double perpDist = target.position().distanceTo(closest);
            if (perpDist > Math.max(cleaveHalfW, cleaveHeight / 2) + 2) continue;

            int oldInvuln = target.invulnerableTime;
            target.invulnerableTime = 0;
            target.hurt(sp.damageSources().playerAttack(sp), cleaveDamage);
            target.invulnerableTime = oldInvuln;

            net.minecraft.world.phys.Vec3 knockDir = target.position().subtract(closest).normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(
                    knockDir.x * 0.8, 0.4, knockDir.z * 0.8));
        }

        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);

        sp.getCooldowns().addCooldown(this, (int) CLEAVE_COOLDOWN_TICKS);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Divine Axe of Erlang Shen").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§e+150% damage (Erlang Shen)"));
        tooltip.add(Component.literal("§e+50% damage (others)"));
        tooltip.add(Component.literal("§c-20% attack speed"));
        tooltip.add(Component.literal("§aErlang Shen: Cleave — hits nearby enemies for 50%"));
        tooltip.add(Component.literal("§7Hold right-click: Ground Cleave"));
        tooltip.add(Component.literal("§7  Tears the ground, explosion at end"));
    }

    public static boolean isErlangShen(Player player) {
        String outcome = player.getPersistentData()
                .getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "eye_of_heaven".equals(outcome);
    }
}
