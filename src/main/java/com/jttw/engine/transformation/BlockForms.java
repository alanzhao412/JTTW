package com.jttw.engine.transformation;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.List;

/**
 * What the block forms (and boats) do, server side.
 *
 * <ul>
 *   <li><b>Chest</b> (and <b>chest boat</b>): using the form opens the player's own 27-slot
 *       chest. Its contents are kept on the player, like a shulker box's, through death and
 *       through changing form; only its owner can open it.</li>
 *   <li><b>TNT</b>: using the form, or another player using flint and steel or a fire charge on
 *       it, lights it. After a TNT's 4-second fuse it explodes like TNT, breaking blocks, without
 *       hurting the player, who returns to their own form.</li>
 *   <li><b>Sign</b>: the player writes its text in a screen on their side
 *       ({@link #setSignText}).</li>
 *   <li><b>Boat</b>: other players right-click it to get in ({@link BoatForms}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class BlockForms {

    private BlockForms() {}

    /** Where a player's form chest is kept. */
    public static final String KEY_CHEST = "jttw_form_chest";
    public static final int CHEST_SLOTS = 27;
    /** A TNT block's fuse, in ticks. */
    public static final int TNT_FUSE_TICKS = 80;
    public static final float TNT_POWER = 4.0F;

    public static boolean isForm(Player player, net.minecraft.world.level.block.Block block) {
        TransformationData data = TransformationManager.getTransformation(player);
        return data != null && data.getBlock() == block;
    }

    private static boolean hasChest(Player player) {
        TransformationData data = TransformationManager.getTransformation(player);
        return data != null && (data.getBlock() == Blocks.CHEST
                || (!data.isBlockForm() && !data.isPlayerForm() && data.getEntityType() == EntityType.CHEST_BOAT));
    }

    /** The player used their form (right-click with an empty hand): open the chest, light the TNT. */
    public static void use(ServerPlayer player) {
        if (hasChest(player)) openChest(player);
        else if (isForm(player, Blocks.TNT)) light(player);
    }

    // ── chest ──────────────────────────────────────────────────────

    public static NonNullList<ItemStack> loadChest(Player player) {
        NonNullList<ItemStack> items = NonNullList.withSize(CHEST_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(player.getPersistentData().getCompound(KEY_CHEST), items);
        return items;
    }

    private static void saveChest(Player player, SimpleContainer container) {
        NonNullList<ItemStack> items = NonNullList.withSize(CHEST_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < CHEST_SLOTS; i++) items.set(i, container.getItem(i));
        player.getPersistentData().put(KEY_CHEST, ContainerHelper.saveAllItems(new CompoundTag(), items, true));
    }

    public static void openChest(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(CHEST_SLOTS);
        List<ItemStack> items = loadChest(player);
        for (int i = 0; i < CHEST_SLOTS; i++) container.setItem(i, items.get(i));
        container.addListener(c -> saveChest(player, container));
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, container),
                Component.translatable("container.chest")));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    /** The chest stays with the player through death, as their own storage. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag src = event.getOriginal().getPersistentData();
        if (src.contains(KEY_CHEST)) event.getEntity().getPersistentData().put(KEY_CHEST, src.getCompound(KEY_CHEST).copy());
    }

    // ── TNT ────────────────────────────────────────────────────────

    /** Lights a TNT form; it explodes {@value #TNT_FUSE_TICKS} ticks later. Already lit: no change. */
    public static void light(ServerPlayer player) {
        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null || data.getBlock() != Blocks.TNT || data.isLit()) return;
        TransformationManager.update(player, data.withLitUntil(player.level().getGameTime() + TNT_FUSE_TICKS));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        tickFuse(sp);
    }

    /** A lit TNT form whose fuse has run out explodes and the player returns to their own form. */
    public static void tickFuse(ServerPlayer player) {
        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null || !data.isLit() || player.level().getGameTime() < data.getLitUntil()) return;
        TransformationManager.demorph(player);
        // The player is the explosion's source, which an explosion never damages or pushes
        player.level().explode(player, player.getX(), player.getY(0.0625), player.getZ(),
                TNT_POWER, Level.ExplosionInteraction.TNT);
    }

    /** Flint and steel or a fire charge, used on a TNT-form player, lights them. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof ServerPlayer target)) return;
        Player user = event.getEntity();
        ItemStack held = event.getItemStack();
        if (isForm(target, Blocks.TNT) && (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE))) {
            TransformationData data = TransformationManager.getTransformation(target);
            if (data.isLit()) return;
            light(target);
            if (!user.getAbilities().instabuild) {
                if (held.is(Items.FLINT_AND_STEEL)) held.hurtAndBreak(1, user, p -> p.broadcastBreakEvent(event.getHand()));
                else held.shrink(1);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        // Climb into a boat-form player
        if (BoatForms.seats(target) > 0 && user != target && !user.isPassenger() && held.isEmpty()) {
            if (user.startRiding(target)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    // ── sign ───────────────────────────────────────────────────────

    /** Sets a sign form's text (from the player's own sign screen). Anything else: ignored. */
    public static void setSignText(ServerPlayer player, List<String> lines) {
        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null || !(data.getBlock() instanceof net.minecraft.world.level.block.SignBlock)) return;
        TransformationManager.update(player, data.withSignLines(lines));
    }

    /** A form change that leaves the player no longer a boat sets their riders down. */
    public static void onFormChanged(Player player) {
        if (BoatForms.seats(player) == 0 && player.isVehicle()) player.ejectPassengers();
        else if (BoatForms.seats(player) < player.getPassengers().size()) player.ejectPassengers();
    }
}
