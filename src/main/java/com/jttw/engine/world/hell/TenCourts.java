package com.jttw.engine.world.hell;

import java.util.List;

/**
 * The plan of the fortress of the Ten Courts of Hell (地狱十殿), pure Java, the same in every
 * world: a walled rectangle north of the Naihe, its gate at the end of the road from the arrival
 * plaza, an avenue running north from the gate through it.
 *
 * <p>The courts are visited in order along the avenue, as a soul is passed from one to the next:
 * up the west side, the First to the Fourth Court (the Office of Records stands first, by the
 * gate, where the clerks keep their ledgers); at the head of the avenue, King Yama's great hall,
 * the Fifth Court; and back down the east side, the Sixth to the Tenth, King Zhuanlun's, whose
 * courtyard holds the Wheel of Reincarnation, by the gate again, where souls go back to the world.
 *
 * <p>Every hall's door faces the avenue; its judge's bench and throne stand at the far end.
 */
public final class TenCourts {

    private TenCourts() {}

    /** The fortress's walls: the south one (with the gate, at x 0), the north one, and the sides at ±{@link #HALF_WIDTH}. */
    public static final int GATE_Z = -320, NORTH_Z = -588, HALF_WIDTH = 64;
    /** The walls' height over the ground, and their thickness. */
    public static final int WALL_HEIGHT = 11, WALL_THICK = 3;
    /** The avenue's half width. */
    public static final int AVENUE_HALF = 5;
    /** The halls of the side rows: centred this far either side of the avenue, rows this far apart from the first. */
    public static final int ROW_X = 38, FIRST_ROW_Z = -360, ROW_PITCH = 40;
    /** A side hall's half size: across (x, away from the avenue) and along (z). */
    public static final int HALL_HALF_X = 12, HALL_HALF_Z = 10;
    /** King Yama's great hall: its middle and half size. */
    public static final int YAMA_Z = -556, YAMA_HALF_X = 20, YAMA_HALF_Z = 14;
    /** The Wheel of Reincarnation's radius. */
    public static final int WHEEL_RADIUS = 6;

    /**
     * A hall: which court (0 for the Office of Records), its middle, its half size across x and
     * along z, and the way its door faces (a unit step out of it).
     */
    public record Court(int number, int x, int z, int halfX, int halfZ, int doorX, int doorZ) {

        /** Where its door is: the middle of its front wall, on the ground. */
        public int[] door() {
            return new int[]{x + doorX * halfX, z + doorZ * halfZ};
        }

        /** Where its king sits: on the throne at the far end, facing the door. */
        public int[] throne() {
            return new int[]{x - doorX * (halfX - 3), z - doorZ * (halfZ - 3)};
        }

        /** The far reach of the hall from its middle, toward the door (how deep it is). */
        public int depth() {
            return doorX != 0 ? halfX : halfZ;
        }

        /** Its half width, across the way in. */
        public int breadth() {
            return doorX != 0 ? halfZ : halfX;
        }

        /** Whether (x, z) is within its walls. */
        public boolean contains(double px, double pz) {
            return Math.abs(px - x) <= halfX && Math.abs(pz - z) <= halfZ;
        }

        /** Whether it is King Yama's. */
        public boolean isYama() {
            return number == 5;
        }

        /** The yaw a king on its throne faces (toward the door), in degrees. */
        public float throneYaw() {
            return (float) Math.toDegrees(Math.atan2(-doorX, doorZ));
        }
    }

    private static Court west(int number, int row) {
        return new Court(number, -ROW_X, FIRST_ROW_Z - ROW_PITCH * row, HALL_HALF_X, HALL_HALF_Z, 1, 0);
    }

    private static Court east(int number, int row) {
        return new Court(number, ROW_X, FIRST_ROW_Z - ROW_PITCH * row, HALL_HALF_X, HALL_HALF_Z, -1, 0);
    }

    /** The Office of Records, by the gate on the west. */
    public static final Court RECORDS = west(0, 0);

    /** The ten courts, the First to the Tenth (index n - 1). */
    public static final List<Court> COURTS = List.of(
            west(1, 1), west(2, 2), west(3, 3), west(4, 4),
            new Court(5, 0, YAMA_Z, YAMA_HALF_X, YAMA_HALF_Z, 0, 1),
            east(6, 4), east(7, 3), east(8, 2), east(9, 1), east(10, 0));

    /** Every hall: the Office of Records and the ten courts. */
    public static final List<Court> HALLS = List.of(RECORDS, COURTS.get(0), COURTS.get(1), COURTS.get(2), COURTS.get(3),
            COURTS.get(4), COURTS.get(5), COURTS.get(6), COURTS.get(7), COURTS.get(8), COURTS.get(9));

    /** The n-th court (1 to 10). */
    public static Court court(int n) {
        return COURTS.get(n - 1);
    }

    /** The middle of the Wheel of Reincarnation, in the Tenth Court's yard beside the way in (it stands upright, facing along z). */
    public static int[] wheel() {
        Court c = court(10);
        return new int[]{(AVENUE_HALF + c.x() - c.halfX()) / 2 + 1, c.z() + 7};
    }

    /** Whether (x, z) is within the fortress's walls (walls included). */
    public static boolean inside(double x, double z) {
        return Math.abs(x) <= HALF_WIDTH && z <= GATE_Z && z >= NORTH_Z;
    }

    /** How far (x, z) is outside the fortress's walls: 0 within. */
    public static double distanceOutside(double x, double z) {
        double dx = Math.max(0, Math.abs(x) - HALF_WIDTH);
        double dz = Math.max(0, Math.max(z - GATE_Z, NORTH_Z - z));
        return Math.hypot(dx, dz);
    }

    /** The hall (court or the Office of Records) whose walls hold (x, z), or null. */
    public static Court hallAt(double x, double z) {
        for (Court c : HALLS) if (c.contains(x, z)) return c;
        return null;
    }
}
