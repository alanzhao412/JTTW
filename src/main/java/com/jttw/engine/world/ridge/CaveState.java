package com.jttw.engine.world.ridge;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * What the world remembers of the White Bone Cave: whether it has one at all (else the old lair),
 * whether its great door stands open, which floors' seals are broken, and when a player was last
 * on each floor (so a floor long left alone is manned again when someone comes back to it). Kept
 * in the overworld's data.
 */
public final class CaveState extends SavedData {

    public static final String NAME = "jttw_white_bone_cave";
    public static final int UNDECIDED = 0, FORTRESS = 1, OLD_LAIR = 2;

    private int layout = UNDECIDED;
    private boolean doorOpen;
    /** Bit k: floor k's seal is broken (and with it the well's grate over floor k + 1's landing). */
    private int seals;
    /** The game time a player was last on each floor (the ninth: the throne room); 0 for never. */
    private final long[] lastSeen = new long[WhiteBoneCave.FLOORS + 1];

    public static CaveState of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CaveState::new, CaveState::load, null), NAME);
    }

    public int layout() {
        return layout;
    }

    public void decide(int layout) {
        this.layout = layout;
        setDirty();
    }

    public boolean doorOpen() {
        return doorOpen;
    }

    public void openDoor() {
        doorOpen = true;
        setDirty();
    }

    public boolean sealBroken(int k) {
        return (seals & (1 << k)) != 0;
    }

    public void breakSeal(int k) {
        seals |= 1 << k;
        setDirty();
    }

    /** Whether the well's grate over floor k's landing is open: floor k has been won (the seal above it broken). */
    public boolean grateOpen(int k) {
        return k >= 1 && sealBroken(k - 1);
    }

    public long lastSeen(int k) {
        return lastSeen[k];
    }

    public void seen(int k, long time) {
        lastSeen[k] = time;
        setDirty();
    }

    /** Forgets everything but the layout (for tests). */
    public void reset() {
        doorOpen = false;
        seals = 0;
        java.util.Arrays.fill(lastSeen, 0);
        setDirty();
    }

    private static CaveState load(CompoundTag tag, HolderLookup.Provider registries) {
        CaveState s = new CaveState();
        s.layout = tag.getInt("Layout");
        s.doorOpen = tag.getBoolean("DoorOpen");
        s.seals = tag.getInt("Seals");
        long[] seen = tag.getLongArray("LastSeen");
        System.arraycopy(seen, 0, s.lastSeen, 0, Math.min(seen.length, s.lastSeen.length));
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Layout", layout);
        tag.putBoolean("DoorOpen", doorOpen);
        tag.putInt("Seals", seals);
        tag.putLongArray("LastSeen", lastSeen);
        return tag;
    }
}
