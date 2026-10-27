package com.jttw.engine.village;

/**
 * The Tang villages' money: copper cash (铜钱, one wen each), strings of cash (一串, eight wen
 * threaded on a cord) and taels of silver (银两, the boat-shaped ingot, sixty-four wen). The rates
 * are fixed, at the money-changer and the crafting table alike: eight copper to the string, eight
 * strings to the tael, so sixty-four copper to the tael either way, with no fee.
 *
 * <p>Prices are reckoned in wen and paid in silver and copper: a price of 100 wen is one tael and 36
 * copper. One trade's price fits two stacks of coins, so no price passes 64 taels and 63 wen.
 */
public final class Coins {

    private Coins() {}

    public static final int COPPER_PER_STRING = 8, STRINGS_PER_TAEL = 8, COPPER_PER_TAEL = COPPER_PER_STRING * STRINGS_PER_TAEL;
    /** The most any one trade can ask: a full stack of silver and all but a tael's worth of copper. */
    public static final int MOST = 64 * COPPER_PER_TAEL + COPPER_PER_TAEL - 1;

    /** A price as it is paid: taels of silver, and copper cash. */
    public record Price(int silver, int copper) {
        public int wen() {
            return silver * COPPER_PER_TAEL + copper;
        }
    }

    /** {@code wen} paid as silver and copper: as many taels as it holds, the rest in copper. */
    public static Price price(int wen) {
        if (wen <= 0 || wen > MOST) throw new IllegalArgumentException("no price of " + wen + " wen");
        return new Price(wen / COPPER_PER_TAEL, wen % COPPER_PER_TAEL);
    }

    /** Whether a trade can ask {@code wen} (in at most two stacks of coins). */
    public static boolean payable(int wen) {
        return wen > 0 && wen <= MOST;
    }

    /** Whether a merchant can pay out {@code wen} in one stack: up to 64 copper, or whole taels up to 64. */
    public static boolean payableInOneStack(int wen) {
        return wen > 0 && (wen <= 64 || wen % COPPER_PER_TAEL == 0 && wen / COPPER_PER_TAEL <= 64);
    }

    /** "1 tael 36 wen": a price written out. */
    public static String describe(int wen) {
        Price p = price(wen);
        if (p.silver() == 0) return p.copper() + " wen";
        return p.silver() + (p.silver() == 1 ? " tael" : " taels") + (p.copper() == 0 ? "" : " " + p.copper() + " wen");
    }
}
