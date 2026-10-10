package com.jttw.engine.item;

import com.jttw.engine.character.nezha.NezhaCharacter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jttw.engine.reincarnation.AlignmentData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class FireTipSpearItem extends Item {

    private static final UUID UUID_ATK = UUID.fromString("c1a2b3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
    /** Game time the fireball is ready again (on the player; absent = ready). */
    private static final String KEY_FIREBALL_READY_AT = "jttw_fire_spear_ready_at";
    private static final long FIREBALL_COOLDOWN_TICKS = 40L;
    /** What the spear's own fireball hits for: a vanilla small fireball's 5. */
    public static final float FIREBALL_DAMAGE = 5.0f;

    /** Lit (Nezha, sneak + right-click): every hit ignites and hits harder; draws Qi while held. */
    public static final String TAG_AFLAME = "jttw_spear_aflame";
    public static final float AFLAME_QI_PER_SECOND = 3f;

    public static boolean isAflame(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_AFLAME);
    }

    public static void setAflame(ItemStack stack, boolean aflame) {
        stack.getOrCreateTag().putBoolean(TAG_AFLAME, aflame);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isAflame(stack);
    }

    public FireTipSpearItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of();
        // The attack bonus is for anyone; the sneak fireball (use) is Nezha's alone
        double mult = isAflame(stack) ? 1.25 : 0.75; // +75% for everyone holding it, +125% lit; Nezha gets extra from handler
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID_ATK, "jttw:spear_atk",
                mult * Armaments.refineMultiplier(stack), AttributeModifier.Operation.MULTIPLY_TOTAL));
        return b.build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isAflame(stack)) target.setRemainingFireTicks(100); // lit: 5 seconds of fire
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // The spear's fire is Nezha's alone; in other hands it is only a spear
        if (!NezhaCharacter.is(sp)) return InteractionResultHolder.pass(stack);

        // Sneak + right-click: light the spear, or put it out
        if (sp.isShiftKeyDown()) {
            if (isAflame(stack)) {
                setAflame(stack, false);
                sp.displayClientMessage(Component.literal("§7The spear's fire dies down."), true);
            } else if (com.jttw.engine.progression.qi.QiData.getCurrent(sp) >= AFLAME_QI_PER_SECOND) {
                setAflame(stack, true);
                sp.displayClientMessage(Component.literal("§6The Fire-Tip Spear blazes!"), true);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 1f);
            } else {
                sp.displayClientMessage(Component.literal("§cNot enough Qi."), true);
            }
            return InteractionResultHolder.success(stack);
        }

        long now = sp.level().getGameTime();
        if (now < sp.getPersistentData().getLong(KEY_FIREBALL_READY_AT)) return InteractionResultHolder.fail(stack);
        sp.getPersistentData().putLong(KEY_FIREBALL_READY_AT, now + FIREBALL_COOLDOWN_TICKS);

        // Three Heads Six Arms II: a barrage of six
        boolean threeHeads = sp.getPersistentData().getBoolean("jttw_nz_three_heads")
                && com.jttw.engine.progression.skilltree.SkillTreeData.getNzFormTier(sp) >= 2;
        int count = threeHeads ? 6 : 1;
        Vec3 look = sp.getLookAngle();

        for (int i = 0; i < count; i++) {
            double spread = (i == 0) ? 0 : (Math.random() - 0.5) * 0.3;
            SmallFireball fb = new SmallFireball(level,
                    sp.getX(), sp.getEyeY() - 0.1, sp.getZ(),
                    look.x + spread, look.y + spread * 0.5, look.z + spread);
            fb.setOwner(sp);
            level.addFreshEntity(fb);
        }

        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6Fire-Tipped Spear of Nezha").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(isAflame(stack) ? "§c+125% damage, every hit ignites (lit)" : "§e+75% damage"));
        tooltip.add(Component.literal("§7Nezha: right-click: Fireball"));
        tooltip.add(Component.literal("§7Nezha: sneak + right-click: light it (Qi each second)"));
    }
}
