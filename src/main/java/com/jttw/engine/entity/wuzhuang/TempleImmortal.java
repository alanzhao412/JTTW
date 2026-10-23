package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.wuzhuang.Wuzhuang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * One of the immortals of the Temple of the Five Villages (Zhenyuan, and his senior disciples
 * Clear Wind and Bright Moon): a {@link ZoneBoss} of the mountain, fought to the death.
 * <ul>
 *   <li>righteous: they fight no one unprovoked but the temple's enemies
 *       ({@link Wuzhuang#isTempleEnemy}: a thief of the fruit, or whoever has eaten one);</li>
 *   <li>hard to harm: no blow takes more than {@link #hitCap()} of their health (only what kills
 *       outright, as /kill, ignores it);</li>
 *   <li>named in gold, with their level.</li>
 * </ul>
 */
public abstract class TempleImmortal extends ZoneBoss implements TempleFolk {

    protected TempleImmortal(EntityType<? extends TempleImmortal> type, Level level) {
        super(type, level);
    }

    // ── What a subclass says ────────────────────────────────────────────

    /** Its level. */
    public abstract int immortalLevel();

    /** The largest share of its health one blow can take. */
    protected double hitCap() {
        return 1.0;
    }

    @Override
    public Zones.Zone zone() {
        return Zones.MOUNT_WUZHUANG;
    }

    // ── What it is ──────────────────────────────────────────────────────

    @Override
    protected Component getTypeName() {
        return Component.empty().append(super.getTypeName()).append(" · Lv " + immortalLevel())
                .withStyle(s -> s.withColor(TextColor.fromRgb(0xF0D060)));
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    /** Fights no one unprovoked but the temple's enemies: these, with its own goals. */
    protected void addTempleTargets() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p instanceof Player pl && Wuzhuang.isTempleEnemy(pl)));
    }

    /** No blow takes more than its cap. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) amount = (float) Math.min(amount, getMaxHealth() * hitCap());
        return super.hurt(source, amount);
    }
}
