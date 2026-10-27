package com.jttw.engine.village;

/**
 * What a Tang village's folk do for a living, and the two merchants of the supernatural world. The
 * mortals are the commoners the pilgrims meet on the road in the novel: the farmers of Gao Village
 * and Chen Village, the monks of the wayside temples, the scholars and petty officials, the
 * merchants and money-changers, the innkeepers who put the Tang Monk up for the night, the
 * herbalists, the fortune tellers of the market and the smiths. None of them deals in talismans:
 * those are traded only in the supernatural world, at the ghost market in Fengdu and in Heaven.
 */
public enum Calling {
    FARMER("farmer", true, true),
    MONK("monk", true, false),
    SCHOLAR("scholar", true, false),
    MERCHANT("merchant", true, true),
    INNKEEPER("innkeeper", true, true),
    HERBALIST("herbalist", true, true),
    FORTUNE_TELLER("fortune_teller", true, false),
    SMITH("smith", true, false),
    /** A dead shopkeeper still keeping a stall in the lower city of Fengdu, the ghost market (鬼市). */
    GHOST_MERCHANT("ghost_merchant", false, false),
    /** A merchant-immortal of Heaven, keeping a stall in a pavilion on the celestial islands. */
    CELESTIAL_MERCHANT("celestial_merchant", false, false);

    private final String id;
    private final boolean mortal, mayBeWoman;

    Calling(String id, boolean mortal, boolean mayBeWoman) {
        this.id = id;
        this.mortal = mortal;
        this.mayBeWoman = mayBeWoman;
    }

    public String id() {
        return id;
    }

    /** A mortal of the villages (else a merchant of the supernatural world). */
    public boolean mortal() {
        return mortal;
    }

    /** Whether a woman may follow it in the villages (a farmer's wife, an innkeeper, a silk merchant, a herbalist). */
    public boolean mayBeWoman() {
        return mayBeWoman;
    }

    public static Calling byId(String id) {
        for (Calling c : values()) if (c.id.equals(id)) return c;
        return FARMER;
    }

    public static Calling byOrdinal(int i) {
        Calling[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
