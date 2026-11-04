"""The Thunder Court's lord and lady: the Thunder Lord (雷公) and the Lightning Mother (电母).

The Thunder Lord: a bird-demon god of thunder, blue-skinned and bare-chested, heavy with muscle: a hooked gold
beak, round glaring eyes that crackle, a wild red mane standing up in flames from a gold band; a capelet of
dark feathers on his shoulders, a gold torque, gold armlets; a tiger-skin kilt on a gold belt, red sash ends;
a bird's legs, scaled gold-yellow, three-toed with black talons; great dark feathered wings, half-folded at
rest and spread wide as he winds up; and behind him the ring of drums he beats, eight red drums on a gold
hoop arching over his head, their heads marked with crackling thunder-spirals. A mallet in his right hand, a
chisel in his left.

The Lightning Mother: a lady of Heaven in flowing robes of rose and white over a long white under-skirt,
great sleeves lined in blue, a blue sash and long ribbons (one arching behind her shoulders, more streaming
down at her sides); her hair dressed high in two upright loops, a gold crown set with a lightning-jewel,
pins hung with pearls; a round bronze mirror in each hand, its face shining.

Run from the repository root: python assets_source/heaven_gods.py thunder lightning"""
import math

from fire_court import part, mirrored
from sea_court import aim
from king_yama import k, mixc, noisy, point, circle, lock, BLUE_BLACK
from heaven_gods_kit import *
from heaven_gods_luminaries import tress, along_z, arc

# How much larger than his (her) pixels each is drawn (HeavenGodRenderers' ThunderModel.SIZE, LightningModel.SIZE)
THUNDER_SIZE = 0.72
LIGHTNING_SIZE = 0.74

# ── The Thunder Lord ─────────────────────────────────────────────────────────

SKIN = (66, 104, 164)
SKIN_DEEP = (30, 50, 96)
BEAK = (236, 184, 64)
BEAK_DEEP = (150, 100, 30)
FEATHER = (40, 52, 86)
FEATHER_TIP = (120, 146, 200)
TIGER = (226, 140, 40)
SPARK = (200, 230, 255)
SPARK_HOT = (255, 255, 255)
DRUM = (176, 32, 30)

TARM, TFOREARM = (-0.85, 0, 0.32), (-1.25, 0.2, 0)            # his right: the mallet raised
TLARM, TLFOREARM = (-0.55, 0, -0.18), (-0.85, -0.2, 0)          # his left: the chisel held out before him
MALLET = aim([TARM, TFOREARM], (0.1, 0, -0.15))
CHISEL = aim([TLARM, TLFOREARM], (0.9, 0, 0))

WING_REST = (0.1, 0.55, 0.42)
WING_TIP_REST = (0, 0.45, -0.2)

drums = []
for i, a in enumerate(range(160, 381, 31)):
    t = math.radians(a)
    drums.append(part('drum_%d' % i, 'drums', (20 * math.cos(t), 20 * math.sin(t), 0), (0, 0, t + math.pi / 2), [
        (-3.6, -3.6, -1.8, 7.2, 7.2, 3.6, 'tl_drum'),
        (-4.0, -4.0, -0.5, 8.0, 8.0, 1.0, 'tl_drum_band'),
    ]))

THUNDER = mirrored([
    # the hips, his tiger-skin kilt
    part('waist', 'root', (0, 2, 0), (0, 0, 0), [
        (-4.6, -2, -3.4, 9.2, 4, 6.8, 'tl_hips'),
        (-5.2, -2.6, -3.9, 10.4, 2, 7.8, 'tl_belt'),
        (-1.8, -3.2, -4.6, 3.6, 3.2, 1, 'tl_buckle'),
    ]),
    part('kilt', 'waist', (0, -0.6, 0), (0, 0, 0), [(-5.6, 0, -4.2, 11.2, 6.5, 8.4, 'tl_kilt')]),
    part('kilt_flap', 'waist', (0, -0.4, -4.3), (-0.1, 0, 0), [(-2.8, 0, -0.4, 5.6, 9, 0.8, 'tl_flap')]),
    part('right_sash', 'waist', (-4.6, -1.8, -2.6), (0.05, 0, 0.18), [(-0.9, 0, -0.35, 1.8, 10, 0.7, 'tl_sash')]),
    # the legs: a bird's, a thigh feathered at its top, a scaled shank, three talons forward and one behind
    part('right_thigh', 'waist', (-2.8, 1, 0.2), (-0.4, 0, 0.06), [(-2.3, -0.5, -2.3, 4.6, 8.5, 4.6, 'tl_thigh')]),
    part('right_shin', 'right_thigh', (0, 7.6, 0), (0.85, 0, 0), [(-1.5, 0, -1.5, 3, 8.4, 3, 'tl_shin')]),
    part('right_tarsus', 'right_shin', (0, 8, 0), (-0.45, 0, 0), [
        (-1.2, 0, -1.2, 2.4, 5.6, 2.4, 'tl_tarsus'),
        (-1.4, 4.6, -1.6, 2.8, 1.6, 3, 'tl_ankle'),
    ]),
    part('right_toe_a', 'right_tarsus', (-0.8, 5.6, -1.0), (0.05, 0.3, 0), [(-0.6, -0.6, -4.2, 1.2, 1.2, 4.2, 'tl_toe'),
                                                                        (-0.5, -0.2, -5.4, 1, 1.4, 1.4, 'tl_claw')]),
    part('right_toe_b', 'right_tarsus', (0.2, 5.6, -1.2), (0.05, -0.05, 0), [(-0.6, -0.6, -4.8, 1.2, 1.2, 4.8, 'tl_toe'),
                                                                          (-0.5, -0.2, -6, 1, 1.4, 1.4, 'tl_claw')]),
    part('right_toe_c', 'right_tarsus', (0, 5.6, 1.0), (-0.05, 0, 0), [(-0.5, -0.5, 0, 1, 1, 3, 'tl_toe_back'),
                                                                    (-0.45, -0.2, 2.6, 0.9, 1.3, 1.3, 'tl_claw')]),
    # the body: bare, blue, heavy-chested; the feather capelet, the gold torque
    part('body', 'waist', (0, -2.4, 0), (0.08, 0, 0), [
        (-6, -12.8, -3.8, 12, 9.4, 7.6, 'tl_chest'),
        (-4.8, -4.2, -3.2, 9.6, 4.6, 6.4, 'tl_belly'),
        (-7.2, -13.6, -4.6, 14.4, 3.2, 9.2, 'tl_mantle'),
        (-7.4, -12.0, -4.7, 14.8, 4.0, 1.2, 'tl_mantle_front'),
        (-3.8, -14.2, -3.6, 7.6, 1.4, 6.4, 'tl_torque'),
        (-2.2, -15.6, -2.2, 4.4, 2.4, 4.4, 'tl_neck'),
    ]),
    part('right_mantle_back', 'body', (-3.6, -12.4, 4.4), (0.15, 0, 0.05), [(-3.6, 0, -0.5, 7.2, 8, 1, 'tl_mantle_back')]),
    # the ring of drums on his back: a gold hoop arching over his head, eight drums on it
    part('drums', 'body', (0, -11, 8.2), (-0.08, 0, 0), []),
] + arc('hoop', 'drums', 0, 0, 0, 20, 160, 380, 18, 1.4, 1.4, 'tl_hoop') + drums + [
    part('drum_strut', 'body', (0, -11, 3.6), (-0.08, 0, 0), [(-0.7, -0.7, 0, 1.4, 1.4, 4.8, 'tl_hoop'),
                                                              (-4.4, -0.6, 3.6, 8.8, 1.2, 1.2, 'tl_hoop')]),
    # the wings: from his shoulder blades, half-folded behind him
    part('right_wing', 'body', (-2.6, -11.4, 3.6), WING_REST, [
        (-14, -2.4, -0.3, 14, 15, 0.6, 'tl_wing'),
        (-14, -3.2, -0.7, 14, 1.6, 1.4, 'tl_wingbone'),
    ]),
    part('right_wing_tip', 'right_wing', (-13.6, -2.4, 0), WING_TIP_REST, [
        (-11, -0.8, -0.3, 11, 15, 0.6, 'tl_wing_tip'),
        (-11, -0.8, -0.6, 11, 1.2, 1.2, 'tl_wingbone'),
    ]),
    # the arms: blue, heavy, gold-banded; the mallet raised in his right, the chisel out in his left
    part('right_arm', 'body', (-7.6, -11.4, 0), TARM, [(-2.4, -1.8, -2.4, 4.8, 8, 4.8, 'tl_upperarm'),
                                                      (-2.7, 3.4, -2.7, 5.4, 1.4, 5.4, 'tl_armlet')]),
    part('right_forearm', 'right_arm', (0, 6, 0), TFOREARM, [(-2.1, 0, -2.1, 4.2, 7, 4.2, 'tl_forearm'),
                                                            (-2.5, 4.2, -2.5, 5, 2.4, 5, 'tl_bracelet')]),
    part('right_hand', 'right_forearm', (0, 6.8, 0), (0, 0, 0), [(-1.8, 0, -1.8, 3.6, 3.6, 3.6, 'tl_fist')]),
    part('mallet', 'right_hand', (0, 1.8, 0), MALLET, [
        (-0.6, -10, -0.6, 1.2, 13, 1.2, 'tl_handle'),
        (-3.6, -13.8, -2.1, 7.2, 4.2, 4.2, 'tl_mallet'),
        (-4.0, -14.2, -2.5, 0.8, 5, 5, 'tl_mallet_end'),
        (3.2, -14.2, -2.5, 0.8, 5, 5, 'tl_mallet_end'),
    ]),
    part('chisel', 'left_hand', (0, 1.8, 0), CHISEL, [
        (-0.7, -4, -0.7, 1.4, 6.5, 1.4, 'tl_chisel_grip'),
        (-1.0, -5, -1.0, 2, 1.2, 2, 'tl_chisel_ring'),
        (-1.3, -12, -0.5, 2.6, 7, 1, 'tl_chisel'),
    ]),
    # the head: blue, round-eyed, beaked, his red mane standing up from a gold band
    part('head', 'body', (0, -15.2, -0.8), (0, 0, 0), [
        (-4, -8.4, -3.8, 8, 8, 7.6, 'tl_face'),
        (-4.3, -8.9, -4.0, 8.6, 1.4, 8.2, 'tl_band'),
        (-1.3, -9.6, -4.6, 2.6, 2.4, 0.8, 'tl_jewel'),
    ]),
    part('right_eye', 'head', (-2.1, -5.7, -3.9), (0, 0.2, 0), [(-1.4, -1.4, -0.6, 2.8, 2.8, 0.8, 'tl_eye')]),
    part('right_brow', 'head', (-0.3, -7.3, -4.1), (0, 0.15, 0.5), [(-3.8, -0.8, -0.6, 3.8, 1.4, 1.2, 'tl_brow')]),
    part('beak', 'head', (0, -2.5, -3.8), (0.08, 0, 0), [
        (-1.9, -1.6, -5.6, 3.8, 3.2, 5.8, 'tl_beak'),
        (-1.4, 1.0, -6.6, 2.8, 3.4, 2.0, 'tl_hook'),
    ]),
    part('lower_beak', 'head', (0, -0.8, -3.6), (0.1, 0, 0), [(-1.4, 0, -4.4, 2.8, 1.5, 4.4, 'tl_lowbeak')]),
    part('right_ear', 'head', (-4, -5.4, 0.6), (0, -0.4, -0.3), [(-2.6, -1.8, -0.4, 2.6, 3.6, 0.8, 'tl_ear')]),
] + [
    part('mane_%d' % i, 'head', (x, -7.4, z), (rx, 0, rz), [(-1.4, -ln, -1.2, 2.8, ln, 2.4, 'tl_mane')])
    for i, (x, z, rx, rz, ln) in enumerate(((-2.8, -1.6, -0.3, -0.6, 7.5), (-0.9, -2.2, -0.25, -0.2, 9), (1.0, -2.2, -0.25, 0.2, 9),
                                            (2.8, -1.6, -0.3, 0.6, 7.5), (-2.4, 1.0, -0.75, -0.4, 9.5), (0, 1.4, -0.85, 0, 11),
                                            (2.4, 1.0, -0.75, 0.4, 9.5), (-1.4, 3.0, -1.2, -0.25, 9), (1.4, 3.0, -1.2, 0.25, 9)))
])


def mirror_left(parts, arm, forearm):
    out = []
    for name, parent, pivot, rot, boxes, mirror in parts:
        if name == 'left_arm':
            rot = arm
        elif name == 'left_forearm':
            rot = forearm
        out.append((name, parent, pivot, rot, boxes, mirror))
    return out


THUNDER = mirror_left(THUNDER, TLARM, TLFOREARM)


def feathers(p, rect, base, tip, rows=4, light=1.0, cut=True, vertical=True):
    """Feathers in overlapping rows down a face, each lit along its shaft, paler at its tip; the last row's
    tips cut free (scalloped)."""
    fx, fy, fw, fh = rect
    rh = max(3, fh // rows)
    fwid = 3
    for i, j, xy in each(rect):
        row = j // rh
        y = (j % rh) / rh
        x = ((i + (row % 2) * (fwid // 2 + 1)) % fwid) / fwid
        c = mixc(base, tip, y ** 1.6 * 0.9)
        if abs(x - 0.5) < 0.18:
            c = k(c, 1.25)                                        # the shaft
        if x < 0.12:
            c = k(c, 0.6)                                         # the shadow under the next feather
        p.px(xy, noisy(p, k(c, light), 3))
        if cut and j >= fh - rh:
            if y > 0.55 + 0.45 * (1 - abs(x - 0.5) * 2):
                p.clear(xy)


def muscle(p, f, skin, deep, chest=False):
    """Bare blue flesh, heavy with muscle: lit above, the swells rounded, dark in the hollows between."""
    skin_box(p, f, skin)
    if chest:
        fx, fy, fw, fh = f['front']                               # 24x19: the pectorals, the line between, the ribs below
        for i, j, xy in each(f['front']):
            u, v = (i + 0.5) / fw, (j + 0.5) / fh
            pec = min(math.hypot((u - 0.3) / 0.22, (v - 0.32) / 0.3), math.hypot((u - 0.7) / 0.22, (v - 0.32) / 0.3))
            t = 1.0
            if pec < 1:
                t = 1.12 - 0.12 * pec
            elif pec < 1.25 and v > 0.3:
                t = 0.72
            if abs(u - 0.5) < 0.03:
                t = 0.7
            if v > 0.68 and abs(u - 0.5) > 0.12 and int((v - 0.68) * 14) % 2 == 0 and abs(u - 0.5) < 0.42:
                t = 0.82                                          # the ribs
            p.px(xy, noisy(p, mixc(k(skin, t), deep, 0.0 if t > 0.8 else 0.3), 2))


def drum_face(p, rect, back=False):
    """A drumhead seen square: round, cleared outside, a brass-studded red rim, the hide, a thunder-spiral that crackles."""
    def c(r, a, d):
        if r > 0.98:
            return None
        if r > 0.8:
            return GOLD if (int((a + math.pi) * 8 / math.pi) % 2 == 0 and r > 0.88) else DRUM
        if back:
            return (210, 190, 150)
        spiral = (r * 3.2 - a / (2 * math.pi)) % 1.0
        if 0.2 < r < 0.78 and spiral < 0.22:
            return ('glow', mixc((150, 210, 255), (60, 120, 255), r), 0.9)
        if r < 0.14:
            return ('glow', SPARK_HOT, 1.0)
        return mixc((214, 186, 136), (150, 116, 76), r)
    disc(p, rect, c)


def paint_thunder(p, style, f, dims):
    if style == 'tl_hips':
        skin_box(p, f, SKIN)
    if style == 'tl_belt':
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(2, fw, 5):
                p.px((fx + i, fy + fh // 2), (60, 120, 200))
    if style == 'tl_buckle':                                      # a gold buckle, a thunder-spiral on it
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        for (a, b) in ((1, 1), (2, 1), (3, 1), (3, 2), (3, 3), (2, 3), (1, 3), (1, 4)):
            if a < fw and b < fh:
                p.lit((fx + a + 1, fy + b), SPARK, glow=0.8)
    if style in ('tl_kilt', 'tl_flap'):                           # a tiger's skin: orange, black stripes, a ragged white-tufted foot
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                p.px(xy, noisy(p, k(mixc(TIGER, (250, 220, 170), 0.15 if j > fh * 0.7 else 0), LIGHTS[s] * (1.05 - 0.15 * j / fh)), 5))
            if s in SIDES:
                p.stripes(r, (30, 20, 16), 5, vertical=True, reach=0.75)
                for i in range(fw):
                    cut = int(3 * abs(math.sin(i * 0.9))) + p.rnd.randint(0, 1)
                    for j in range(cut):
                        p.clear((fx + i, fy + fh - 1 - j))
                hline(p, r, 0, GOLD_DEEP)
    if style == 'tl_sash':
        for s, r in f.items():
            silk_face(p, r, (200, 40, 36), (110, 14, 14), LIGHTS[s], 4)
            hline(p, r, r[3] - 1, GOLD)
    if style == 'tl_thigh':                                       # blue flesh, a fringe of dark feathers at the top
        skin_box(p, f, SKIN)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            feathers(p, (fx, fy, fw, fh // 2), FEATHER, FEATHER_TIP, rows=2, light=LIGHTS[s])
    if style in ('tl_shin', 'tl_tarsus', 'tl_toe', 'tl_toe_back'):   # a bird's shank: gold-yellow scutes, dark between
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                along = j if style != 'tl_toe' and style != 'tl_toe_back' else i
                c = BEAK if along % 3 else BEAK_DEEP
                across = abs((i + 0.5) / fw - 0.5) * 2
                p.px(xy, noisy(p, k(c, LIGHTS[s] * (1.1 - 0.3 * across)), 3))
    if style == 'tl_ankle':
        gold_box(p, f)
    if style == 'tl_claw':                                        # a black talon, curving down to its point
        for s, r in f.items():
            for i, j, xy in each(r):
                p.px(xy, mixc((40, 34, 40), (10, 8, 12), j / max(1, r[3] - 1)))
        hline(p, f['top'], 0, (90, 84, 96))

    if style == 'tl_chest':
        muscle(p, f, SKIN, SKIN_DEEP, chest=True)
        fx, fy, fw, fh = f['back']                                # the back: shoulder blades
        for j in range(3, fh - 3):
            p.px((fx + fw // 2, fy + j), k(SKIN, 0.7))
    if style == 'tl_belly':                                       # the belly: banded muscle
        muscle(p, f, SKIN, SKIN_DEEP)
        fx, fy, fw, fh = f['front']
        for j in range(1, fh, 3):
            hline(p, f['front'], j, k(SKIN, 0.78), 3, fw - 3)
        vline(p, (fx, fy, fw, fh), fw // 2, k(SKIN, 0.72))
    if style in ('tl_mantle', 'tl_mantle_front', 'tl_mantle_back'):   # the capelet of dark feathers
        for s, r in f.items():
            if s == 'top':
                p.face(r, FEATHER, 1.1, noise=4)
                fx, fy, fw, fh = r
                for i, j, xy in each(r):
                    if (i + j * 2) % 5 == 0:
                        p.px(xy, FEATHER_TIP)
            elif s == 'bottom':
                p.face(r, k(FEATHER, 0.6), 1.0, noise=3)
            else:
                feathers(p, r, FEATHER, FEATHER_TIP, rows=2 if style == 'tl_mantle_front' else max(1, r[3] // 6), light=LIGHTS[s])
        if style == 'tl_mantle':
            for s in SIDES:
                hline(p, f[s], 0, GOLD)
    if style == 'tl_torque':
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        p.lit((fx + fw // 2, fy + fh // 2), SPARK, glow=0.9)
    if style == 'tl_neck':
        skin_box(p, f, k(SKIN, 0.85))

    if style == 'tl_hoop':                                        # the hoop: red lacquer bound in gold
        p.shaded(f, (170, 30, 26))
        for s in SIDES + ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 4):
                vline(p, f[s], i, GOLD)
    if style == 'tl_drum':
        for s in ('front', 'back'):
            drum_face(p, f[s], back=s == 'back')
        for s in ('top', 'bottom', 'right', 'left'):              # its barrel: red, a row of brass studs
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                p.px(xy, noisy(p, k(DRUM, LIGHTS[s] * (1.1 - 0.3 * abs((i + 0.5) / fw - 0.5) * 2)), 3))
            for i in range(1, fw, 2):
                p.px((fx + i, fy + fh // 2), GOLD_LIGHT)
            for j in range(1, fh, 2):
                if fw < fh:
                    p.px((fx + fw // 2, fy + j), GOLD_LIGHT)
    if style == 'tl_drum_band':                                   # a gold band round the barrel, its faces cut away
        gold_box(p, f)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            disc(p, f[s], lambda r, a, d: None if r < 0.9 or r > 1.0 else GOLD)

    if style in ('tl_wing', 'tl_wing_tip'):                       # a great dark wing: coverts at its top, long flight feathers below, their ends scalloped
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            tip = style == 'tl_wing_tip'
            for i, j, xy in each(f[s]):
                u, v = (i + 0.5) / fw, (j + 0.5) / fh
                if s == 'back':
                    u = 1 - u                                     # u 0 at the wing's outer end .. 1 at its root
                fl = (i // 3)                                     # each flight feather three pixels wide
                fx_ = (i % 3) / 3
                end = 0.72 + 0.28 * (u if not tip else u * 0.6) - 0.08 * fx_ - (0.06 if fl % 2 else 0)
                if tip:
                    end = 0.55 + 0.45 * u - 0.08 * fx_ - (0.06 if fl % 2 else 0)
                if v > end:
                    p.clear(xy)
                    continue
                if v < 0.3:                                       # the coverts: small feathers, scaled, gold-tipped near the bone
                    row = int(v / 0.1)
                    cx_ = ((i + row * 2) % 4) / 4
                    c = mixc(FEATHER, FEATHER_TIP, ((v % 0.1) / 0.1) ** 1.5)
                    if row == 0 and cx_ < 0.5:
                        c = GOLD_DEEP
                    if cx_ < 0.15:
                        c = k(c, 0.6)
                else:                                             # the flight feathers, long, a pale shaft, paler toward their ends
                    c = mixc(FEATHER, FEATHER_TIP, ((v - 0.3) / max(0.1, end - 0.3)) ** 2 * 0.8)
                    if fx_ < 0.2:
                        c = k(c, 0.55)
                    elif abs(fx_ - 0.5) < 0.2:
                        c = mixc(c, (160, 180, 220), 0.35)
                p.px(xy, noisy(p, k(c, 1.0 if s == 'front' else 0.85), 3))
        bare(p, f)
    if style == 'tl_wingbone':                                    # the wing's leading edge: dark, gold-banded
        p.shaded(f, (36, 42, 70))
        for s in SIDES + ('top',):
            fx, fy, fw, fh = f[s]
            for i in range(0, fw, 6):
                vline(p, f[s], i, GOLD)

    if style == 'tl_upperarm':
        muscle(p, f, SKIN, SKIN_DEEP)
    if style == 'tl_forearm':
        muscle(p, f, SKIN, SKIN_DEEP)
    if style in ('tl_armlet', 'tl_bracelet'):                     # gold bands, a crackling stone
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            hline(p, f[s], fh // 2, GOLD_DEEP)
            p.lit((fx + fw // 2, fy + fh // 2), SPARK, glow=0.7)
    if style == 'tl_fist':
        hand(p, f, SKIN, nails=(20, 18, 24))

    if style == 'tl_handle':                                      # dark wood, cord-wound, gold-capped
        p.shaded(f, (80, 50, 30))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for j in range(fh - 12, fh - 2):
                hline(p, f[s], j, (150, 30, 26) if j % 2 else (100, 20, 18))
            hline(p, f[s], 0, GOLD)
    if style == 'tl_mallet':                                      # the mallet's head: a gilt barrel, bands crackling with light
        gold_box(p, f)
        for s in SIDES + ('top', 'bottom'):
            fx, fy, fw, fh = f[s]
            if s in ('front', 'back', 'top', 'bottom'):
                for x in (2, fw - 3):
                    for j in range(fh):
                        p.lit((fx + x, fy + j), SPARK, glow=0.75, skin=(120, 170, 230))
                for i in range(4, fw - 4):
                    if (i + p.rnd.randint(0, 1)) % 3 == 0:
                        p.px((fx + i, fy + fh // 2), GOLD_DEEP)
    if style == 'tl_mallet_end':                                  # its striking faces, a thunder-spiral each
        gold_box(p, f)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            def c(r, a, d):
                if r > 1:
                    return None
                spiral = (r * 2.6 - a / (2 * math.pi)) % 1.0
                return ('glow', SPARK, 0.85) if spiral < 0.25 and r > 0.15 else GOLD_DEEP if r > 0.85 else GOLD
            disc(p, f[s], c, clear_out=False)
    if style in ('tl_chisel_grip',):
        p.shaded(f, (80, 50, 30))
        for s in SIDES:
            for j in range(1, f[s][3], 2):
                hline(p, f[s], j, (150, 30, 26))
    if style == 'tl_chisel_ring':
        gold_box(p, f)
    if style == 'tl_chisel':                                      # a steel wedge, its point white-hot with lightning
        steel_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                if j < fh * 0.4:
                    p.lit(xy, mixc(SPARK_HOT, (120, 180, 255), j / (fh * 0.4)), glow=0.9)
            point(p, f[s], keep=0.5, down=False, sharp=0.32 if fw > 2 else 0.6)

    # the head
    if style == 'tl_face':                                        # blue; round sockets; a scowl; the hair dark under the mane
        skin_box(p, f, SKIN)
        for s in ('back', 'top'):
            hair_box(p, {s: f[s]}, RED_HAIR)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for i, j, xy in each(f[s]):
                if (s == 'right' and i < fw * 0.4) or (s == 'left' and i > fw * 0.6) or j < 3:
                    p.px(xy, shade_of(RED_HAIR, 1.2 + p.rnd.uniform(-0.4, 0.4)))
        fx, fy, fw, fh = f['front']                               # 16x16
        for side in (0, 1):                                       # dark sockets round the eyes
            ex = 1 if side == 0 else fw - 1 - 7
            for i, j, xy in each((fx + ex, fy + 2, 7, 6)):
                d = math.hypot((i + 0.5) / 7 - 0.5, (j + 0.5) / 6 - 0.5)
                if d < 0.55:
                    p.px(xy, k(SKIN_DEEP, 0.9))
        for j in range(2, 6):
            p.px((fx + fw // 2 - 1, fy + j), k(SKIN, 0.7))
            p.px((fx + fw // 2, fy + j), k(SKIN, 0.7))
        for j in range(fh - 5, fh):                               # whiskers of feathers along the jaw
            for i in range(fw):
                if (i < 4 or i > fw - 5) and (i + j) % 2 == 0:
                    p.px((fx + i, fy + j), FEATHER_TIP)
    if style == 'tl_band':                                        # a gold band, thunder-spirals worked on it
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(1, fw, 4):
                p.px((fx + i, fy + 1), GOLD_DEEP)
    if style == 'tl_jewel':
        gold_box(p, f)
        fx, fy, fw, fh = f['front']
        for i, j, xy in each((fx + 1, fy + 1, fw - 2, fh - 2)):
            p.lit(xy, SPARK, glow=1.0)
    if style == 'tl_eye':                                         # round, glaring, crackling white-blue, a pin of a pupil
        for s, r in f.items():
            for i, j, xy in each(r):
                p.lit(xy, (230, 240, 255), glow=0.5)
        fx, fy, fw, fh = f['front']                               # 6x6
        def c(r, a, d):
            if r > 1.0:
                return None
            if r > 0.82:
                return (20, 16, 30)
            if r < 0.22:
                return (10, 10, 20)
            return ('glow', mixc(SPARK_HOT, (110, 170, 255), r), 1.0)
        disc(p, f['front'], c)
        p.glow.putpixel((fx + fw // 2, fy + fh // 2), (0, 0, 0, 0))
        p.glow.putpixel((fx + fw // 2 - 1, fy + fh // 2 - 1), (0, 0, 0, 0))
        bare(p, f, ('top', 'bottom'))
    if style == 'tl_brow':
        lock(p, f, along_x=True, tone=RED_HAIR, grey=0.0)
    if style in ('tl_beak', 'tl_hook', 'tl_lowbeak'):             # the hooked beak: gold-yellow, horn-streaked, dark at its hook
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                across = abs((i + 0.5) / fw - 0.5) * 2
                c = mixc(mixc((255, 226, 120), BEAK, 0.6), BEAK_DEEP, across * 0.6 + (0.2 if s in ('right', 'left') else 0))
                p.px(xy, noisy(p, k(c, LIGHTS[s]), 3))
        if style == 'tl_beak':
            for s in ('right', 'left'):                           # the nostril, the gape's line
                fx, fy, fw, fh = f[s]
                p.px((fx + (fw // 3 if s == 'left' else fw - 1 - fw // 3), fy + 1), (60, 30, 10))
                hline(p, f[s], fh - 1, (90, 50, 20))
        if style == 'tl_hook':
            for s in SIDES:
                fx, fy, fw, fh = f[s]
                for i, j, xy in each(f[s]):
                    if j > fh * 0.5:
                        p.px(xy, mixc(BEAK_DEEP, (60, 40, 20), (j - fh * 0.5) / (fh * 0.5)))
                point(p, f[s], keep=0.5, down=True, sharp=0.6)
        if style == 'tl_lowbeak':
            hline(p, f['top'], 0, (140, 30, 30))
    if style == 'tl_ear':                                         # a tuft of feathers for an ear
        for s, r in f.items():
            feathers(p, r, FEATHER, FEATHER_TIP, rows=2, light=LIGHTS[s], cut=False)
    if style == 'tl_mane':                                        # his red mane standing up in flames, its tips crackling
        tress_up(p, f)


def tress_up(p, f):
    tress(p, f, RED_HAIR, axis='y', up=True, keep=0.4)
    for s in SIDES:
        fx, fy, fw, fh = f[s]
        for i, j, xy in each((fx, fy, fw, max(1, fh // 4))):
            if p.img.getpixel(xy)[3]:
                p.lit(xy, mixc((255, 220, 120), (240, 100, 40), j / max(1, fh // 4)), glow=0.55)


# ── The Lightning Mother ─────────────────────────────────────────────────────

ROSE = (232, 120, 156)
ROSE_DEEP = (140, 50, 86)
WHITE = (246, 242, 248)
WHITE_DEEP = (176, 170, 196)
BLUE = (86, 146, 230)
BLUE_DEEP = (36, 70, 150)
LADY = (248, 228, 218)
HAIR = BLUE_BLACK
FLASH = (200, 236, 255)

LMARM, LMFOREARM = (-0.2, 0, 0.42), (-1.85, 0.3, 0)            # her right: out from her side, the forearm raised, a mirror held up
MIRROR = aim([LMARM, LMFOREARM], (0.05, 0.4, 0))

LIGHTNING = mirrored([
    part('body', 'root', (0, 5, 0), (0, 0, 0), [
        (-5, -14, -3.8, 10, 14, 7.6, 'lm_chest'),
        (-3.2, -15, -2.8, 6.4, 1.4, 5.6, 'lm_collar'),
        (-1.8, -16.8, -1.8, 3.6, 3, 3.6, 'lm_neck'),
        (-6.4, -14.6, -4.6, 12.8, 0.8, 9.2, 'lm_cloudtop'),
        (-6.0, -14.2, -4.7, 12, 5, 0.5, 'lm_lobes'),
        (-6.0, -14.2, 4.2, 12, 5, 0.5, 'lm_lobes'),
        (-5.6, -4.6, -4.3, 11.2, 2.4, 8.6, 'lm_sash'),
        (-1.6, -5.4, -4.9, 3.2, 3.2, 0.8, 'lm_knot'),
    ]),
    part('skirt', 'body', (0, 0, 0), (0, 0, 0), [
        (-5.4, -0.4, -4.2, 10.8, 7, 8.4, 'lm_robe1'),
        (-6.6, 6, -5.0, 13.2, 7, 10, 'lm_robe2'),
        (-8.2, 12.4, -6.2, 16.4, 6.8, 12.4, 'lm_hem'),
    ]),
    part('underskirt', 'skirt', (0, -1, -4.6), (-0.14, 0, 0), [(-3.6, 0, -0.8, 7.2, 20.5, 0.8, 'lm_under')]),
    part('right_sash_end', 'body', (-1.2, -3.4, -4.8), (-0.12, 0.1, 0.1), [(-0.8, 0, -0.3, 1.6, 22, 0.6, 'lm_sash_end')]),
    part('right_panel', 'skirt', (-5.0, -1, -4.8), (-0.1, 0.4, 0.05), [(-2, 0, -0.4, 4, 19.5, 0.4, 'lm_panel')]),
    part('back_panel', 'skirt', (0, -1.5, 4.5), (0.14, 0, 0), [(-4, 0, 0, 8, 20, 0.5, 'lm_backpanel')]),
    part('right_shoe', 'root', (-2.6, 22.6, -6.6), (0, 0.1, 0), [(-1.3, -0.6, -2.2, 2.6, 1.6, 3.4, 'lm_shoe')]),
    # the ribbons: one arching behind her shoulders, more streaming down at her sides
    part('arch', 'body', (0, -13, 5.4), (0.15, 0, 0), []),
] + arc('arch_seg', 'arch', 0, 0, 0, 13.4, 180, 360, 11, 2.2, 0.4, 'lm_ribbon') + [
    part('right_ribbon', 'body', (-13.2, -13.4, 5.2), (0.1, 0.4, 0.12), [(-1.2, 0, -0.2, 2.4, 13, 0.4, 'lm_ribbon_a')]),
    part('right_ribbon_2', 'right_ribbon', (0, 12.6, 0), (0.15, 0, 0.22), [(-1.3, 0, -0.2, 2.6, 12, 0.4, 'lm_ribbon_a')]),
    part('right_ribbon_3', 'right_ribbon_2', (0, 11.6, 0), (0.15, 0, 0.25), [(-1.4, 0, -0.2, 2.8, 10, 0.4, 'lm_ribbon_b')]),
    part('right_streamer', 'body', (-4.6, -3.6, 4.0), (0.35, 0.2, 0.25), [(-1.1, 0, -0.2, 2.2, 13, 0.4, 'lm_streamer')]),
    part('right_streamer_2', 'right_streamer', (0, 12.6, 0), (0.25, 0, 0.2), [(-1.2, 0, -0.2, 2.4, 11, 0.4, 'lm_streamer_end')]),
    # the arms: great sleeves, the mirrors held up
    part('right_arm', 'body', (-6.4, -12.6, 0), LMARM, [(-2.6, -1.6, -2.4, 4.2, 8, 4.8, 'lm_sleeve')]),
    part('right_shoulder', 'right_arm', (-1, -1.8, 0), (0, 0, 0.25), [(-3.6, -1, -3.3, 4.8, 1.8, 6.6, 'lm_shoulder')]),
    part('right_forearm', 'right_arm', (-0.6, 5.8, 0), LMFOREARM, [
        (-1.9, 0, -1.9, 3.8, 6.4, 3.8, 'lm_forearm'),
        (-2.2, 4.4, -2.2, 4.4, 1.8, 4.4, 'lm_cuff'),
    ]),
    part('right_sleeve', 'right_forearm', (0, 0.6, 1.8), aim([LMARM, LMFOREARM], (0.05, -0.35, 0.18)), [
        (-3.2, -1, -8.4, 6.4, 13, 8.4, 'lm_bigsleeve')]),
    part('right_hand', 'right_forearm', (0, 6.6, 0), (0, 0, 0), [(-1.3, 0, -1.3, 2.6, 2.6, 2.6, 'lm_hand')]),
    part('right_mirror', 'right_hand', (0, 1.4, -0.2), MIRROR, [
        (-0.6, -3.6, -0.6, 1.2, 4.6, 1.2, 'lm_handle'),
        (-1.8, -4.6, -0.9, 3.6, 1.2, 1.8, 'lm_mount'),
        (-5.4, -15.4, -0.6, 10.8, 10.8, 1.2, 'lm_mirror'),
        (-1.2, -17, -0.5, 2.4, 1.8, 1, 'lm_finial'),
    ]),
    # the head: a lady's, her hair high in two upright loops, a gold crown, pins with pearls
    part('head', 'body', (0, -16.8, -0.6), (0, 0, 0), [
        (-4, -8, -3.6, 8, 7.2, 7.2, 'lm_face'),
        (-0.6, -4.0, -4.1, 1.2, 1.4, 0.5, 'lm_nose'),              # below the eyes' band, not over it
        (-4.2, -8.3, -0.6, 8.4, 7.6, 4.6, 'lm_hair_back'),
        (-4.3, -9.2, -3.3, 8.6, 1.8, 7.4, 'lm_hair_top'),
        (-2.8, -0.9, -3.3, 5.6, 1.4, 5.6, 'lm_chin'),              # a narrow chin, the face tapering to it
    ]),
    part('right_lock', 'head', (-3.9, -7.0, -2.0), (-0.04, 0, 0.05), [(-0.7, 0, -0.9, 1.4, 10, 1.8, 'lm_lock')]),
    part('right_ear', 'head', (-4.0, -4.6, -0.6), (0, -0.2, 0), [(-0.6, -1.2, -0.8, 0.7, 2.8, 1.6, 'lm_ear')]),
    part('crown', 'head', (0, -8.8, 0), (0, 0, 0), [                  # set high: its band clear of her brows
        (-2.6, -3.2, -1.8, 5.2, 3.6, 4.4, 'lm_bun'),
        (-4.4, -0.6, -4.1, 8.8, 1.6, 8.2, 'lm_crown'),
        (-2.0, -4.2, -4.4, 4.0, 4.0, 0.6, 'lm_crown_front'),
    ]),
    part('right_loop', 'crown', (-1.6, -2.8, 0.2), (-0.1, 0, -0.32), [(-1.3, -6.4, -1.3, 2.6, 6.4, 2.6, 'lm_loop')]),
    part('right_loop_top', 'right_loop', (0, -6.2, 0), (0, 0, 0.9), [(-1.2, -3.4, -1.2, 2.4, 3.4, 2.4, 'lm_loop_top')]),
    part('right_pin', 'crown', (-2.6, -1.6, -0.4), (0, 0.2, 0.3), [
        (-4.4, -0.3, -0.3, 4.4, 0.6, 0.6, 'lm_pin'), (-4.2, 0.3, -0.3, 0.6, 3.2, 0.6, 'lm_dangle'),
        (-2.8, 0.3, -0.3, 0.6, 2.2, 0.6, 'lm_dangle')]),
])


def lm_silk(p, f, base=ROSE, deep=ROSE_DEEP, fold=6):
    silk(p, f, base, deep, fold=fold)


def zigzag(p, rect, colour, glow=0.8, x0=None):
    """A bolt of lightning down a face: a jagged line, bright at its core."""
    fx, fy, fw, fh = rect
    x = fx + (fw // 2 if x0 is None else x0)
    for j in range(fh):
        x += (1 if (j // 2) % 3 == 0 else -1 if (j // 2) % 3 == 1 else 0)
        x = max(fx, min(fx + fw - 1, x))
        p.lit((x, fy + j), colour, glow=glow)


def lm_face_front(p, rect):
    """Her face, as a heavenly lady of the opera stage is painted: pale, a peach-red flush (桃花妆) washed
    round the eyes and up to the temples, fading into the cheeks; black brows winging up to the temples;
    the lash line drawn out into a flick at each outer corner; eyes of lightning, flat blocks of glowing
    blue iris as vanilla's, a thin dark pupil in each; a zigzag of lightning glowing on her brow; small red
    lips. On the front only (her cheeks are bare, so from three-quarters there are two eyes, not four)."""
    fx, fy, fw, fh = rect
    cx = fw / 2
    BLUSH, BLUSH_DEEP = (236, 96, 120), (196, 52, 84)
    BROW = (58, 46, 78)
    IRIS, IRIS_DEEP, PUPIL = (110, 196, 255), (60, 130, 240), (16, 22, 60)
    for i, j, xy in each(rect):                                    # the skin, lit across the brow, rounded
        across = abs((i + 0.5) / fw - 0.5) * 2
        c = k(LADY, 1.06 - 0.16 * across ** 2 - 0.06 * j / fh)
        side = min(i, fw - 1 - i)                                   # 0 at her temple .. 7 at the middle
        # the peach flush: strongest about the eyes (rows 3..7) and out to the temples, fading downward
        if 3 <= j <= 10 and side <= 6:
            near = 1 - abs(j - 5.5) / 5.0
            out = 1 - max(0, side - 1) / 6.0
            t = max(0.0, near) * (0.35 + 0.5 * out)
            if j >= 8:
                t *= 0.75
            c = mixc(c, BLUSH, min(0.62, t))
        p.px(xy, noisy(p, c, 2))
    for i in range(fw):                                            # the hairline, a widow's peak at its middle
        p.px((fx + i, fy), shade_of(HAIR, 1.1))
        if i < 3 or i > fw - 4 or abs(i + 0.5 - cx) < 1:
            p.px((fx + i, fy + 1), shade_of(HAIR, 0.9))
    for side in (0, 1):
        m = (lambda x: x) if side == 0 else (lambda x: fw - 1 - x)  # side 0 her right (on the left as we look)
        # brows: fine and black, rising from the inner end in a wing to the temple
        for x, y in ((6, 3), (5, 3), (4, 3), (3, 2), (2, 2), (1, 1)):
            p.px((fx + m(x), fy + y), BROW)
        # the lash line over the eye, drawn out at the outer corner and flicked up
        for x in range(1, 6):
            p.px((fx + m(x), fy + 5), (30, 18, 36))
        p.px((fx + m(0), fy + 4), (30, 18, 36))
        # the eye: a flat block of glowing blue iris, its lower row a shade deeper; a thin pupil inside
        for x in (3, 4, 5):
            p.lit((fx + m(x), fy + 6), IRIS, glow=0.9)
            p.lit((fx + m(x), fy + 7), IRIS_DEEP, glow=0.8)
        p.px((fx + m(2), fy + 6), (246, 242, 248))                  # the white at each corner of the eye
        p.px((fx + m(6), fy + 6), (246, 242, 248))
        p.px((fx + m(2), fy + 7), k(LADY, 0.86))
        p.px((fx + m(6), fy + 7), k(LADY, 0.86))
        for y in (6, 7):
            p.px((fx + m(4), fy + y), PUPIL)
            p.glow.putpixel((fx + m(4), fy + y), (0, 0, 0, 0))
        for x in range(2, 7):                                      # the flush deepening under the eye
            p.px((fx + m(x), fy + 8), mixc(p.img.getpixel((fx + m(x), fy + 8))[:3], BLUSH_DEEP, 0.35))
    # the lightning mark on her brow (花钿), glowing
    for x, y in ((8, 2), (7, 3), (8, 3), (7, 4)):
        p.lit((fx + x, fy + y), (70, 150, 255), glow=1.0)
    # the shadow under her nose, her lips: small and red, the upper a deeper bow
    mid = int(cx)
    p.px((fx + mid - 1, fy + 11), k(LADY, 0.84))
    p.px((fx + mid, fy + 11), k(LADY, 0.84))
    for x in (mid - 2, mid + 1):
        p.px((fx + x, fy + 12), k(RED_LIPS, 0.8))
    for x in (mid - 1, mid):
        p.px((fx + x, fy + 12), k(RED_LIPS, 0.7))
        p.px((fx + x, fy + 13), RED_LIPS)
    p.px((fx + mid - 2, fy + 13), k(RED_LIPS, 0.9))
    p.px((fx + mid + 1, fy + 13), k(RED_LIPS, 0.9))
    # polish (none of it on the eyes, the flush about them or the brow mark): a scalloped hairline, the
    # small flat curls of an opera lady's hair pieces (片子) along her forehead; the cheekbones catching the
    # light under the flush; the face's sides turning away into shadow down to a narrow jaw; the lips
    # glossed, their corners tucked in shadow; the dip under the lower lip
    for x in (4, 5, fw - 5, fw - 6):
        p.px((fx + x, fy + 1), shade_of(HAIR, 0.7 if x in (4, fw - 5) else 1.0))
    for side in (0, 1):
        m = (lambda x: x) if side == 0 else (lambda x: fw - 1 - x)
        for x, y in ((3, 9), (4, 9), (3, 10), (4, 10), (5, 10)):
            p.px((fx + m(x), fy + y), mixc(p.img.getpixel((fx + m(x), fy + y))[:3], (255, 246, 240), 0.35))
        for y in range(10, fh):
            t = (y - 9) / (fh - 9)
            p.px((fx + m(0), fy + y), mixc(p.img.getpixel((fx + m(0), fy + y))[:3], k(LADY, 0.7), 0.5 * t + 0.2))
            if y >= 12:
                p.px((fx + m(1), fy + y), mixc(p.img.getpixel((fx + m(1), fy + y))[:3], k(LADY, 0.74), 0.35 * t))
    p.px((fx + mid - 3, fy + 12), k(LADY, 0.84))                 # the mouth's corners
    p.px((fx + mid + 2, fy + 12), k(LADY, 0.84))
    p.px((fx + mid - 1, fy + 13), mixc(RED_LIPS, (255, 236, 236), 0.4))   # the gloss on the lower lip
    if fh > 14:
        p.px((fx + mid - 1, fy + 14), k(LADY, 0.86))             # the dip beneath it
        p.px((fx + mid, fy + 14), k(LADY, 0.86))


def paint_lightning(p, style, f, dims):
    trim, trim_deep = GOLD, GOLD_DEEP

    if style == 'lm_chest':                                       # rose silk, a crossed white collar, clouds in gold thread
        lm_silk(p, f)
        fx, fy, fw, fh = f['front']
        for j in range(fh // 2 + 2):
            for d in range(3):
                for x in (fw // 2 - j // 2 - d, fw // 2 + j // 2 + d - 1):
                    if 0 <= x < fw:
                        p.px((fx + x, fy + j), trim if d == 0 else WHITE)
        for s in ('front', 'back'):
            clouds(p, f[s], trim_deep, every=9, r=1, share=0.5)
    if style == 'lm_collar':
        p.shaded(f, WHITE)
        edges(p, f, trim)
    if style == 'lm_neck':
        skin_box(p, f, k(LADY, 0.85))
    if style == 'lm_cloudtop':
        p.shaded(f, BLUE_DEEP)
        fx, fy, fw, fh = f['top']
        for i, j, xy in each(f['top']):
            dx, dy = (i + 0.5 - fw / 2) / (fw / 2), (j + 0.5 - fh / 2) / (fh / 2)
            a, r = math.atan2(dy, dx), math.hypot(dx, dy)
            reach = 0.55 + 0.45 * abs(math.cos(2 * a)) ** 0.6
            if r < reach - 0.12:
                p.px(xy, noisy(p, BLUE, 3))
            elif r < reach:
                p.px(xy, trim)
        edges(p, f, trim)
    if style == 'lm_lobes':
        for s, r in f.items():
            p.face(r, BLUE_DEEP, 1.0, noise=3)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                t = (i % (fw // 2)) / (fw // 2)
                depth = int(fh * (0.35 + 0.65 * math.sin(t * math.pi) ** 0.7))
                for j in range(fh):
                    if j >= depth:
                        p.clear((fx + i, fy + j))
                    elif j >= depth - 2:
                        p.px((fx + i, fy + j), trim)
                    elif j >= depth - 4:
                        p.px((fx + i, fy + j), BLUE)
    if style in ('lm_sash', 'lm_knot', 'lm_sash_end'):            # the blue sash, knotted, its ends to the ground
        for s, r in f.items():
            silk_face(p, r, BLUE, BLUE_DEEP, LIGHTS[s], 4 if style != 'lm_sash' else 6)
        if style == 'lm_sash':
            edges(p, f, trim)
        if style == 'lm_sash_end':
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                hline(p, f[s], fh - 1, trim)
                hline(p, f[s], fh - 3, trim)
                for j in range(6, fh - 6, 8):
                    p.px((fx + fw // 2, fy + j), trim)
    if style in ('lm_robe1', 'lm_robe2', 'lm_hem'):               # the rose robe in tiers: clouds in gold, a border of white and blue at each foot
        lm_silk(p, f, ROSE if style != 'lm_robe2' else mixc(ROSE, ROSE_DEEP, 0.15))
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            clouds(p, f[s], trim_deep, every=9, r=1, share=0.6, skip_bottom=4)
            if style == 'lm_hem':
                hem_band(p, f[s], 4, WHITE, trim, trim_deep)
                for i in range(0, fw, 6):                         # a fringe of lightning in blue at the hem
                    zigzag(p, (fx + i, fy + fh - 10, 3, 5), (140, 200, 255), glow=0.5)
            else:
                hline(p, f[s], fh - 1, trim)
                hline(p, f[s], fh - 2, BLUE)
        if style == 'lm_robe1':                                   # the robe open down the front over the white
            fx, fy, fw, fh = f['front']
            for j in range(fh):
                for i in range(fw // 2 - 3, fw // 2 + 3):
                    p.px((fx + i, fy + j), trim if abs(i - fw // 2 + 0.5) > 2 else noisy(p, WHITE, 3))
    if style == 'lm_under':                                       # the white under-skirt, falling in folds, a blue border
        for s, r in f.items():
            silk_face(p, r, WHITE, WHITE_DEEP, LIGHTS[s], 4, fall=0.3)
        fx, fy, fw, fh = f['front']
        hem_band(p, f['front'], 4, BLUE, trim, trim_deep)
        for j in range(6, fh - 8, 9):
            cloud_mark(p, fx + fw // 2 - 3, fy + j, (190, 210, 240), 1, f['front'])
    if style == 'lm_panel':
        for s, r in f.items():
            silk_face(p, r, mixc(ROSE, ROSE_DEEP, 0.4), ROSE_DEEP, LIGHTS[s], 5)
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            vline(p, f[s], 0, trim)
            vline(p, f[s], fw - 1, trim)
            hline(p, f[s], fh - 1, trim)
    if style == 'lm_backpanel':
        for s, r in f.items():
            silk_face(p, r, mixc(ROSE, ROSE_DEEP, 0.3), ROSE_DEEP, LIGHTS[s], 5)
        for s in ('front', 'back'):
            border(p, f[s], trim)
            hem_band(p, f[s], 4, WHITE, trim, trim_deep)
    if style == 'lm_shoe':                                        # a cloud-toed slipper
        p.shaded(f, ROSE_DEEP)
        edges(p, f, trim, bottom=False)

    if style in ('lm_ribbon', 'lm_ribbon_a', 'lm_ribbon_b', 'lm_streamer', 'lm_streamer_end'):
        along_x = style == 'lm_ribbon'
        base = BLUE if style.startswith('lm_ribbon') else WHITE
        for s, r in f.items():
            fx, fy, fw, fh = r
            for i, j, xy in each(r):
                along = i if along_x else j
                across = (j / max(1, fh - 1)) if along_x else (i / max(1, fw - 1))
                wave = 1 + 0.13 * math.sin(along * 0.45)
                p.px(xy, noisy(p, k(base, LIGHTS[s] * wave * (1.08 - 0.25 * abs(across - 0.4))), 2))
            if s in ('front', 'back'):
                if along_x:
                    hline(p, r, 0, trim)
                    hline(p, r, fh - 1, trim)
                else:
                    vline(p, r, 0, trim if base == BLUE else BLUE)
                    vline(p, r, fw - 1, trim if base == BLUE else BLUE)
        if style in ('lm_ribbon_b', 'lm_streamer_end'):
            for s in ('front', 'back'):
                fx, fy, fw, fh = f[s]
                for i in range(fw):
                    cut = int(4 * (1 - abs(i + 0.5 - fw / 2) / (fw / 2)))
                    for j in range(cut):
                        p.clear((fx + i, fy + fh - 1 - j))

    if style in ('lm_sleeve', 'lm_forearm'):
        lm_silk(p, f)
    if style == 'lm_shoulder':
        p.shaded(f, BLUE_DEEP)
        for s in SIDES:
            hline(p, f[s], f[s][3] - 1, trim)
        fx, fy, fw, fh = f['top']
        for i, j, xy in each(f['top']):
            r = math.hypot((i + 0.5 - fw / 2) / (fw / 2) * 0.9, (j + 0.5 - fh / 2) / (fh / 2))
            if 0.55 < r < 0.75:
                p.px(xy, trim)
            elif r <= 0.55:
                p.px(xy, noisy(p, BLUE, 3))
    if style == 'lm_cuff':
        p.shaded(f, WHITE)
        edges(p, f, trim)
    if style == 'lm_bigsleeve':                                   # the great sleeve: rose, lined in blue, a white border
        lm_silk(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            clouds(p, f[s], trim_deep, every=9, r=1, share=0.6, skip_bottom=8)
            hem_band(p, f[s], 6, WHITE, trim, trim_deep)
            for i in range(1, fw - 3, 6):
                zigzag(p, (fx + i, fy + fh - 6, 3, 5), (120, 180, 250), glow=0.4)
        fx, fy, fw, fh = f['bottom']
        for i, j, xy in each(f['bottom']):
            e = min(i, j, fw - 1 - i, fh - 1 - j)
            p.px(xy, trim if e == 0 else BLUE if e < 3 else BLUE_DEEP)
    if style == 'lm_hand':
        hand(p, f, LADY)

    if style == 'lm_handle':
        metal(p, f, BRONZE, BRONZE_DEEP)
        for s in SIDES:
            for j in range(1, f[s][3], 3):
                hline(p, f[s], j, (170, 40, 40))
    if style in ('lm_mount', 'lm_finial'):
        gold_box(p, f)
    if style == 'lm_mirror':                                      # the bronze mirror: its face polished and shining, a bolt in it; its back cast
        def front(r, a, d):
            if r > 1.0:
                return None
            if r > 0.86:
                return BRONZE_DEEP if r > 0.95 else (GOLD if int((a + math.pi) * 8 / math.pi) % 2 else BRONZE)
            if r > 0.8:
                return BRONZE_DEEP
            t = r / 0.8
            c = mixc((250, 254, 255), (120, 180, 240), t ** 1.3)
            if -1.25 < a < -0.55 and 0.3 < r < 0.62:
                c = (255, 255, 255)
            return ('glow', c, 0.85)

        def back(r, a, d):
            if r > 1.0:
                return None
            if r > 0.88:
                return BRONZE_DEEP if r > 0.95 else BRONZE
            if r < 0.16:
                return GOLD
            if abs(r - 0.48) < 0.05 or (0.55 < r < 0.82 and int((a + math.pi) * 6 / math.pi) % 2 == 0 and abs(r - 0.68) < 0.07):
                return BRONZE_GREEN
            return noisy(p, BRONZE, 4)
        disc(p, f['front'], front)
        disc(p, f['back'], back)
        fx, fy, fw, fh = f['front']                               # a bolt across the glass
        zigzag(p, (fx + fw // 2 - 3, fy + 4, 6, fh - 8), (120, 190, 255), glow=1.0, x0=3)
        bare(p, f)

    # the head
    if style == 'lm_face':
        hair_box(p, f, HAIR)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            for j in range(3, fh):
                for i in range(5):
                    x = fx + (fw - 1 - i if s == 'right' else i)
                    p.px((x, fy + j), k(LADY, 0.92 - 0.03 * i))
            # her hair pieces (片子) down before the ear: a long flat black curl lying on the cheek, its tip
            # turned forward (not an eye: nothing is painted on her face's sides but skin and hair)
            for j in range(3, fh - 2):
                for i in (2, 3):
                    if j > fh - 5 and i == 3:
                        continue
                    x = fx + (fw - 1 - i if s == 'right' else i)
                    p.px((x, fy + j), shade_of(HAIR, 0.6 + 0.3 * ((j + i) % 3)))
            x = fx + (fw - 1 - 1 if s == 'right' else 1)
            p.px((x, fy + fh - 3), shade_of(HAIR, 0.8))
        lm_face_front(p, f['front'])
        p.face(f['bottom'], k(LADY, 0.75), 1.0)
    if style == 'lm_nose':
        skin_box(p, f, LADY)
        p.face(f['bottom'], k(LADY, 0.75), 1.0)
    if style == 'lm_chin':
        skin_box(p, f, LADY)
        fx, fy, fw, fh = f['front']                               # the chin's point, lit
        p.px((fx + fw // 2, fy + fh // 2), mixc(LADY, (255, 248, 244), 0.35))
        p.px((fx + fw // 2 - 1, fy + fh // 2), mixc(LADY, (255, 248, 244), 0.2))
    if style in ('lm_hair_back', 'lm_hair_top', 'lm_bun', 'lm_loop', 'lm_loop_top'):
        hair_box(p, f, HAIR)
        if style == 'lm_hair_top':
            fx, fy, fw, fh = f['front']
            for i in range(fw):                                   # the parting, the hair swept back from her brow
                if abs(i - fw / 2) < 1:
                    vline(p, f['front'], i, shade_of(HAIR, 0.3))
        if style == 'lm_loop_top':
            for s in SIDES:
                hline(p, f[s], f[s][3] - 1, trim)
    if style == 'lm_lock':
        tress(p, f, HAIR, axis='y', up=False, keep=0.6)
    if style == 'lm_ear':
        skin_box(p, f, LADY)
        for s in ('right', 'left'):
            fx, fy, fw, fh = f[s]
            p.px((fx + fw // 2, fy + fh - 1), (250, 250, 255))
    if style == 'lm_crown':                                       # gold, openwork, kingfisher-blue between, pearls
        gold_box(p, f)
        for s in SIDES:
            fx, fy, fw, fh = f[s]
            for i in range(fw):
                if (i // 2) % 3 == 1:
                    for j in range(1, fh - 1):
                        p.px((fx + i, fy + j), (60, 140, 210))
            for i in range(0, fw, 3):
                p.px((fx + i, fy), (250, 250, 255))
    if style == 'lm_crown_front':                                 # a gold flame-plaque, a lightning-jewel in it
        for s in ('front', 'back'):
            fx, fy, fw, fh = f[s]
            def mask(s_, t):
                u = abs(s_ - 0.5) * 2
                return u < 0.3 + 0.7 * t ** 0.8 * (0.85 + 0.15 * math.cos(t * 10))
            cutout(p, f[s], mask, lambda s_, t: mixc(GOLD_LIGHT, GOLD_DEEP, t * 0.7))
            if s == 'front':
                for i, j, xy in each(f[s]):
                    d = math.hypot(i + 0.5 - fw / 2, j + 0.5 - fh * 0.6)
                    if d < 1.8:
                        p.lit(xy, mixc((255, 255, 255), (110, 180, 255), d / 1.8), glow=1.0)
        bare(p, f)
    if style == 'lm_pin':
        metal(p, f, GOLD, GOLD_DEEP)
    if style == 'lm_dangle':
        for s, r in f.items():
            for i, j, xy in each(r):
                p.px(xy, k((250, 250, 255), 1.0 if j % 2 == 0 else 0.6))
