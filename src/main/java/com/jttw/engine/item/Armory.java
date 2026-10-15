package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A character's armory: a slot for each of their armaments ({@link Armaments}), all of it carried
 * at once. Everything in it counts as equipped: what is worn (Nezha's Wind Fire Wheels, his
 * Huntian sash) works from there, leaving the armour slots free, and the weapons wait there to be
 * cycled into hand ({@link #cycle}). It is kept with the character, through death: their own
 * armaments, dropped as they die, go back into it.
 *
 * <p>The more of their armaments a character has forged and refined, the stronger they are: each
 * one held adds {@value #PER_ARMAMENT_PERCENT}% to their health and attack, and each level of
 * refinement across them all {@value #PER_REFINE_TENTHS}/10 of a percent more. Nezha, with the
 * most, gains the most.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Armory {

    private Armory() {}

    public static final String KEY = "jttw_armory";
    public static final int PER_ARMAMENT_PERCENT = 6, PER_REFINE_TENTHS = 5;

    /** Worn rather than held: they work from the armory and are never cycled into hand. */
    private static final List<String> WORN = List.of("wind_fire_wheels", "huntian_cloth");
    /** Held in the off hand alongside another: the Yang sword with the Yin. */
    private static final Map<String, String> PARTNER = Map.of("yin_sword", "yang_sword");

    @Nullable
    public static ReincarnationOutcome owner(Player p) {
        return ReincarnationOutcome.byId(p.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME));
    }

    /** This character's armaments, slot by slot (none for a character without them). */
    public static List<Armaments.Armament> slots(Player p) {
        return Armaments.of(owner(p));
    }

    /** A character's armory, as a container that keeps itself with them whenever it changes. */
    public static final class Contents extends SimpleContainer {
        private final List<Armaments.Armament> slots;
        @Nullable private final Player player;

        Contents(List<Armaments.Armament> slots, @Nullable Player player) {
            super(Math.max(1, slots.size()));
            this.slots = slots;
            this.player = player;
        }

        /** A slot holds only its own armament, one of it. */
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < slots.size() && slots.get(slot).is().test(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        private boolean quiet;

        @Override
        public void setChanged() {
            super.setChanged();
            if (player != null && !quiet) save(player, this);
        }

        /** Puts a stack in a slot without keeping the armory yet (call {@link #setChanged} after). */
        void place(int slot, ItemStack stack) {
            quiet = true;
            setItem(slot, stack);
            quiet = false;
        }

        public List<Armaments.Armament> armaments() {
            return slots;
        }
    }

    /** An empty armory of these slots, not kept anywhere (the client's copy, in its screen). */
    public static Contents detached(List<Armaments.Armament> slots) {
        return new Contents(slots, null);
    }

    /** The player's armory. */
    public static Contents of(Player p) {
        List<Armaments.Armament> slots = slots(p);
        Contents kept = new Contents(slots, p);
        ListTag list = p.getPersistentData().getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int slot = t.getInt("Slot");
            if (slot >= 0 && slot < slots.size()) kept.place(slot, ItemStack.parseOptional(p.registryAccess(), LegacyItems.upgrade(t)));
        }
        return kept;
    }

    static void save(Player p, SimpleContainer c) {
        ListTag list = new ListTag();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putInt("Slot", i);
            list.add(s.save(p.registryAccess(), t));
        }
        p.getPersistentData().put(KEY, list);
    }

    // ── What a character has ─────────────────────────────────────────────

    /** This armament of the character's: in their armory, in hand, or worn; else empty. */
    public static ItemStack find(Player p, String id) {
        List<Armaments.Armament> slots = slots(p);
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).id().equals(id)) continue;
            ItemStack kept = of(p).getItem(i);
            if (!kept.isEmpty()) return kept;
            // what is worn counts worn, not carried in a hand; a weapon counts in hand
            List<ItemStack> about = WORN.contains(id)
                    ? List.of(p.getItemBySlot(EquipmentSlot.FEET), p.getItemBySlot(EquipmentSlot.CHEST))
                    : List.of(p.getMainHandItem(), p.getOffhandItem());
            for (ItemStack s : about) {
                if (slots.get(i).is().test(s)) return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Every one of the character's armaments they have about them (armory, hands, worn), one of each. */
    public static List<ItemStack> held(Player p) {
        List<ItemStack> out = new ArrayList<>();
        for (Armaments.Armament a : slots(p)) {
            ItemStack s = find(p, a.id());
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    // ── Cycling a weapon into hand ───────────────────────────────────────

    /** The weapons, in the order the cycle takes them (worn things and off-hand partners left out). */
    private static List<Integer> weaponSlots(List<Armaments.Armament> slots) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            String id = slots.get(i).id();
            if (!WORN.contains(id) && !PARTNER.containsValue(id)) out.add(i);
        }
        return out;
    }

    private static int slotOf(List<Armaments.Armament> slots, String id) {
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).id().equals(id)) return i;
        return -1;
    }

    /** A free inventory slot that is not the hand's own, or -1. */
    private static int freeSlotBesidesHand(Inventory inv) {
        for (int i = 0; i < inv.items.size(); i++) if (i != inv.selected && inv.items.get(i).isEmpty()) return i;
        return -1;
    }

    /**
     * The next weapon from the armory into hand: what is in hand goes back to its slot (or, if it
     * is not an armament, into the inventory), and the next weapon there comes out (the Yin sword
     * brings the Yang into the off hand). After the last, the hands are left empty. Nothing is ever
     * deleted or overwritten. Returns what is in hand after, or empty.
     */
    public static ItemStack cycle(ServerPlayer p) {
        List<Armaments.Armament> slots = slots(p);
        if (slots.isEmpty()) return ItemStack.EMPTY;
        Contents armory = of(p);
        Inventory inv = p.getInventory();
        List<Integer> weapons = weaponSlots(slots);

        // which weapon is in hand now, if one of these
        int current = -1;
        for (int k = 0; k < weapons.size(); k++) if (slots.get(weapons.get(k)).is().test(p.getMainHandItem())) current = k;
        // the next one the armory holds, or bare hands after the last
        int next = -1;
        for (int step = 1; step <= weapons.size(); step++) {
            int k = current + step;
            if (k >= weapons.size()) break;
            if (!armory.getItem(weapons.get(k)).isEmpty()) {
                next = k;
                break;
            }
        }
        if (next < 0 && current < 0) { // nothing in hand to put away, and nothing to take out: start from the first
            for (int k = 0; k < weapons.size(); k++) if (!armory.getItem(weapons.get(k)).isEmpty()) { next = k; break; }
            if (next < 0) {
                p.displayClientMessage(Component.literal("§7Your armory holds no weapons."), true);
                return ItemStack.EMPTY;
            }
        }

        // the hands: armaments back to their slots
        stowHands(p, armory, slots);
        if (next < 0) {
            p.displayClientMessage(Component.literal("§7Weapons put away."), true);
            return ItemStack.EMPTY;
        }
        int slot = weapons.get(next);
        String partner = PARTNER.get(slots.get(slot).id());
        int partnerSlot = partner == null ? -1 : slotOf(slots, partner);
        // room for what is in hand that is not an armament
        if (!clearHand(p, true) || (partnerSlot >= 0 && !armory.getItem(partnerSlot).isEmpty() && !clearHand(p, false))) {
            p.displayClientMessage(Component.literal("§cNo room to put away what you are holding."), true);
            return ItemStack.EMPTY;
        }
        inv.items.set(inv.selected, armory.removeItemNoUpdate(slot)); // into the hand, out of the armory
        if (partnerSlot >= 0 && !armory.getItem(partnerSlot).isEmpty()) {
            p.setItemSlot(EquipmentSlot.OFFHAND, armory.removeItemNoUpdate(partnerSlot));
        }
        armory.setChanged();
        ItemStack inHand = p.getMainHandItem();
        p.displayClientMessage(Component.literal("§6" + inHand.getHoverName().getString()), true);
        return inHand;
    }

    /** Puts the character's armaments in hand back into their armory slots. */
    private static void stowHands(ServerPlayer p, Contents armory, List<Armaments.Armament> slots) {
        for (int i = 0; i < slots.size(); i++) {
            if (!armory.getItem(i).isEmpty()) continue;
            Armaments.Armament a = slots.get(i);
            if (a.is().test(p.getMainHandItem())) {
                armory.place(i, p.getMainHandItem());
                p.getInventory().items.set(p.getInventory().selected, ItemStack.EMPTY);
            } else if (a.is().test(p.getOffhandItem())) {
                armory.place(i, p.getOffhandItem());
                p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        }
        armory.setChanged();
    }

    /** Moves what is in a hand into the inventory, so a weapon can go there. False if there is no room. */
    private static boolean clearHand(ServerPlayer p, boolean main) {
        ItemStack held = main ? p.getMainHandItem() : p.getOffhandItem();
        if (held.isEmpty()) return true;
        int free = freeSlotBesidesHand(p.getInventory());
        if (free < 0) return false;
        p.getInventory().items.set(free, held);
        if (main) p.getInventory().items.set(p.getInventory().selected, ItemStack.EMPTY);
        else p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        return true;
    }

    // ── The collection's strength ────────────────────────────────────────

    /** What having these armaments adds to health and attack, as a share: 6% each, and 0.5% a refinement level. */
    public static double bonus(List<ItemStack> armaments) {
        int levels = armaments.stream().mapToInt(Armaments::refine).sum();
        return armaments.size() * PER_ARMAMENT_PERCENT / 100.0 + levels * PER_REFINE_TENTHS / 1000.0;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 20 != 0) return;
        double share = slots(sp).isEmpty() ? 0 : bonus(held(sp));
        PassiveModifiers.set(sp, Attributes.MAX_HEALTH, "jttw_armory_hp", share, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PassiveModifiers.set(sp, Attributes.ATTACK_DAMAGE, "jttw_armory_atk", share, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // ── Kept through death ───────────────────────────────────────────────

    /** Their own armaments, dropped as they die, go back into the armory (where a slot is free). */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || slots(p).isEmpty()) return;
        Contents armory = of(p);
        List<Armaments.Armament> slots = slots(p);
        boolean changed = false;
        for (var it = event.getDrops().iterator(); it.hasNext(); ) {
            ItemEntity drop = it.next();
            for (int i = 0; i < slots.size(); i++) {
                if (armory.getItem(i).isEmpty() && slots.get(i).is().test(drop.getItem())) {
                    armory.place(i, drop.getItem());
                    it.remove();
                    changed = true;
                    break;
                }
            }
        }
        if (changed) armory.setChanged();
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag from = event.getOriginal().getPersistentData();
        if (from.contains(KEY)) event.getEntity().getPersistentData().put(KEY, from.getList(KEY, Tag.TAG_COMPOUND).copy());
    }
}
