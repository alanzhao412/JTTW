"""The Black Bear Demon (黑熊精), King of the Black Wind, and his Black Bear Soldiers (黑熊兵). As
wolfman.py (wild_paint): each one's model is written into BlackWindRenderers between its GENERATED
markers, its skin painted to the same box-UV layout at twice the model's resolution, its eyes (and the
boss's gold and jewels' glints) glowing (<name>_glow.png). A captain of the soldiers is laced in violet
(bear_soldier_captain.png).

One frame for both, an Asiatic black bear risen on its hind legs: short, massive legs and broad paws,
a barrel of a body, a great hump of shoulders, thick arms ending in clawed paws; a broad bear's head on a
heavy neck in a ruff of shaggy fur, round ears, a long tan muzzle with a wet black nose, fangs, small
eyes burning under a heavy brow, the pale crescent (月牙) on the throat. Black lamellar armour laced in
red: a breastplate and two round mirror plates, a scaled belly guard, a belt with a gold bear's-face
buckle, tassets before, behind and at the hips, tiered pauldrons each bitten by a gold bear's head,
greaves and bracers.

The King (a head and a half taller): edged in gold, a gold diadem with a flame peak on his brow, gold
rings in his ears; a black war cloak bordered in gold down his back; and over it all the stolen brocade
kasaya (锦襕袈裟), red patchwork framed in gold, thrown across his breast from the left shoulder to the
right hip, clasped with a jade ring, its end hanging at his hip; in his right hand his black-tasselled
spear (黑缨枪), a long shaft lacquered black-red, a leaf-bladed head, a great black horsehair tassel.

The soldier: plainer, red-laced, a horned iron helm with cheek guards and a red plume, a round shield
studded in bronze on his back, a great iron mace studded with spikes in his right paw.

Run from the repository root: python assets_source/black_bear.py"""
import math

import beastgen as bg
import wild_paint as w
from wild_paint import part, mirrored, aim, paint, Painter, k, mix

bg.UNITS = 384
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/BlackWindRenderers.java'
KING_SIZE, SOLDIER_SIZE = 1.0, 0.78   # BlackWindRenderers: BearKingRenderer.SIZE, BearSoldierRenderer.SIZE

FUR = (34, 30, 32)
FUR_DARK = (12, 10, 12)
FUR_SHEEN = (78, 70, 74)
FUR_TIP = (96, 86, 88)
MUZZLE = (150, 108, 70)
MUZZLE_DARK = (88, 60, 38)
NOSE = (16, 12, 14)
CRESCENT = (222, 214, 196)
FANG = (238, 228, 200)
GUM = (110, 30, 36)
CLAW = (40, 34, 30)
LAMELLA = (30, 28, 34)
IRON = (64, 62, 70)
GOLD = (228, 178, 62)
GOLD_DEEP = (142, 96, 30)
GOLD_LIGHT = (255, 228, 140)
LEATHER = (92, 34, 30)
EYE = (255, 170, 40)
EYE_KING = (255, 120, 30)
SILK = (176, 28, 30)
SILK_DEEP = (104, 14, 20)
JADE = (92, 182, 132)
CLOAK = (24, 20, 24)
LACQUER = (58, 18, 18)
STEEL = (200, 206, 216)
HORSEHAIR = (14, 12, 14)
BONE = (226, 214, 184)
BRONZE = (176, 120, 56)

LACES = {False: ((176, 30, 28), (96, 14, 18)), True: ((150, 72, 206), (78, 30, 116))}

BODY = (0.24, 0, 0)          # hunched forward over his belly
ARM, FOREARM, HAND = (-0.2, 0, 0.2), (-0.7, -0.1, 0), (0, 0, 0)


def kasaya_strips(side, W, D):
    """The kasaya over his breast (side -1) or back (+1): thrown over the left shoulder and down across him to the
    right hip, its breadth gathered in five folds fanning out from the shoulder, each lying a little out from the
    one above it and at its own angle, so the cloth falls in rolls; the uppermost carrying its gold border along
    the neck, the lowest its border and fringe; below it the cloth sagging in swags between the folds' ends."""
    out = []
    z = -D - 2.2 if side < 0 else D + 1.8
    name = 'front' if side < 0 else 'back'
    for k in range(5):
        ang = -0.74 + 0.065 * k                              # the folds fanning out from the shoulder
        ux, uy = -math.sin(ang), math.cos(ang)               # across the fold, toward his left hip
        cx, cy = 1.4 + ux * 3.3 * k, -18.8 + uy * 3.3 * k
        length = 37 - 6.4 * k
        out_ = (0.55 if k % 2 else 0.0) + 0.18 * k          # each a little proud of the last
        style = 'kasaya_edge' if k == 0 else 'kasaya_edge_low' if k == 4 else 'kasaya_fold'
        out.append(part('kasaya_%s_%d' % (name, k), 'body', (cx, cy, z + side * out_), (side * (-0.05 - 0.02 * k), 0, ang),
                        [(-length / 2, -2.2, -0.75, length, 4.4 if k < 4 else 4.8, 1.5, style)]))
    # the swags: the cloth's lower edge sagging below the last fold, in three hanging curves
    ang = -0.74 + 0.065 * 4
    ux, uy = -math.sin(ang), math.cos(ang)
    bx, by = 1.4 + ux * 3.3 * 4 + ux * 2.6, -18.8 + uy * 3.3 * 4 + uy * 2.6
    for n, (along, wd, ht) in enumerate(((-8.5, 6.0, 2.6), (-1.5, 6.6, 3.2), (5.0, 5.4, 2.4))):
        x, y = bx + math.cos(ang) * along, by + math.sin(ang) * along
        out.append(part('kasaya_%s_swag_%d' % (name, n), 'body', (x, y, z + side * 1.0), (side * -0.12, 0, ang * 0.35),
                        [(-wd / 2, -0.4, -0.6, wd, ht, 1.2, 'kasaya_swag')]))
    return out


def bear(king):
    """The frame: a black bear on its hind legs, armoured, hunched over his belly, his shoulders a great hump of
    muscle and fur. The king's grander, and robed in the kasaya."""
    W, D = (13, 9) if king else (10.5, 7.5)     # half the chest's breadth and depth
    sx = W + 3.2
    arm = 1.25 if king else 1.0
    parts = [
        # the legs: a heavy furred thigh, a greaved shin, a broad paw with its claws
        part('right_leg', 'root', (-W * 0.5, 6, 1.5), (-0.2, 0, 0.05), [(-5, -1, -5, 10, 11.5, 10, 'thigh')]),
        part('right_shin', 'right_leg', (0, 10, 0.4), (0.3, 0, 0), [
            (-4.2, 0, -4.2, 8.4, 8.6, 8.4, 'shin'),
            (-4.7, 0.4, -4.9, 9.4, 6.4, 3.4, 'greave'),
            (-4.5, -1.6, -5.3, 9, 2.8, 1.6, 'kneeguard')]),
        part('right_foot', 'right_shin', (0, 8, 0), (-0.1, 0, 0), [
            (-4.8, 0, -6.8, 9.6, 3.8, 10.4, 'paw'),
            (-4.6, 1.2, -8.3, 1.7, 2.2, 2.0, 'claw'), (-2.3, 1.2, -8.6, 1.7, 2.2, 2.2, 'claw'),
            (0.6, 1.2, -8.6, 1.7, 2.2, 2.2, 'claw'), (2.9, 1.2, -8.3, 1.7, 2.2, 2.0, 'claw')]),

        # the body from the hips up: a great belly, a barrel of a chest, the armour over them; the hump of his shoulders
        part('body', 'root', (0, 6, 1.5), BODY, [
            (-W + 1, -12, -D + 0.2, 2 * W - 2, 13, 2 * D - 0.4, 'belly'),
            (-W, -27, -D, 2 * W, 16, 2 * D, 'chest'),
            (-W - 0.7, -27.6, -D - 0.8, 2 * W + 1.4, 13.4, 2 * D + 1.6, 'breastplate'),
            (-W + 0.2, -14.4, -D - 0.5, 2 * W - 0.4, 8, 2 * D + 1, 'abdomen'),
            (-W, -6.6, -D - 0.7, 2 * W, 3.6, 2 * D + 1.4, 'belt'),
            (-3.4, -7.8, -D - 1.8, 6.8, 6, 1.4, 'buckle'),
            (-W + 1, -26.4, -D - 1.6, 6, 6, 0.8, 'mirror'),
            (W - 7, -26.4, -D - 1.6, 6, 6, 0.8, 'mirror'),
            (-W - 2.2, -35, -4, 2 * W + 4.4, 9.5, 12, 'hump'),
            (-W + 1.5, -34, -D + 0.5, 2 * W - 3, 8, 9, 'ruff'),
            (-7, -30.4, -D - 0.3, 14, 3.8, 1, 'crescent'),
        ]),
        part('right_tasset', 'body', (-W / 2, -3.4, -D - 0.7), (-0.12, 0.05, 0.05), [(-W / 2 + 0.6, 0, -0.6, W - 1.2, 11, 1.2, 'tasset')]),
        part('right_side_tasset', 'body', (-W + 0.2, -3.4, 0), (0, 0, 0.18), [(-1.2, 0, -D + 0.8, 1.2, 10, 2 * D - 1.6, 'tasset_side')]),
        part('back_tasset', 'body', (0, -3.4, D + 0.5), (0.14, 0, 0), [(-W + 1, 0, -0.6, 2 * W - 2, 11.5, 1.2, 'tasset_back')]),
        part('tail', 'body', (0, -3, D - 0.4), (-0.6, 0, 0), [(-2.6, -2.6, 0, 5.2, 5.2, 3.8, 'tailfur')]),

        # the arms: massive, furred, under tiered pauldrons; bracers; great paws
        part('right_arm', 'body', (-sx, -27.5, 0), ARM, [(-5.2 * arm / 1.25 - 0.4, -2.4, -5.0, 10 * arm / 1.25 + 0.4, 13.5, 10, 'upperarm')]),
        part('right_pauldron', 'right_arm', (-0.8, -2.6, 0), (0, 0, 0.42), [
            (-6.0, -1.8, -6.0, 9.6, 2.2, 12.0, 'pauldron1'),
            (-6.8, 0.4, -6.6, 10.4, 2.4, 13.2, 'pauldron2'),
            (-7.6, 2.8, -7.2, 11, 2.6, 14.4, 'pauldron3'),
            (-8.4, 0, -2.9, 1.2, 5.6, 5.8, 'bearface')]),
        part('right_forearm', 'right_arm', (0, 10.4, 0), FOREARM, [
            (-4.4, 0, -4.4, 8.8, 10.5, 8.8, 'forearm'),
            (-4.9, 1.4, -4.9, 9.8, 6.8, 9.8, 'bracer'),
            (-5.4, -1.0, -3.0, 2, 3.4, 6, 'elbowfur')]),
        part('right_hand', 'right_forearm', (0, 10, 0), HAND, [
            (-4.6, 0, -4.8, 9.2, 5.6, 9.6, 'hand'),
            (-4.6, 4.8, -5.2, 1.8, 2.8, 1.8, 'claw'), (-2.2, 4.8, -5.4, 1.8, 3.0, 1.8, 'claw'),
            (0.4, 4.8, -5.4, 1.8, 3.0, 1.8, 'claw'), (2.8, 4.8, -5.2, 1.8, 2.8, 1.8, 'claw')]),

        # the head: a broad domed skull, heavy jowls, a long muzzle tapering to a wet black nose
        part('head', 'body', (0, -33.5, -4), (-0.12, 0, 0), [
            (-7.6, -11.4, -6.8, 15.2, 11, 12.6, 'skull'),
            (-6.0, -13.4, -5.4, 12, 2.4, 9.6, 'dome'),
            (-5.2, -6.8, -10.2, 10.4, 6.6, 4.2, 'muzzle_base'),
            (-4.0, -6.2, -13.6, 8, 5.4, 3.8, 'muzzle'),
            (-3.0, -5.6, -16.2, 6, 4.4, 3.0, 'muzzle_tip'),
            (-2.6, -6.4, -17.2, 5.2, 3.0, 1.8, 'nose'),
            (-3.4, -1.6, -16.0, 6.8, 1.6, 5.8, 'lip'),
            (-3.0, -1.0, -15.4, 1.1, 2.6, 1.1, 'fang'), (1.9, -1.0, -15.4, 1.1, 2.6, 1.1, 'fang'),
            (-7.2, -14.6, -4.4, 14.4, 2.6, 4, 'forelock'),
        ]),
        part('bridge', 'head', (0, -6.6, -9.4), (-0.18, 0, 0), [(-3.2, -1.4, -6.6, 6.4, 1.6, 6.8, 'bridge')]),
        part('right_jowl', 'head', (-7.0, -4.6, -4.6), (0, 0.25, 0.22), [(-2.6, -2.8, -3.6, 3.4, 6.8, 8.6, 'jowl')]),
        part('right_face_ruff', 'head', (-7.6, -8.4, 1.6), (0, -0.35, 0.3), [(-3.0, -4.0, -1.2, 3.2, 9.6, 5.2, 'face_ruff')]),
        part('jaw', 'head', (0, -0.6, -6.4), (0.16, 0, 0), [
            (-4.0, 0, -9.0, 8, 2.9, 9.2, 'jaw'),
            (-3.7, -1.0, -8.8, 7.4, 1.0, 1.2, 'lowteeth'),
            (-3.2, -2.4, -8.4, 1, 2.4, 1, 'fang'), (2.2, -2.4, -8.4, 1, 2.4, 1, 'fang')]),
        part('right_ear', 'head', (-6.0, -11.8, -0.8), (-0.12, 0.25, -0.36), [
            (-2.8, -4.4, -1.3, 5.6, 4.6, 2.6, 'ear'), (-2.0, -5.6, -1.1, 4, 1.4, 2.2, 'ear_top')]),
        part('right_brow', 'head', (-0.6, -10.2, -7.0), (0.1, 0.15, 0.3), [(-5.6, -1.3, -1.2, 5.8, 2.4, 2.6, 'brow')]),
        part('right_eye', 'head', (-3.7, -8.2, -6.9), (0, 0.22, 0), [(-1.3, -0.7, -0.5, 2.6, 1.5, 0.7, 'eye')]),
    ]
    if king:
        parts += [
            # his diadem of gold, a flame peak over the brow; rings in his ears
            part('crown', 'head', (0, -12.6, -0.5), (0, 0, 0), [
                (-7.6, -1.4, -6.8, 15.2, 2, 13.2, 'crown_band'),
                (-1.7, -7.4, -7.4, 3.4, 6, 1.2, 'crown_peak'),
                (-6.6, -4.4, -7.1, 2.6, 3.2, 1, 'crown_plate'), (4.0, -4.4, -7.1, 2.6, 3.2, 1, 'crown_plate'),
                (-0.9, -4.0, -7.8, 1.8, 1.8, 0.6, 'crown_gem')]),
            part('right_earring', 'right_ear', (-2.2, -0.4, 0), (0, 0, 0), [(-0.3, 0, -1.2, 0.6, 2.6, 2.4, 'ring')]),
            # the war cloak, black, gold-bordered, from his shoulders under the kasaya
            part('cloak', 'body', (0, -32, D + 2.4), (0.14, 0, 0), [(-W - 1.5, 0, -0.6, 2 * W + 3, 19, 1.2, 'cloak')]),
            part('cloak_lower', 'cloak', (0, 18.6, 0), (0.06, 0, 0), [(-W - 2.2, 0, -0.6, 2 * W + 4.4, 17, 1.2, 'cloak_lower')]),
            # the kasaya: over the left shoulder, across breast and back to the right hip in folding bands; its
            # skirt hanging from the belt over his left side and before him in folds; its corner falling from the
            # shoulder down his front; its long fall down his back from the shoulder, lifting as he moves
            # over the shoulder the cloth rounds in three layers, its gold border at its edge
            part('kasaya_shoulder', 'body', (W + 1.8, -30.5, 0), (0, 0, -0.38), [(-5.5, -1.6, -D - 3.0, 10, 3.2, 2 * D + 6.0, 'kasaya_drape'),
                                                                                (-4.3, -2.7, -D - 2.2, 7.6, 1.3, 2 * D + 4.4, 'kasaya_drape_top'),
                                                                                (-5.5, 1.4, -D - 3.0, 2.0, 5, 2 * D + 6.0, 'kasaya_drape')]),
            part('kasaya_shoulder_fall', 'kasaya_shoulder', (-5.3, 1.2, 0), (0, 0, 0.32), [(-0.7, 0, -D - 2.6, 1.4, 6.5, 2 * D + 5.2, 'kasaya_side')]),
            # its skirt from the belt over his left side in three pleated panels, each at its own angle and
            # length, hemmed in gold over a fringe; the side panel wrapping round his hip
            part('kasaya_skirt', 'body', (W * 0.35, -4.4, -D - 2.4), (-0.12, 0, -0.04), [(-W * 0.3, 0, -0.7, W * 0.6, 9, 1.4, 'kasaya_pleat')]),
            part('kasaya_skirt_hem', 'kasaya_skirt', (0, 8.6, 0), (-0.14, 0, 0), [(-W * 0.32, 0, -0.7, W * 0.64, 8.6, 1.4, 'kasaya_hem')]),
            part('kasaya_pleat_in', 'kasaya_skirt', (-W * 0.5, 0.3, 0.5), (-0.03, 0.2, 0.06), [(-W * 0.26, 0, -0.6, W * 0.52, 8.6, 1.2, 'kasaya_pleat_dark')]),
            part('kasaya_pleat_in_hem', 'kasaya_pleat_in', (0, 8.3, 0), (-0.1, 0, 0), [(-W * 0.27, 0, -0.6, W * 0.54, 7.4, 1.2, 'kasaya_hem')]),
            part('kasaya_pleat_out', 'kasaya_skirt', (W * 0.52, 0.2, 0.6), (0.0, -0.22, -0.06), [(-W * 0.26, 0, -0.6, W * 0.52, 8.8, 1.2, 'kasaya_pleat_dark')]),
            part('kasaya_pleat_out_hem', 'kasaya_pleat_out', (0, 8.5, 0), (-0.1, 0, 0), [(-W * 0.27, 0, -0.6, W * 0.54, 8.2, 1.2, 'kasaya_hem')]),
            part('kasaya_side', 'body', (W + 0.8, -4.4, 0), (0, 0, -0.1), [(-0.7, 0, -D - 1.6, 1.4, 15, 2 * D + 2.8, 'kasaya_side')]),
            # its corner falling from the shoulder down his front, in two layers, the under one longer
            part('kasaya_tail', 'body', (W - 2.0, -24.5, -D - 2.6), (-0.08, 0, 0.04), [(-3.2, 0, -0.6, 6.4, 22, 1.2, 'kasaya_end')]),
            part('kasaya_tail_over', 'kasaya_tail', (1.2, -0.4, -0.9), (-0.04, 0, -0.06), [(-2.4, 0, -0.5, 4.8, 13, 1.0, 'kasaya_end_short')]),
            # its long fall down his back from the shoulder, in two lengths
            part('kasaya_cape', 'body', (W - 3, -31, D + 2.6), (0.16, 0, -0.06), [(-6.5, 0, -0.6, 13, 16, 1.2, 'kasaya_cape')]),
            part('kasaya_cape_lower', 'kasaya_cape', (0, 15.6, 0), (0.08, 0, 0), [(-7, 0, -0.6, 14, 14, 1.2, 'kasaya_hem')]),
            part('jade_ring', 'body', (W - 3.0, -27.0, -D - 3.4), (0, 0, 0), [(-2.2, -2.2, -0.6, 4.4, 4.4, 1.2, 'jade')]),
        ] + kasaya_strips(-1, W, D) + kasaya_strips(1, W, D) + [
            # the black-tasselled spear, upright in his right paw: a leaf blade with its spine, a gold beast's
            # mouth biting it, a guard, and under it a great tassel of red horsehair
            part('spear', 'right_hand', (0, 2.8, -0.2), aim([BODY, ARM, FOREARM, HAND], (0.02, 0, 0)), [
                (-0.8, -48, -0.8, 1.6, 74, 1.6, 'shaft'),
                (-1.4, -50, -1.4, 2.8, 2.4, 2.8, 'collar'),
                (-2.4, -51.6, -0.7, 4.8, 1.8, 1.4, 'guard'),
                (-1.3, -54.4, -1.3, 2.6, 3, 2.6, 'beastmouth'),
                (-3.0, -70, -0.45, 6, 16, 0.9, 'spearhead'),
                (-0.5, -70.6, -0.9, 1, 17, 1.8, 'spine'),
                (-1.2, 24, -1.2, 2.4, 3.2, 2.4, 'butt'),
                (-1.1, -30, -1.1, 2.2, 1.2, 2.2, 'band'), (-1.1, 8, -1.1, 2.2, 1.2, 2.2, 'band')]),
            part('tassel', 'spear', (0, -48.4, 0), (0, 0, 0), [(-1.8, 0, -1.8, 3.6, 2.2, 3.6, 'knot')]),
        ]
        for k in range(8):
            a = k * math.pi / 4
            parts.append(part('tassel_strand_%d' % k, 'tassel', (math.cos(a) * 1.3, 1.8, math.sin(a) * 1.3),
                              (math.sin(a) * 0.32, 0, -math.cos(a) * 0.32), [(-0.75, 0, -0.75, 1.5, 9 + (k % 3) * 1.5, 1.5, 'tassel_strand')]))
    else:
        parts += [
            # a horned iron helm, cheek guards, a red plume
            part('helm', 'head', (0, -12.6, -0.5), (0, 0, 0), [
                (-7.8, -2.6, -7.0, 15.6, 3.8, 13.6, 'helm_bowl'),
                (-7.0, -4.6, -5.8, 14, 2.2, 11.4, 'helm_top'),
                (-8.0, 0.6, -7.1, 16, 1.2, 13.8, 'helm_rim'),
                (-1.2, -7.6, -1.2, 2.4, 3.2, 2.4, 'plume_socket'),
                (-8.6, 0.8, -5.2, 1.4, 6.4, 6.4, 'cheekguard'), (7.2, 0.8, -5.2, 1.4, 6.4, 6.4, 'cheekguard')]),
            part('plume', 'helm', (0, -7.2, 0.6), (0.7, 0, 0), [(-1.3, -1, 0, 2.6, 2.6, 8, 'plume')]),
            part('right_horn', 'helm', (-6.8, -2.6, -2), (0, 0.2, -0.7), [(-1.1, -6.5, -1.1, 2.2, 6.5, 2.2, 'horn'), (-0.8, -9.8, -0.7, 1.6, 3.6, 1.6, 'horn_tip')]),
            part('shield', 'body', (0, -20, D + 0.9), (0.1, 0, 0), [(-8.5, -8.5, 0, 17, 17, 1.4, 'shield'), (-2, -2, 1.2, 4, 4, 1, 'shield_boss')]),
            part('mace', 'right_hand', (0, 2.8, -0.2), aim([BODY, ARM, FOREARM, HAND], (0.5, 0, 0)), [
                (-0.9, -21, -0.9, 1.8, 30, 1.8, 'haft'),
                (-3.0, -33, -3.0, 6, 12, 6, 'macehead'),
                (-3.7, -31, -0.55, 7.4, 1.1, 1.1, 'spike'), (-0.55, -31, -3.7, 1.1, 1.1, 7.4, 'spike'),
                (-3.7, -25.5, -0.55, 7.4, 1.1, 1.1, 'spike'), (-0.55, -25.5, -3.7, 1.1, 1.1, 7.4, 'spike'),
                (-0.6, -35.4, -0.6, 1.2, 2.6, 1.2, 'spike_top'),
                (-1.3, 7.6, -1.3, 2.6, 2.2, 2.6, 'pommel')]),
        ]
    out = mirrored(parts)
    return w.settle(out, ('right_foot', 'left_foot'))


KING = bear(True)
SOLDIER = bear(False)


# ── The skin ─────────────────────────────────────────────────────────────────

def pelt(p, f, base=FUR, faces=None, density=0.5):
    """Shaggy black fur, a brown-grey sheen on its tips, darker beneath."""
    for s in faces or f:
        b = base if s != 'bottom' else FUR_DARK
        w.strands(p, f[s], k(b, w.LIGHT[s]), k(FUR_DARK, w.LIGHT[s]), k(FUR_TIP, w.LIGHT[s] * (1.0 if s != 'back' else 0.8)),
                  density=density, lean=0.15)


def lamellae(p, f, lace, faces=None, rows=3, cols=2, rim=None):
    for s in faces or w.SIDES:
        w.lamellar(p, f[s], LAMELLA, lace, rows=rows, cols=cols, light=w.LIGHT[s], rim=rim or k(LAMELLA, 2.3))


def gold_rim(p, f, faces=None, top=True, bottom=True, colour=GOLD):
    for s in faces or w.SIDES:
        fx, fy, fw, fh = f[s]
        for i in range(fw):
            if top:
                p.px((fx + i, fy), k(colour, w.LIGHT[s] * 1.05))
            if bottom:
                p.px((fx + i, fy + fh - 1), k(colour, w.LIGHT[s] * 0.8))


def bear_face(p, rect, base=GOLD, deep=GOLD_DEEP, glow=False):
    """A gold bear's face: round ears, a heavy brow, eyes, a snout and a snarl of teeth."""
    fx, fy, fw, fh = rect
    for i, j, xy in w.each(p, rect):
        p.px(xy, [v * (1.05 - 0.25 * j / max(1, fh)) + p.rnd.randint(-6, 6) for v in base])
    ear = max(1, fw // 5)
    for i in range(ear):
        p.px((fx + i, fy), deep)
        p.px((fx + fw - 1 - i, fy), deep)
    ey = max(1, fh // 3)
    for ex in (max(0, fw // 4 - 1), min(fw - 2, fw - fw // 4 - 1)):
        p.px((fx + ex, fy + ey), (40, 16, 10))
        if glow:
            p.lit((fx + ex + 1, fy + ey), (255, 120, 40), 0.8)
        else:
            p.px((fx + ex + 1, fy + ey), (200, 50, 30))
        p.px((fx + ex, fy + ey - 1), deep)
        p.px((fx + ex + 1, fy + ey - 1), deep)
    for j in range(ey + 1, fh - 2):
        p.px((fx + fw // 2, fy + j), deep)
    for i in range(max(1, fw // 4), fw - max(1, fw // 4)):
        p.px((fx + i, fy + fh - 2), (40, 16, 10))
        if i % 2 == 0:
            p.px((fx + i, fy + fh - 1), (240, 228, 190))


def brocade(p, rect, light=1.0, cell=6):
    """The kasaya's patchwork: panels of red silk, each its own shade, framed in gold thread (the rice-field
    pattern, 田相), a little gold flower worked in each panel's middle."""
    fx, fy, fw, fh = rect
    for i, j, xy in w.each(p, rect):
        ci, cj = i // cell, j // cell
        tone = 0.86 + 0.24 * w._hash(ci, cj, 77)
        u, v = i % cell, j % cell
        if u == 0 or v == 0:
            c = k(GOLD, 0.95 + 0.1 * ((i + j) % 2))
        elif u == cell // 2 and v == cell // 2:
            c = GOLD_LIGHT
        elif (abs(u - cell // 2) + abs(v - cell // 2)) == 1:
            c = mix(GOLD, SILK, 0.4)
        else:
            c = mix(SILK_DEEP, SILK, 0.5 + 0.5 * w.vnoise(fx + i, fy + j, 2.0, 5))
            c = k(c, tone)
        p.px(xy, [x * light + p.rnd.randint(-3, 3) for x in c])


def patchwork(p, rect, light=1.0, cell=10):
    """The kasaya's patchwork as it is sewn (the rice-field pattern, 田相): broad panels of red silk, each its
    own shade and with a faint damask of its own, divided by narrow strips of deep wine-red (not gold: from a
    step away it must read as one cloth, its gold kept for its border); a small gold flower worked in every
    other panel."""
    fx, fy, fw, fh = rect
    for i, j, xy in w.each(p, rect):
        ci, cj = (i + fx) // cell, (j + fy) // (cell + 2)
        u, v = (i + fx) % cell, (j + fy) % (cell + 2)
        tone = 0.84 + 0.26 * w._hash(ci, cj, 77)
        if u == 0 or v == 0:
            c = mix(SILK_DEEP, (70, 10, 14), 0.4)
        elif u == 1 or v == 1:
            c = mix(SILK_DEEP, SILK, 0.35)                        # the seam's shadow
        else:
            c = mix(SILK_DEEP, SILK, 0.55 + 0.45 * w.vnoise(fx + i, fy + j, 3.0, 5))
            if (u + v) % 4 == 0:
                c = k(c, 1.06)                                    # the damask's glint
            c = k(c, tone)
            if (ci + cj) % 2 == 0:
                du, dv = u - cell // 2, v - (cell + 2) // 2
                if abs(du) + abs(dv) == 0:
                    c = GOLD_LIGHT
                elif abs(du) + abs(dv) == 1:
                    c = mix(GOLD, SILK, 0.25)
        p.px(xy, [x * light + p.rnd.randint(-3, 3) for x in c])


def gold_scroll(p, rect):
    """A running scroll of dark gold worked along a gold border."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        j = int((fh - 1) * (0.5 + 0.5 * math.sin(i * 0.9)))
        p.px((fx + i, fy + j), GOLD_DEEP)


def paint_bear(king, lace):
    lace_c, lace_d = lace

    def paint_box(p, style, f, dims):
        S = w.SIDES
        # the second pass's: the bear's dome, jowls and ruffs; the kasaya's bands and folds; the spear's beast and tassel
        if style in ('dome', 'jowl', 'face_ruff', 'elbowfur'):
            pelt(p, f, base=mix(FUR, FUR_SHEEN, 0.25) if style != 'dome' else FUR, density=0.6)
            for s in S:
                w.tufts(p, f[s], depth=3 if style == 'dome' else 4, every=2, down=style != 'dome')
            if style == 'jowl':                                   # the brown of the muzzle creeping onto the jowl
                fx, fy, fw, fh = f['front']
                for i, j, xy in w.each(p, f['front']):
                    if j > fh * 0.5:
                        p.px(xy, mix(p.get(xy), MUZZLE_DARK, 0.5))
        if style == 'ear_top':
            pelt(p, f, density=0.5)
            for s in S + ('top',):
                fx, fy, fw, fh = f[s]
                p.clear((fx, fy))
                p.clear((fx + fw - 1, fy))
        if style in ('kasaya_edge', 'kasaya_edge_low', 'kasaya_fold', 'kasaya_swag', 'kasaya_pleat', 'kasaya_pleat_dark',
                     'kasaya_end_short', 'kasaya_cape', 'kasaya_drape_top'):
            # the cloth rolled into a fold: lit along its crest, falling into shadow toward its lower edge and
            # dark in the crease beneath; its sides (the cloth's thickness) shadowed
            dark = style == 'kasaya_pleat_dark'
            for s_ in f:
                fx, fy, fw, fh = f[s_]
                patchwork(p, f[s_], light=w.LIGHT[s_] * (0.86 if dark else 1.0))
                for i, j, xy in w.each(p, f[s_]):
                    t = j / max(1, fh - 1)
                    if style in ('kasaya_pleat', 'kasaya_pleat_dark', 'kasaya_end_short', 'kasaya_cape'):
                        u = (i + 0.5) / fw                         # a pleat falling: its crest down its middle
                        p.px(xy, k(p.get(xy), 0.78 + 0.32 * math.sin(u * math.pi)))
                    elif style == 'kasaya_swag':                   # a swag: hanging in a curve, its belly lit
                        u = (i + 0.5) / fw
                        p.px(xy, k(p.get(xy), 0.75 + 0.35 * math.sin(u * math.pi) * (1 - 0.5 * t)))
                    else:
                        p.px(xy, k(p.get(xy), 1.16 - 0.5 * t * t))
                if s_ in ('right', 'left', 'top', 'bottom'):
                    w.flat(p, {s_: f[s_]}, k(SILK_DEEP, w.LIGHT[s_] * 0.8))
            for s_ in ('front', 'back'):
                fx, fy, fw, fh = f[s_]
                if style == 'kasaya_edge':                         # its border along the neck: a broad gold band, a dark rule
                    for j in range(2):
                        w.hline(p, f[s_], j, GOLD_LIGHT if j == 0 else GOLD)
                    w.hline(p, f[s_], 2, GOLD_DEEP)
                    gold_scroll(p, (fx, fy, fw, 3))
                if style in ('kasaya_edge_low', 'kasaya_swag', 'kasaya_cape', 'kasaya_end_short', 'kasaya_pleat',
                             'kasaya_pleat_dark'):
                    edge = fh - 3 if style != 'kasaya_swag' else fh - 2
                    w.hline(p, f[s_], edge - 1, GOLD_DEEP)
                    for j in range(edge, fh):
                        w.hline(p, f[s_], j, GOLD if j < fh - 1 else GOLD_DEEP)
                    if style == 'kasaya_edge_low':
                        gold_scroll(p, (fx, fy + edge, fw, 2))
                if style in ('kasaya_end_short', 'kasaya_cape', 'kasaya_pleat', 'kasaya_pleat_dark'):
                    w.vline(p, f[s_], 0, GOLD)
                    w.vline(p, f[s_], fw - 1, GOLD_DEEP)
            if style == 'kasaya_drape_top':
                w.border(p, f, GOLD, width=1)
        if style in ('kasaya_band', 'kasaya_band_seam', 'kasaya_hem', 'kasaya_side'):
            for s in f:
                fx, fy, fw, fh = f[s]
                patchwork(p, f[s], light=w.LIGHT[s])
                for i, j, xy in w.each(p, f[s]):              # the fold: the cloth rounding over, shadowed beneath
                    t = j / max(1, fh - 1)
                    p.px(xy, k(p.get(xy), 1.12 - 0.42 * t * t))
                if s in ('front', 'back'):
                    w.hline(p, f[s], 0, GOLD_LIGHT)
                    if style == 'kasaya_band_seam':
                        w.hline(p, f[s], fh // 2, GOLD)
                    w.vline(p, f[s], 0, GOLD_LIGHT)
                    w.vline(p, f[s], fw - 1, GOLD_DEEP)
                    if style == 'kasaya_hem':                     # a hem of gold, a fringe below it
                        w.hline(p, f[s], fh - 2, GOLD)
                        for i in range(fw):
                            p.px((fx + i, fy + fh - 1), GOLD_LIGHT if i % 2 == 0 else SILK_DEEP)
            w.flat(p, f, GOLD_DEEP, faces=('right', 'left'))
        if style == 'beastmouth':                                 # a gold beast's mouth (吞口) biting the blade's root
            w.iron(p, f, GOLD, rivets=0, rim=False, dents=0)
            for s in w.SIDES:
                bear_face(p, f[s], glow=False)
        if style == 'spine':                                      # the blade's raised spine
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    p.px(xy, k(mix(STEEL, (250, 252, 255), 0.3 if i == 0 else 0), w.LIGHT[s]))
            for s in w.SIDES:
                w.point(p, f[s], down=False, keep=0.2, sharp=0.4)
        if style == 'knot':
            w.iron(p, f, GOLD, rivets=0, dents=0)
            for s in w.SIDES:
                w.hline(p, f[s], f[s][3] - 1, (150, 20, 24))
        if style == 'tassel_strand':                              # the tassel's red horsehair, darker at the root, ragged at the end
            for s in f:
                w.strands(p, f[s], k((196, 30, 32), w.LIGHT[s]), k((96, 10, 16), w.LIGHT[s]), k((240, 90, 70), w.LIGHT[s]), density=0.5)
            for s in w.SIDES:
                w.tufts(p, f[s], depth=3, every=2)
        if style in ('thigh', 'shin', 'upperarm', 'forearm', 'belly', 'tailfur', 'hand', 'paw'):
            pelt(p, f, density=0.55)
            if style == 'paw':
                w.flat(p, f, (40, 30, 30), faces=('bottom',))
                fx, fy, fw, fh = f['front']                      # the pads and toes' clefts
                for i in range(0, fw, max(1, fw // 4)):
                    w.vline(p, f['front'], i, FUR_DARK, fh // 3)
            if style == 'tailfur':
                for s in S:
                    w.tufts(p, f[s], depth=3, every=2)
        if style == 'claw':
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    p.px(xy, k(mix(CLAW, (150, 140, 130), 0.4 * (j / max(1, fh))), w.LIGHT[s]))
            for s in S:
                w.point(p, f[s], down=True, keep=0.3, sharp=0.6)
        if style == 'chest':
            pelt(p, f)
        if style == 'ruff':                                       # the shaggy ruff about his neck and shoulders
            pelt(p, f, base=mix(FUR, FUR_SHEEN, 0.35), density=0.6)
            for s in S:
                w.tufts(p, f[s], depth=5, every=3)
        if style == 'hump':
            pelt(p, f, base=mix(FUR, FUR_SHEEN, 0.2), density=0.6)
            for s in ('front', 'right', 'left'):
                w.tufts(p, f[s], depth=4, every=3)
        if style == 'crescent':                                   # the pale crescent on his throat, its horns up
            pelt(p, f)
            fx, fy, fw, fh = f['front']
            cx = fw / 2
            for i, j, xy in w.each(p, f['front']):
                t = (i - cx + 0.5) / cx
                arc = (fh - 2.2) * (1 - t * t)                    # the crescent's lower edge rising to its horns
                if abs(t) < 0.98 and arc - 1.6 <= (fh - 1 - j) <= arc + 0.8 * (1 - abs(t)) + 0.6:
                    p.px(xy, mix(CRESCENT, (180, 170, 150), p.rnd.random() * 0.3))
        if style in ('breastplate', 'abdomen'):
            lamellae(p, f, lace_c, rows=3 if style == 'breastplate' else 2)
            w.flat(p, f, LAMELLA, faces=('top', 'bottom'))
            if king:
                gold_rim(p, f)
            else:
                w.border(p, f, k(IRON, 1.4), bottom=False)
            if style == 'breastplate':                            # straps over his shoulders, studded
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for x in (4, fw - 7):
                        for j in range(0, fh // 2):
                            for t in range(3):
                                p.px((fx + x + t, fy + j), k(LEATHER, 1.1 * w.LIGHT[s]))
                        w.rivet(p, fx + x, fy + 2, GOLD if king else BRONZE)
        if style == 'mirror':                                     # the round mirror plates (护心镜)
            w.flat(p, f, LAMELLA)
            fx, fy, fw, fh = f['front']
            m = GOLD if king else (170, 176, 186)
            w.disc(p, fx + fw / 2, fy + fh / 2, fw / 2 - 0.2, k(m, 0.9), rim=k(m, 0.6))
            w.disc(p, fx + fw / 2, fy + fh / 2, fw / 3, k(m, 1.15))
            p.lit((fx + fw / 2 - 1, fy + fh / 2 - 1), (255, 250, 220), 0.5)
        if style == 'belt':                                       # red leather, studded in gold (bronze)
            w.leather(p, f, LEATHER, stitch=GOLD_DEEP)
            for s in S:
                fx, fy, fw, fh = f[s]
                for i in range(1, fw - 1, 3):
                    w.rivet(p, fx + i, fy + fh // 2 - 1, GOLD if king else BRONZE)
        if style == 'buckle':
            w.flat(p, f, GOLD_DEEP)
            bear_face(p, f['front'], glow=king)
        if style in ('tasset', 'tasset_side', 'tasset_back'):
            lamellae(p, f, lace_c, rows=3)
            for s in S:
                fx, fy, fw, fh = f[s]
                for i in range(fw):                               # a hem of red, a fringe
                    p.px((fx + i, fy + fh - 1), lace_d)
                    p.px((fx + i, fy + fh - 2), lace_c if i % 2 == 0 else lace_d)
                    p.px((fx + i, fy), k(GOLD if king else IRON, w.LIGHT[s] * (1.0 if king else 1.5)))
        if style in ('pauldron1', 'pauldron2', 'pauldron3'):
            lamellae(p, f, lace_c, faces=S + ('top',), rows=3)
            gold_rim(p, f, top=False, colour=GOLD if king else k(IRON, 1.6))
        if style == 'bearface':
            w.iron(p, f, GOLD if king else BRONZE, rivets=0, rim=False, dents=0.05)
            bear_face(p, f['right'], base=GOLD if king else BRONZE, glow=king)
            bear_face(p, f['left'], base=GOLD if king else BRONZE, glow=king)
        if style in ('greave', 'bracer', 'kneeguard'):
            w.iron(p, f, IRON if not king else (40, 38, 44), rivets=3 if style != 'kneeguard' else 0, dents=0.15)
            if king:
                gold_rim(p, f)
            for s in ('front',):
                fx, fy, fw, fh = f[s]
                for j in range(1, fh - 1, 2):
                    p.px((fx + 1, fy + j), lace_c)
                    p.px((fx + fw - 2, fy + j), lace_c)
            if style == 'kneeguard':
                bear_face(p, f['front'], base=GOLD if king else BRONZE)

        # the head
        if style in ('skull', 'cheek', 'forelock'):
            pelt(p, f, density=0.45)
            if style in ('cheek', 'forelock'):
                for s in S:
                    w.tufts(p, f[s], depth=4, every=2, down=style == 'cheek')
            if style == 'skull':
                fx, fy, fw, fh = f['front']                       # a brown-grey mask about the eyes, lighter toward the muzzle
                for i, j, xy in w.each(p, f['front']):
                    if j > fh * 0.45 and abs(i - fw / 2) < fw * 0.3:
                        p.px(xy, mix(p.get(xy), MUZZLE_DARK, 0.45))
        if style == 'brow':                                       # the heavy scowling brow
            pelt(p, f, base=FUR_DARK, density=0.7)
            fx, fy, fw, fh = f['front']
            for i in range(fw):                                   # knit in a V over the snout
                d = abs(i - fw / 2) / (fw / 2)
                for j in range(fh):
                    if j > fh - 1 - (1 - d) * 2:
                        p.px((fx + i, fy + j), k(FUR_DARK, 0.6))
        if style in ('muzzle', 'bridge', 'muzzle_tip', 'lip', 'muzzle_base'):                         # the tan muzzle, darker along its lips, whisker spots
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    t = j / max(1, fh - 1)
                    c = mix(MUZZLE, MUZZLE_DARK, 0.25 + 0.5 * t if s in ('right', 'left') else 0.2)
                    p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-5, 5) for v in c])
            if style in ('muzzle', 'muzzle_tip', 'muzzle_base'):
                for s in ('right', 'left'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):
                        p.px((fx + i, fy + fh - 1), (30, 18, 16))
                        if i % 3 == 1:
                            p.px((fx + i, fy + fh - 3), (60, 38, 26))
                    for i in range(1, fw - 1, 3):                 # its upper fangs showing at the lip
                        p.px((fx + i, fy + fh - 1), FANG if i < fw // 2 else (30, 18, 16))
                w.flat(p, f, GUM, faces=('bottom',))
                fx, fy, fw, fh = f['bottom']
                for i in range(fw):
                    p.px((fx + i, fy), FANG if i % 2 == 0 else GUM)
                fx, fy, fw, fh = f['front']
                for i in range(fw):
                    p.px((fx + i, fy + fh - 1), (30, 18, 16))
        if style == 'lip':
            w.flat(p, f, (40, 26, 22), faces=('front', 'right', 'left', 'bottom'))
        if style == 'nose':                                       # the wet black nose, its nostrils, a gleam
            w.flat(p, f, NOSE)
            fx, fy, fw, fh = f['top']
            for i in range(1, fw - 1):
                p.px((fx + i, fy + fh // 2), (150, 146, 158))
            fx, fy, fw, fh = f['front']
            p.px((fx + 1, fy + fh - 2), (2, 2, 2))
            p.px((fx + fw - 2, fy + fh - 2), (2, 2, 2))
            p.px((fx + fw // 2, fy + fh - 1), (4, 2, 2))
            p.px((fx + 1, fy), (130, 120, 130))
            p.px((fx + 2, fy), (100, 92, 100))
        if style == 'jaw':
            pelt(p, f, base=mix(FUR, MUZZLE_DARK, 0.4), density=0.4)
            fx, fy, fw, fh = f['top']
            for i, j, xy in w.each(p, f['top']):
                p.px(xy, GUM if 0 < i < fw - 1 else (FANG if j % 2 == 0 else (60, 20, 24)))
            w.teeth_row(p, f['top'], 0, FANG, GUM, every=3, rows=1)
        if style == 'lowteeth':
            for s in f:
                w.teeth_row(p, f[s], 0, FANG, GUM, every=2, rows=min(2, f[s][3]))
        if style == 'fang':
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    p.px(xy, k(mix(FANG, (190, 170, 130), j / max(1, fh)), w.LIGHT[s]))
            for s in S:
                w.point(p, f[s], down=True, keep=0.3, sharp=0.6)
        if style == 'ear':                                        # round ears, furred, tan within
            pelt(p, f, density=0.5)
            fx, fy, fw, fh = f['front']
            for i, j, xy in w.each(p, f['front']):
                d = math.hypot((i - fw / 2 + 0.5) / (fw / 2), (j - fh * 0.6) / (fh * 0.6))
                if d < 0.6:
                    p.px(xy, mix(MUZZLE_DARK, (70, 44, 36), d))
            for s in S + ('top',):                                # rounded off
                fx, fy, fw, fh = f[s]
                for i in (0, fw - 1):
                    p.clear((fx + i, fy))
        if style == 'eye':                                        # small eyes burning amber under the brow
            pelt(p, f)
            fx, fy, fw, fh = f['front']
            for i, j, xy in w.each(p, f['front']):
                c = mix(EYE_KING if king else EYE, (255, 236, 160), 0.5 if j == fh // 2 else 0)
                p.lit(xy, c)
            p.px((fx + fw // 2, fy + fh // 2), (24, 6, 4))
            p.glow.putpixel((fx + fw // 2, fy + fh // 2), (0, 0, 0, 0))
            for i in range(fw):
                p.px((fx + i, fy), (14, 10, 10))

        # the king's
        if style in ('crown_band', 'crown_plate', 'crown_peak'):
            w.iron(p, f, GOLD, rivets=0, rim=True, dents=0.05)
            for s in S:
                fx, fy, fw, fh = f[s]
                for i in range(0, fw, 3):                         # a band of cloud scrolls
                    p.px((fx + i, fy + fh // 2), GOLD_DEEP)
                    p.px((fx + i + 1, fy + fh // 2 - 1), GOLD_DEEP)
            if style == 'crown_peak':                             # a flame of gold, pointed
                for s in S:
                    w.point(p, f[s], down=False, keep=0.4, sharp=0.45)
        if style == 'crown_gem':
            for s in f:
                for i, j, xy in w.each(p, f[s]):
                    p.lit(xy, (220, 30, 40), 0.55, skin=(200, 24, 36))
        if style == 'ring':
            w.iron(p, f, GOLD, rivets=0, rim=False, dents=0)
        if style in ('cloak', 'cloak_lower'):                     # black cloth, gold-bordered, cloud scrolls in dark gold, tattered below
            for s in ('front', 'back'):
                w.weave(p, f[s], CLOAK, (8, 6, 8), light=w.LIGHT[s], fold=6, fall=0.3)
                fx, fy, fw, fh = f[s]
                for j in range(fh):
                    for i in (0, 1, fw - 2, fw - 1):
                        p.px((fx + i, fy + j), k(GOLD_DEEP if i in (0, fw - 1) else GOLD, w.LIGHT[s]))
                if style == 'cloak':
                    for i in range(fw):
                        p.px((fx + i, fy), GOLD)
                if s == 'back':                                   # a great bear's head in gold thread on his back
                    cx, cy = fx + fw / 2, fy + fh * (0.6 if style == 'cloak' else 0.2)
                    for i, j, xy in w.each(p, f[s]):
                        r = math.hypot(i + fx - cx, (j + fy - cy) * 1.1)
                        if 6.5 < r < 7.6 or (r < 3 and j + fy > cy):
                            p.px(xy, GOLD_DEEP)
                    w.disc(p, cx - 6, cy - 6, 2.2, GOLD_DEEP)
                    w.disc(p, cx + 6, cy - 6, 2.2, GOLD_DEEP)
                    p.px((cx - 2.5, cy - 1.5), GOLD)
                    p.px((cx + 2.5, cy - 1.5), GOLD)
                if style == 'cloak_lower':
                    w.ragged(p, f[s], teeth=3, depth=6)
                    w.tear_holes(p, f[s], n=2, size_=2)
            w.flat(p, f, (10, 8, 10), faces=('right', 'left', 'top', 'bottom'))
        if style in ('kasaya', 'kasaya_drape', 'kasaya_end', 'kasaya_panel'):
            for s in f:
                patchwork(p, f[s], light=w.LIGHT[s], cell=10 if style != 'kasaya_drape' else 8)
                if style == 'kasaya_end':                          # hanging: a fold down its middle
                    fx, fy, fw, fh = f[s]
                    for i, j, xy in w.each(p, f[s]):
                        p.px(xy, k(p.get(xy), 0.8 + 0.3 * math.sin((i + 0.5) / fw * math.pi)))
            w.border(p, f, GOLD, width=1)
            if style in ('kasaya_end', 'kasaya_panel'):
                for s in ('front', 'back'):
                    fx, fy, fw, fh = f[s]
                    for i in range(fw):                           # a fringe of gold
                        p.px((fx + i, fy + fh - 1), GOLD if i % 2 == 0 else SILK_DEEP)
        if style == 'jade':                                       # the jade ring that clasps it
            w.flat(p, f, JADE)
            fx, fy, fw, fh = f['front']
            w.disc(p, fx + fw / 2, fy + fh / 2, fw / 2, k(JADE, 1.1), rim=k(JADE, 0.7))
            w.disc(p, fx + fw / 2, fy + fh / 2, fw / 5, (30, 60, 40))
            p.lit((fx + 1, fy + 1), (200, 255, 220), 0.4)
        if style == 'shaft':                                      # black-red lacquer, wound with cord at the grip
            w.wood(p, f, LACQUER, dark=(20, 6, 8), along='y')
            for s in S:
                fx, fy, fw, fh = f[s]
                for j in range(int(fh * 0.55), int(fh * 0.72)):
                    if j % 2 == 0:
                        w.hline(p, f[s], j, (16, 10, 10))
        if style in ('collar', 'band', 'butt', 'guard'):
            w.iron(p, f, GOLD if style != 'butt' else IRON, rivets=0, dents=0.05)
            if style == 'butt':
                for s in S:
                    w.point(p, f[s], down=True, keep=0.4, sharp=0.5)
        if style == 'spearhead':                                  # the leaf blade, a ridge down it, bright edges
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    v = j / max(1, fh - 1)                        # 0 tip .. 1 root
                    half = 0.5 * math.sin(math.pi * min(1, 0.15 + v * 0.95)) ** 0.8
                    u = abs(i - fw / 2 + 0.5) / (fw / 2)
                    if s in ('front', 'back') and u > half * 1.9:
                        p.clear(xy)
                        continue
                    c = STEEL if u < 0.25 else k(STEEL, 0.78) if u < half * 1.5 else (240, 244, 250)
                    p.px(xy, [x * w.LIGHT[s] + p.rnd.randint(-3, 3) for x in c])
            for s in S:
                w.point(p, f[s], down=False, keep=0.3, sharp=0.5)
        if style in ('tassel', 'tassel_tip', 'tassel_flare'):                     # a great tassel of black horsehair, red at its root
            for s in f:
                w.strands(p, f[s], k(HORSEHAIR, w.LIGHT[s]), (4, 4, 4), k((70, 60, 66), w.LIGHT[s]), density=0.5)
                if style == 'tassel':
                    w.hline(p, f[s], 0, (176, 28, 30))
                    w.hline(p, f[s], 1, (120, 18, 20))
            for s in S:
                w.tufts(p, f[s], depth=4 if style != 'tassel' else 2, every=2)

        # the soldier's
        if style in ('helm_bowl', 'helm_top', 'helm_rim', 'cheekguard', 'plume_socket'):
            w.iron(p, f, IRON, rivets=3 if style == 'helm_bowl' else 0, dents=0.2)
            if style == 'helm_bowl':                              # a bronze bear's face on its brow
                fx, fy, fw, fh = f['front']
                bear_face(p, (fx + fw // 2 - 4, fy, 8, fh), base=BRONZE, deep=(100, 64, 26))
            if style == 'helm_rim':
                gold_rim(p, f, colour=BRONZE)
        if style == 'plume':
            for s in f:
                w.strands(p, f[s], k(lace_c, w.LIGHT[s]), k(lace_d, w.LIGHT[s]), density=0.4)
            for s in S:
                w.tufts(p, f[s], depth=2, every=2)
        if style in ('horn', 'horn_tip'):                         # horns of bone, ringed
            for s in f:
                fx, fy, fw, fh = f[s]
                for i, j, xy in w.each(p, f[s]):
                    c = mix(BONE, (150, 130, 100), 0.3 * ((j % 3) == 0))
                    if style == 'horn_tip':
                        c = mix(BONE, (60, 50, 44), 1 - j / max(1, fh))
                    p.px(xy, [v * w.LIGHT[s] + p.rnd.randint(-4, 4) for v in c])
            if style == 'horn_tip':
                for s in S:
                    w.point(p, f[s], down=False, keep=0.3, sharp=0.5)
        if style == 'shield':                                     # a round shield: dark wood, a bronze rim, bronze studs, a red bear's head
            w.flat(p, f, (40, 24, 18), faces=('right', 'left', 'top', 'bottom'))
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                cx, cy, r = fx + fw / 2, fy + fh / 2, fw / 2
                for i, j, xy in w.each(p, f[s]):
                    d = math.hypot(i + 0.5 - fw / 2, j + 0.5 - fh / 2)
                    if d > r:
                        p.clear(xy)
                    elif d > r - 1.5:
                        p.px(xy, k(BRONZE, w.LIGHT[s]))
                    else:
                        p.px(xy, k(mix((54, 34, 24), (30, 18, 14), w.vnoise(i, j, 3, 9)), w.LIGHT[s]))
                for a in range(12):
                    x, y = cx + (r - 3) * math.cos(a * math.pi / 6), cy + (r - 3) * math.sin(a * math.pi / 6)
                    w.rivet(p, x, y, BRONZE)
                if s == 'back':                                   # (its face, outward)
                    w.disc(p, cx, cy, r * 0.45, lace_c, rim=lace_d)
        if style == 'shield_boss':
            w.iron(p, f, BRONZE, rivets=0, dents=0.05)
        if style in ('haft', 'pommel'):
            w.wood(p, f, (60, 40, 28), along='y') if style == 'haft' else w.iron(p, f, IRON, rivets=0)
            if style == 'haft':
                for s in S:
                    fx, fy, fw, fh = f[s]
                    for j in range(int(fh * 0.6), fh, 2):
                        w.hline(p, f[s], j, (30, 20, 16))
        if style == 'macehead':                                   # black iron, banded, studded
            w.iron(p, f, (52, 50, 56), rivets=2, dents=0.3, rust=0.15)
            for s in S:
                fx, fy, fw, fh = f[s]
                for j in (1, fh // 2, fh - 2):
                    w.hline(p, f[s], j, k(IRON, 1.7))
        if style in ('spike', 'spike_top'):
            w.iron(p, f, (150, 150, 160), rivets=0, rim=False, dents=0)
            if style == 'spike_top':
                for s in S:
                    w.point(p, f[s], down=False, keep=0.2, sharp=0.5)
    return paint_box


if __name__ == '__main__':
    p = Painter(51)
    slots = paint(KING, p, paint_bear(True, LACES[False]))
    p.save('black_bear_demon')
    w.emit(KING, slots, JAVA, 'king')
    w.report('black_bear_demon', KING, KING_SIZE, slots)
    for captain in (False, True):
        p = Painter(52)
        slots = paint(SOLDIER, p, paint_bear(False, LACES[captain]))
        p.save('bear_soldier_captain' if captain else 'bear_soldier')
    w.emit(SOLDIER, slots, JAVA, 'soldier')
    w.report('bear_soldier', SOLDIER, SOLDIER_SIZE, slots)
