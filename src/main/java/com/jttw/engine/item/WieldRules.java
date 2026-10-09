package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.erlang.ErlangShenCharacter;
import com.jttw.engine.character.nezha.NezhaCharacter;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.StatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Who may wield the artifacts, and what they cost in the wrong hands. The character an
 * artifact belongs to is never held back by it.
 *
 * <ul>
 *   <li><b>Ruyi Jingu Bang:</b> 30 STR to fight with (Wukong: none).</li>
 *   <li><b>Erlang's Axe:</b> 40 STR to fight with; its charged cleave is Erlang's alone.</li>
 *   <li><b>Fire-Tip Spear:</b> burns whoever holds it, unless Nezha.</li>
 *   <li><b>Qiankun Ring:</b> only Nezha throws it or fights with it; it burns anyone else
 *       who holds it.</li>
 *   <li><b>Yin-Yang Swords:</b> a pair: holding one draws the other into the off hand. Unless
 *       Nezha, whoever holds them is frostbitten and on fire at once.</li>
 *   <li><b>Huntian Cloth:</b> binds and wounds whoever wears it, unless Nezha.</li>
 * </ul>
 * STR is the strength the player's class and level give them, as the character screen shows it.
 *
 * <p>Every enhanced form of a weapon draws Qi each second it is held in the main hand, and falls
 * back to its plain form when the Qi runs out: the Ruyi's Qi Empowered ({@value #RUYI_EMPOWERED_QI})
 * and Large ({@value #RUYI_LARGE_QI}), Erlang's large axe ({@value ErlangAxeItem#LARGE_QI_PER_SECOND}),
 * Nezha's lit spear ({@value FireTipSpearItem#AFLAME_QI_PER_SECOND}).
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class WieldRules {

    private WieldRules() {}

    public static final double RUYI_STR = 30;
    public static final double AXE_STR = 40;
    /** Set on a Yin-Yang sword this drew into the off hand, so it goes back when the pair is put away. */
    public static final String TAG_DRAWN = "jttw_yy_drawn";
    public static final float RUYI_EMPOWERED_QI = 3f;
    public static final float RUYI_LARGE_QI = 5f;

    /** An enhanced form a weapon is in: its name and what it costs each second. */
    public record Form(String name, float qiPerSecond) {}

    /** The enhanced form this weapon is in, or null if it is plain. */
    @Nullable
    public static Form enhancedForm(ItemStack s) {
        if (s.getItem() instanceof RuyiJinguBangItem) {
            if (RuyiJinguBangItem.isLargeMode(s)) return new Form("The Ruyi Jingu Bang's immense size", RUYI_LARGE_QI);
            if (RuyiJinguBangItem.isQiEmpowered(s)) return new Form("The Ruyi Jingu Bang's Qi", RUYI_EMPOWERED_QI);
        }
        if (s.getItem() instanceof ErlangAxeItem && ErlangAxeItem.isLarge(s)) {
            return new Form("The axe's great size", ErlangAxeItem.LARGE_QI_PER_SECOND);
        }
        if (s.getItem() instanceof FireTipSpearItem && FireTipSpearItem.isAflame(s)) {
            return new Form("The spear's fire", FireTipSpearItem.AFLAME_QI_PER_SECOND);
        }
        return null;
    }

    /** Back to the plain form. */
    public static void revert(ItemStack s) {
        if (s.getItem() instanceof RuyiJinguBangItem) RuyiJinguBangItem.revert(s);
        if (s.getItem() instanceof ErlangAxeItem) ErlangAxeItem.setLarge(s, false);
        if (s.getItem() instanceof FireTipSpearItem) FireTipSpearItem.setAflame(s, false);
    }

    /** Whether this player may hold this weapon in its enhanced form at all. */
    private static boolean ownsForm(Player p, ItemStack s) {
        if (s.getItem() instanceof RuyiJinguBangItem) return WukongCharacter.is(p);
        if (s.getItem() instanceof ErlangAxeItem) return isErlang(p);
        if (s.getItem() instanceof FireTipSpearItem) return NezhaCharacter.is(p);
        return true;
    }

    /** One second's draw on the main-hand weapon's form: it falls back when the Qi runs out. */
    public static void drawFormQi(ServerPlayer p) {
        ItemStack main = p.getMainHandItem();
        Form form = enhancedForm(main);
        if (form == null) return;
        if (!ownsForm(p, main)) { // in the wrong hands it is only itself
            revert(main);
            return;
        }
        if (!com.jttw.engine.progression.qi.QiData.spend(p, form.qiPerSecond())) {
            revert(main);
            p.displayClientMessage(Component.literal("§e" + form.name() + " fades: out of Qi."), true);
        }
        com.jttw.engine.progression.qi.QiManager.syncNow(p);
    }

    /** The player's STR: their class's strength at their level, 1 before they have a class. */
    public static double strength(Player p) {
        ProgressionClass cls = PlayerLevel.getProgressionClass(p);
        return cls == null ? 1.0 : StatBlock.compute(cls, PlayerLevel.getLevel(p)).attackDamage();
    }

    /** The first level at which this class has {@code str} STR, or -1 if it never does. */
    public static int levelFor(ProgressionClass cls, double str) {
        for (int lv = 1; lv <= 100; lv++) {
            if (StatBlock.compute(cls, lv).attackDamage() >= str) return lv;
        }
        return -1;
    }

    private static boolean isErlang(Player p) {
        return Characters.of(p) == ErlangShenCharacter.INSTANCE;
    }

    /** STR this player needs to fight with this item: 0 if none. */
    public static double strNeeded(Player p, ItemStack stack) {
        if (stack.getItem() instanceof RuyiJinguBangItem && !WukongCharacter.is(p)) return RUYI_STR;
        if (stack.getItem() instanceof ErlangAxeItem && !isErlang(p)) return AXE_STR;
        return 0;
    }

    /** Why this player cannot fight with this item, or null if they can. */
    @Nullable
    public static Component refusal(Player p, ItemStack stack) {
        if (stack.getItem() instanceof QiankunRingItem && !NezhaCharacter.is(p)) {
            return Component.literal("§cThe Qiankun Ring answers only to Nezha.");
        }
        double need = strNeeded(p, stack);
        double have = strength(p);
        if (need > 0 && have < need) {
            return Component.literal(String.format("§cToo heavy: needs %.0f STR (you have %.1f).", need, have));
        }
        return null;
    }

    /** Only Erlang charges the axe's cleave. */
    public static boolean canCleave(Player p) {
        return isErlang(p);
    }

    /** Only Nezha throws the Qiankun Ring. */
    public static boolean canThrowRing(Player p) {
        return NezhaCharacter.is(p);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player p = event.getEntity();
        Component why = refusal(p, p.getMainHandItem());
        if (why == null) return;
        event.setCanceled(true);
        if (!p.level().isClientSide()) p.displayClientMessage(why, true);
    }

    // ── Holding and wearing ────────────────────────────────────────

    private static boolean holding(Player p, Class<?> item) {
        return item.isInstance(p.getMainHandItem().getItem()) || item.isInstance(p.getOffhandItem().getItem());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        pairSwords(p);
        if (p.tickCount % 20 == 0) drawFormQi(p);
        if (p.tickCount % 4 == 0 && FireTipSpearItem.isAflame(p.getMainHandItem())) {
            // The lit spear trails flame from the wielder's hand
            net.minecraft.world.phys.Vec3 tip = p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.4, 0);
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, tip.x, tip.y, tip.z, 3, 0.1, 0.1, 0.1, 0.01);
        }
        boolean nezha = NezhaCharacter.is(p);
        if (nezha) return;

        // The swords: frostbite and fire together (frost thaws each tick, so it is held there)
        if (holding(p, YinYangSwordItem.class)) {
            p.setTicksFrozen(Math.max(p.getTicksFrozen(), p.getTicksRequiredToFreeze() + 20));
            if (p.getRemainingFireTicks() < 40) p.setRemainingFireTicks(40);
        }
        if (p.tickCount % 20 != 0) return;
        if (holding(p, FireTipSpearItem.class) || holding(p, QiankunRingItem.class)) {
            p.setSecondsOnFire(3);
        }
        if (p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof HuntianClothItem) {
            // The cloth binds: slowed, clumsy, weakened, and wrung
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true));
            p.hurt(p.damageSources().magic(), 1f);
        }
    }

    /**
     * The swords are a pair: one in the main hand draws the other from the inventory into the off
     * hand; put away, the drawn one goes back.
     */
    public static void pairSwords(ServerPlayer p) {
        Inventory inv = p.getInventory();
        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        if (main.getItem() instanceof YinYangSwordItem) {
            boolean mainYang = YinYangSwordItem.isYang(main);
            if (off.getItem() instanceof YinYangSwordItem && YinYangSwordItem.isYang(off) != mainYang) return; // paired
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == inv.selected) continue;
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof YinYangSwordItem && YinYangSwordItem.isYang(s) != mainYang) {
                    ItemStack partner = s.copy();
                    partner.getOrCreateTag().putBoolean(TAG_DRAWN, true);
                    inv.items.set(i, off.copy()); // what was in the off hand takes the partner's place
                    p.setItemSlot(EquipmentSlot.OFFHAND, partner);
                    return;
                }
            }
            return;
        }
        // The pair put away: the drawn sword goes back into the inventory
        if (off.getItem() instanceof YinYangSwordItem && off.hasTag() && off.getTag().getBoolean(TAG_DRAWN)) {
            ItemStack back = off.copy();
            back.getTag().remove(TAG_DRAWN);
            int free = inv.getFreeSlot();
            if (free >= 0) {
                inv.items.set(free, back);
                p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        }
    }

    // ── What the tooltip says ──────────────────────────────────────

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Player p = event.getEntity();
        ItemStack stack = event.getItemStack();
        double need = p == null ? 0 : strNeeded(p, stack);
        if (need > 0) {
            double have = strength(p);
            event.getToolTip().add(Component.literal(String.format("Requires %.0f STR (you have %.1f)", need, have))
                    .withStyle(have >= need ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        String warning = null;
        if (stack.getItem() instanceof ErlangAxeItem) warning = "Its cleave answers only to Erlang Shen.";
        if (stack.getItem() instanceof FireTipSpearItem) warning = "Burns any hand but Nezha's.";
        if (stack.getItem() instanceof QiankunRingItem) warning = "Only Nezha throws or wields it; it burns any other hand.";
        if (stack.getItem() instanceof YinYangSwordItem) warning = "A pair: held, both hands are filled. Any hand but Nezha's freezes and burns.";
        if (stack.getItem() instanceof HuntianClothItem) warning = "Binds and wounds any wearer but Nezha.";
        if (warning != null) event.getToolTip().add(Component.literal(warning).withStyle(ChatFormatting.DARK_RED));
    }
}
