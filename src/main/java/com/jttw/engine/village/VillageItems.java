package com.jttw.engine.village;

import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The goods of the Tang villages and of the supernatural world's markets, as items: the coins; the
 * innkeeper's food and drink; the herbalist's medicine and Qi tonics (far weaker than an Immortal
 * Peach or Laozi's pill, which refill a whole pool of Qi); the fortune teller's rumour scrolls; and
 * the talismans, incense and charms sold only by the ghost market and Heaven's merchants
 * ({@link Talismans}). Each says what it does in its tooltip ({@code item.jttw_engine.<id>.desc}).
 */
public final class VillageItems {

    private VillageItems() {}

    /** The Qi a Qi-gathering pill restores: this share of its eater's pool (and never less than {@link #LEAST_QI}). */
    public static final float PILL_SHARE = 0.15f, DEW_SHARE = 0.35f, LEAST_QI = 40f;
    /** How long a talisman's effect lasts once burned (3 minutes); a stick of incense (90 seconds). */
    public static final int TALISMAN_TICKS = 3 * 60 * 20, INCENSE_TICKS = 90 * 20;
    /** How much a herbal decoction heals at once. */
    public static final float DECOCTION_HEAL = 4f;

    /** An item that says what it is in its tooltip, from its {@code .desc} entry. */
    public static class Described extends Item {
        public Described(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        }
    }

    /** A coin: worth so many wen ({@link Coins}). */
    public static class Coin extends Described {
        private final int wen;

        public Coin(int wen) {
            super(new Properties());
            this.wen = wen;
        }

        public int wen() {
            return wen;
        }
    }

    // ── Food and drink ───────────────────────────────────────────────

    public static Item riceBowl() {
        return new Described(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f)
                .usingConvertsTo(Items.BOWL).build()));
    }

    /** A steamed bun with a filling of pork and leek (包子). */
    public static Item steamedBun() {
        return new Described(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7f).build()));
    }

    /** A plain steamed bun (馒头): the pilgrims' alms-bowl staple. */
    public static Item mantou() {
        return new Described(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).fast().build()));
    }

    /** Rice wine (米酒): heartening, a little dizzying. Not the Immortals' wine. */
    public static Item riceWine() {
        return new Drink(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible()
                .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 0), 1f)
                .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 4 * 20, 0), 1f).build()), 0f);
    }

    /** A bowl of tea: it clears the head. */
    public static Item tea() {
        return new Drink(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.4f).alwaysEdible()
                .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 90 * 20, 0), 1f).build()), 0f,
                MobEffects.CONFUSION, MobEffects.MOVEMENT_SLOWDOWN);
    }

    /** An herbal decoction (汤药): heals a little, and cures poison, nausea, hunger and weakness. */
    public static Item herbalDecoction() {
        return new Drink(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(0).saturationModifier(0f).alwaysEdible()
                .usingConvertsTo(Items.BOWL).build()), DECOCTION_HEAL,
                MobEffects.POISON, MobEffects.CONFUSION, MobEffects.HUNGER, MobEffects.WEAKNESS);
    }

    /** A drink: drunk, not eaten; heals {@code heal} and takes away the effects it cures. */
    public static class Drink extends Described {
        private final float heal;
        private final List<Holder<MobEffect>> cures;

        @SafeVarargs
        public Drink(Properties properties, float heal, Holder<MobEffect>... cures) {
            super(properties);
            this.heal = heal;
            this.cures = List.of(cures);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
            if (!level.isClientSide) {
                for (Holder<MobEffect> e : cures) drinker.removeEffect(e);
                if (heal > 0) drinker.heal(heal);
            }
            return super.finishUsingItem(stack, level, drinker);
        }
    }

    /**
     * A Qi tonic: the herbalist's Qi-gathering pill (聚气丹), or Heaven's jade dew (玉露), drunk.
     * Restores a share of its user's Qi pool, at least {@link #LEAST_QI}.
     */
    public static class QiTonic extends Described {
        private final float share;
        private final boolean drink;

        public QiTonic(float share, boolean drink) {
            super(new Properties().stacksTo(16).rarity(drink ? Rarity.UNCOMMON : Rarity.COMMON)
                    .food(drink ? new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f).alwaysEdible()
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 10 * 20, 0), 1f).build()
                            : new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).alwaysEdible().fast().build()));
            this.share = share;
            this.drink = drink;
        }

        /** How much Qi it gives someone whose pool is {@code max}. */
        public float qiFor(float max) {
            return Math.max(LEAST_QI, max * share);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return drink ? UseAnim.DRINK : UseAnim.EAT;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            if (!level.isClientSide && user instanceof ServerPlayer p) {
                QiData.restore(p, qiFor(QiData.getMax(p)));
                QiManager.syncNow(p);
            }
            return super.finishUsingItem(stack, level, user);
        }
    }

    // ── Rumours ──────────────────────────────────────────────────────

    /** A rumour scroll: read, it tells which demons lie ahead on its reader's road ({@link Rumours}). */
    public static class RumourScroll extends Described {
        public RumourScroll() {
            super(new Properties().stacksTo(16));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            ServerPlayer p = (ServerPlayer) player;
            for (Component line : Rumours.tell(p)) p.sendSystemMessage(line);
            level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
            stack.consume(1, p);
            p.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.consume(stack);
        }
    }

    // ── Talismans and charms ─────────────────────────────────────────

    /** A paper talisman (符箓), or a stick of incense, burned for its effect on its user ({@link Talismans}). */
    public static class Talisman extends Described {
        private final Holder<MobEffect> effect;
        private final int ticks;

        public Talisman(Holder<MobEffect> effect, int ticks) {
            super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
            this.effect = effect;
            this.ticks = ticks;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            burn(player, effect, ticks);
            stack.consume(1, player);
            return InteractionResultHolder.consume(stack);
        }

        /** Burns one on {@code user}: its effect for {@code ticks}, a puff of ash and sparks. */
        public static void burn(LivingEntity user, Holder<MobEffect> effect, int ticks) {
            user.addEffect(new MobEffectInstance(effect, ticks, 0));
            if (user.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.FLAME, user.getX(), user.getY() + 1.2, user.getZ(), 10, 0.25, 0.2, 0.25, 0.02);
                server.sendParticles(ParticleTypes.WHITE_ASH, user.getX(), user.getY() + 1.4, user.getZ(), 20, 0.35, 0.3, 0.35, 0.02);
                server.playSound(null, user.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
            }
        }
    }

    /** A house-warding talisman (镇宅符), pasted on a block: no monster comes into the world near it for a day ({@link Wards}). */
    public static class HouseWard extends Described {
        public HouseWard() {
            super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
            var at = ctx.getClickedPos();
            Wards.of(level).add(at, level.getGameTime() + Wards.TICKS);
            level.sendParticles(ParticleTypes.ENCHANT, at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, 40, 0.5, 0.5, 0.5, 0.4);
            level.playSound(null, at, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1f, 0.8f);
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().displayClientMessage(Component.translatable("item.jttw_engine.house_ward_talisman.pasted", Wards.RADIUS), true);
                ctx.getItemInHand().consume(1, ctx.getPlayer());
            }
            return InteractionResult.CONSUME;
        }
    }

    /** A peace charm (平安符): carried, it softens the blows of demons, ghosts and the walking dead ({@link Talismans}). */
    public static class PeaceCharm extends Described {
        public PeaceCharm() {
            super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        }
    }
}
