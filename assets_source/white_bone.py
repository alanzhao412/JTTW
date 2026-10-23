"""Lady White Bone's true form (白骨夫人): a towering, gaunt corpse-fiend in a grave shroud. As
fire_court.py: her model and skin come from one list of parts (beastgen): the model is written into
RidgeRenderers.ladyLayer() between its GENERATED markers, the skin painted to the same box-UV layout
at four times the model's resolution (lady_white_bone.png), with a second skin of what glows, drawn
full-bright: her eyes, dim red in black sockets, and an ember of a heart behind her ribs
(lady_white_bone_glow.png).

She is cracked porcelain-white bone: a long skull under a crown of finger bones (a hooked horn of
bone swept back on one side), long ragged black hair, a jaw hung a little open; a jagged ribcage
and a hollow waist bare through a black shroud left open down the front; a necklace of little
skulls; vertebrae standing out through the hair down her back; arms long past her knees, wide
sleeves torn off above the elbow, bone forearms and long clawed fingers black at the tips; and
under a blood-red sash hung with white funeral streamers, tiers of tattered shroud and an
underrobe the colour of old blood ending in rags, the shroud's back trailing behind her.

Run from the repository root: python assets_source/white_bone.py"""
import math

import beastgen as bg
from beastgen import Painter, paint, emit
from fire_court import part, mirrored

bg.UNITS = 128
bg.S = 4
bg.TEX = bg.UNITS * bg.S

JAVA = 'src/main/java/com/jttw/engine/client/renderer/RidgeRenderers.java'

BONE = (234, 230, 220)       # porcelain-white
BONE_SH = (184, 178, 168)
BONE_DK = (112, 104, 98)
CRACK = (44, 36, 42)
VOID = (12, 6, 10)           # the dark inside her
HAIR = (18, 16, 22)
HAIR_HI = (50, 46, 60)
SHROUD = (50, 46, 54)        # the grave shroud: ash-black
SHROUD_DK = (26, 24, 30)
HEMP = (204, 196, 178)       # faded mourning hemp
HEMP_DK = (128, 118, 102)
SASH = (106, 18, 26)
GRAVE_RED = (66, 26, 32)     # an underrobe the colour of old blood
EYE = (104, 14, 18)          # dim, deep red: a smoulder, not a blaze
EYE_HALO = (44, 6, 10)
HEART = (70, 10, 15)

LIGHT = {'top': 1.06, 'bottom': 0.62, 'front': 1.0, 'back': 0.84, 'right': 0.9, 'left': 0.9}

# ── her parts: (name, parent, pivot, rotation, boxes), boxes (x, y, z, w, h, d, style) ──────────
# The ground is at y 24; she faces -z. Torso and skirt hang from her waist (y -3), the torso
# hunched forward from it in the renderer; her head, arms and hair hang from the torso.
Q = math.pi / 2   # a quarter turn: the tatters at her sides hang flat to them
FINGERS = ((-1.0, 3.2, 3.0), (-0.33, 3.8, 3.6), (0.33, 3.6, 3.4), (1.0, 3.0, 2.8))  # x, finger, claw

PARTS = mirrored([
    part('torso', 'root', (0, -3, 0), (0, 0, 0), [
        (-4.5, -11, -3, 9, 9, 6, 'robe'),            # the shroud over her chest, open down the front
        (-3.5, -10.5, -2.5, 7, 8, 5, 'ribs'),        # the ribcage within
        (-2.5, -2.5, -2, 5, 3, 4, 'waist'),          # a hollow waist: the spine bare in the dark
        (-4.5, -13, 3.1, 9, 16, 1, 'hair_long'),     # her hair down her back
        (-1, -14.5, -1.3, 2, 4, 2, 'neck'),
        # a necklace of little skulls, slung low across the ribs
        (-3.9, -10.6, -3.5, 1.4, 1.4, 1, 'bead'),
        (2.5, -10.6, -3.5, 1.4, 1.4, 1, 'bead'),
        (-3.0, -9.5, -3.4, 1.4, 1.4, 1, 'bead'),
        (1.6, -9.5, -3.4, 1.4, 1.4, 1, 'bead'),
        (-1, -8.8, -3.6, 2, 2, 1.2, 'jewel'),
    ]),
] + [
    # vertebrae standing out through her hair down her back, swept up like spines
    part('ridge_%d' % k, 'torso', (0, y, 2.6), (0.5, 0, 0), [(-0.6, -0.7, 0, 1.2, 1.4, l, 'ridge')])
    for k, (y, l) in enumerate(((-11, 4.2), (-8.4, 3.8), (-5.8, 3.3), (-3.2, 2.8), (-0.8, 2.2)))
] + [
    part('head', 'torso', (0, -13.5, -0.5), (0, 0, 0), [
        (-3, -8, -3.5, 6, 8, 7, 'skull'),
        (-3.5, -8.6, -2.9, 7, 7, 7, 'hair_cap'),
        (-4, -9, -4.2, 8, 1, 9, 'band'),             # the crown's band of finger bones
        (-1, -10.6, -4.6, 2, 2, 1.2, 'jewel'),       # a little skull set at its front
        (-3.9, -6, -3.7, 1.3, 18, 1, 'strand_long'), # a long lock down over her right breast
        (2.6, -6, -2.9, 1.3, 11, 1, 'strand'),       # a shorter one at her left cheek
        (-3.3, -7, -3.9, 1.2, 12, 0.4, 'veil_strand'),  # and one fallen across her face
        (-4.2, -7.5, -2, 1, 9, 5, 'hair_side'),      # her hair hanging thick at either side
        (3.2, -7.5, -1, 1, 7, 4, 'hair_side'),
    ]),
    part('jaw', 'head', (0, -0.2, -0.6), (0.16, 0, 0), [(-2, 0, -3, 4, 3, 4, 'jaw')]),
    # the crown: finger bones rising from the band, tallest at the front, flaring out; one broken
] + [p for args in (
        ('crown_0', (0, -9, -3.4), (-0.08, 0, 0), 4, 4, (-0.2, 0, 0)),
        ('crown_1', (-1.9, -9, -3.5), (0, 0, -0.3), 3, 3, (0, 0, -0.3)),
        ('crown_2', (1.9, -9, -3.5), (0.05, 0, 0.26), 3, 2.5, (0, 0, 0.35)),
        ('crown_3', (-3.5, -9, -2), (-0.1, 0, -0.7), 2.5, 2.5, (0, 0, -0.35)),
        ('crown_4', (3.5, -9, -2), (-0.1, 0, 0.6), 2.5, 2, (0, 0, 0.3)),
        ('crown_6', (0, -9, 4.2), (-0.7, 0, 0), 2, 2, (-0.3, 0, 0)))
    for p in (part(args[0], 'head', args[1], args[2], [(-0.6, -args[3], -0.6, 1.2, args[3], 1.2, 'horn')]),
              part(args[0] + '_tip', args[0], (0, -args[3] + 0.2, 0), args[5], [(-0.4, -args[4], -0.4, 0.8, args[4], 0.8, 'spike')]))
] + [
    part('crown_5', 'head', (-3.7, -9, 1.0), (-0.3, 0, -1.0), [(-0.5, -2, -0.5, 1, 2, 1, 'stub')]),  # snapped off
    # a hooked horn of bone, swept back from the left of her crown
    part('horn', 'head', (3.3, -8.6, 1.6), (-0.8, 0, 0.5), [(-0.75, -6, -0.75, 1.5, 6, 1.5, 'horn')]),
    part('horn_tip', 'horn', (0, -5.8, 0), (-0.6, 0, -0.35), [(-0.5, -5, -0.5, 1, 5, 1, 'spike')]),
    # a long, curved bone rising from her left shoulder
    part('shoulder_spike', 'torso', (5.0, -11.6, 0.6), (-0.3, 0, 0.42), [(-0.75, -5, -0.75, 1.5, 5, 1.5, 'horn')]),
    part('shoulder_spike_tip', 'shoulder_spike', (0, -4.8, 0), (-0.45, 0, -0.5), [(-0.5, -4, -0.5, 1, 4, 1, 'spike')]),
    # arms long past the knee: a bone in a wide, torn sleeve, two bones below the elbow, a long hand
    part('right_arm', 'torso', (-5.6, -10.2, 0), (0, 0, 0), [
        (-0.75, 0, -0.75, 1.5, 11, 1.5, 'bone'),
        (-2.5, -1.6, -2.5, 5, 10, 5, 'sleeve'),
        (-1.25, -2.4, -1.25, 2.5, 1, 2.5, 'knob'),
    ]),
    part('right_forearm', 'right_arm', (0, 11, 0), (0, 0, 0), [
        (-0.9, 0, -0.45, 0.8, 10, 0.8, 'bone'),
        (0.1, 0, -0.35, 0.8, 10, 0.8, 'bone'),
        (-1, -0.7, -0.7, 2, 1.4, 1.4, 'knob'),
    ]),
    part('right_hand', 'right_forearm', (0, 10, 0), (0, 0, 0), [(-1.3, 0, -0.7, 2.6, 2.6, 1.4, 'palm')]),
] + [p for k, (x, fl, cl) in enumerate(FINGERS) for p in (
    part('right_finger_%d' % k, 'right_hand', (x, 2.4, 0), (0, 0, 0), [(-0.35, 0, -0.35, 0.7, fl, 0.7, 'finger')]),
    part('right_claw_%d' % k, 'right_finger_%d' % k, (0, fl - 0.1, 0), (0, 0, 0), [(-0.3, 0, -0.3, 0.6, cl, 0.6, 'claw')]))
] + [
    part('right_thumb', 'right_hand', (1.2, 1.0, -0.5), (0, 0, 0), [(-0.35, 0, -0.35, 0.7, 2.6, 0.7, 'finger')]),
    part('right_thumb_claw', 'right_thumb', (0, 2.5, 0), (0, 0, 0), [(-0.3, 0, -0.3, 0.6, 2.4, 0.6, 'claw')]),
    # below the waist: a blood-red sash, then tiers of shroud and hemp, the last trailing behind
    part('skirt', 'root', (0, -3, 0), (0, 0, 0), [
        (-3.5, -0.5, -2.5, 7, 2, 5, 'sash'),
        (-4, 1, -3, 8, 8, 6, 'skirt1'),
        (-5.5, 8, -4.5, 11, 9, 9, 'skirt2'),
        (2.4, 1.4, -3.4, 1.5, 7, 0.4, 'sash_end'),  # its end hanging at her left
    ]),
    # funeral streamers of white hemp, hung from her sash
    part('streamer_0', 'skirt', (1.0, 0.8, -2.7), (-0.27, 0, 0.03), [(-0.8, 0, -0.3, 1.6, 17, 0.3, 'streamer')]),
    part('streamer_1', 'skirt', (-2.3, 0.8, -2.7), (-0.25, 0, -0.05), [(-0.6, 0, -0.3, 1.2, 12, 0.3, 'streamer')]),
    part('hem', 'skirt', (0, 16, 0), (0, 0, 0), [(-7.5, 0, -6, 15, 11, 12, 'skirt3')]),
    # the shroud's back, falling from her waist and widening, to trail on the ground behind her
    part('shroud', 'skirt', (0, 0.5, 2.6), (0.27, 0, 0), [(-5, 0, -0.5, 10, 15, 1, 'shroud')]),
    part('shroud_low', 'shroud', (0, 15, 0), (0.13, 0, 0), [(-7, -0.2, -0.5, 14, 12.8, 1, 'shroud_low')]),
] + [
    # tatters hanging from it, longer than its hem, so it ends in points
    part('tatter_%d' % k, 'hem', (x, 1, z), rot, [(-0.8, 0, -0.2, 1.6, h, 0.4, 'tatter')])
    for k, (x, z, rot, h) in enumerate((
        (-4.5, -6.2, (-0.06, 0, 0.02), 9.5), (2.0, -6.2, (-0.05, 0, -0.03), 8.5), (5.8, -6.2, (-0.07, 0, -0.05), 9),
        (-7.7, -2.5, (0, Q, 0.07), 9), (-7.7, 3.0, (0, Q, 0.06), 8.5), (7.7, 0.5, (0, Q, -0.07), 9.5)))
])


# ── painting ─────────────────────────────────────────────────────────────

def clear(p, x, y):
    if 0 <= x < bg.TEX and 0 <= y < bg.TEX:
        p.img.putpixel((int(x), int(y)), (0, 0, 0, 0))
        p.glow.putpixel((int(x), int(y)), (0, 0, 0, 0))


def ragged(p, rect, depth, phase=0.0, dark=None):
    """Tears the bottom of a face into tatters: each column cut away by up to `depth` pixels, in
    uneven points and the odd deep rent, and darkened just above where it is torn."""
    fx, fy, fw, fh = rect
    depth = min(depth, fh - 1)
    for i in range(fw):
        w = 0.5 + 0.5 * math.sin(i * 1.3 + phase) * math.sin(i * 0.37 + phase * 2)
        cut = int(depth * (0.25 + 0.75 * abs(w)) * p.rnd.uniform(0.55, 1.0))
        if p.rnd.random() < 0.08:
            cut = depth
        for j in range(cut):
            clear(p, fx + i, fy + fh - 1 - j)
        if dark:
            for j in range(cut, min(fh, cut + 3)):
                p.px((fx + i, fy + fh - 1 - j), [c * (0.7 + 0.1 * (j - cut)) for c in dark])


def cloth(p, rect, base, light, folds=6, noise=4):
    """Cloth hanging in folds: soft vertical bands of light and shadow, uneven."""
    fx, fy, fw, fh = rect
    phase = p.rnd.uniform(0, 6)
    for i in range(fw):
        band = 0.86 + 0.14 * math.sin(i * 2 * math.pi / max(3, folds) + phase) + p.rnd.uniform(-0.03, 0.03)
        for j in range(fh):
            k = light * band * (1 - 0.18 * j / max(1, fh - 1))
            n = p.rnd.randint(-noise, noise)
            p.px((fx + i, fy + j), [c * k + n for c in base])


def stain(p, rect, colour, share=0.35, from_bottom=True):
    """Grave dirt soaking up from the bottom of a face, blotchy."""
    fx, fy, fw, fh = rect
    for i in range(fw):
        reach = int(fh * share * p.rnd.uniform(0.5, 1.2))
        for j in range(reach):
            y = fy + fh - 1 - j if from_bottom else fy + j
            if p.rnd.random() < 0.85 - 0.5 * j / max(1, reach):
                c = p.img.getpixel((fx + i, y))
                if c[3]:
                    p.px((fx + i, y), [a * 0.55 + b * 0.45 for a, b in zip(c, colour)])


def trim(p, rect, colour, rows, at_bottom=True):
    """A band of faded mourning hemp along a face's edge."""
    fx, fy, fw, fh = rect
    for j in range(rows):
        y = fy + fh - 1 - j - 6 if at_bottom else fy + j
        for i in range(fw):
            p.px((fx + i, y), [c * (0.9 + 0.1 * p.rnd.random()) for c in colour])


def bone(p, f, base=BONE, joints=0, dark_end=None):
    """Bone: porcelain-white, shaded for its form, with `joints` dark knuckle rings along it; with
    dark_end ('top' or 'bottom'), aged dark toward that end, black at its point."""
    for k, r in f.items():
        fx, fy, fw, fh = r
        for j in range(fh):
            t = j / max(1, fh - 1)
            for i in range(fw):
                k2 = LIGHT[k] * (1 - 0.08 * abs(i - (fw - 1) / 2) / max(1, fw / 2))
                c = [v * k2 + p.rnd.randint(-3, 3) for v in base]
                if dark_end and k not in ('top', 'bottom'):
                    e = (1 - t) if dark_end == 'top' else t
                    if e > 0.55:
                        m = min(1, (e - 0.55) / 0.45)
                        c = [a * (1 - m) + b * m for a, b in zip(c, (34, 26, 28))]
                if joints and k not in ('top', 'bottom') and (j + 1) % max(4, fh // (joints + 1)) == 0:
                    c = [v * 0.62 for v in c]
                p.px((fx + i, fy + j), c)
    if dark_end:
        p.face(f[dark_end], (30, 24, 26), 1.0, noise=2)


def hair(p, f, depth=0, light=1.0):
    """Long black hair: dark strands with a cold sheen, the ends ragged."""
    for k, r in f.items():
        fx, fy, fw, fh = r
        for i in range(fw):
            hi = p.rnd.random() < 0.22
            for j in range(fh):
                k2 = LIGHT[k] * light * (1 - 0.25 * j / max(1, fh - 1))
                base = HAIR_HI if (hi and (j + i) % 7 < 4) else HAIR
                n = p.rnd.randint(-3, 3)
                p.px((fx + i, fy + j), [c * k2 + n for c in base])
        if depth and k not in ('top', 'bottom'):
            ragged(p, r, depth, phase=fx * 0.1)


def tiny_skull(p, r, glow_eyes=False):
    """A little skull's face, in an 8- or so pixel square."""
    fx, fy, fw, fh = r
    p.face(r, BONE, 1.0, noise=3)
    s = fw / 8
    ey = fy + int(2.5 * s)
    for ex in (fx + int(1 * s), fx + fw - int(1 * s) - max(2, int(2 * s))):
        for i in range(max(2, int(2 * s))):
            for j in range(max(2, int(2 * s))):
                p.px((ex + i, ey + j), VOID)
        if glow_eyes:
            p.px((ex + max(2, int(2 * s)) // 2, ey + max(2, int(2 * s)) // 2), EYE_HALO, glow=True)
    cx = fx + fw // 2
    p.px((cx - 1, ey + int(2.5 * s)), VOID)
    p.px((cx, ey + int(2.5 * s)), VOID)
    for i in range(int(1.5 * s), fw - int(1.5 * s)):
        if i % 2 == 0:
            p.px((fx + i, fy + fh - 2), BONE_DK)
    for i in (0, fw - 1):
        clear(p, fx + i, fy + fh - 1)


def paint_skull(p, f):
    bone(p, f)
    fx, fy, fw, fh = f['front']                       # 24 x 32
    # hollows: the temples, under the cheekbones, the cheeks drawn in toward a narrow chin
    for j in range(fh):
        for i in range(fw):
            edge = min(i, fw - 1 - i)
            k = 1.0
            if edge < 2:
                k *= 0.82
            if 19 <= j <= 26 and 2 <= edge <= 6:
                k *= 0.74 + 0.04 * abs(edge - 4)
            if j >= 24 and edge < 4 - (j - 24) // 3:
                k *= 0.7
            if k < 1:
                c = p.img.getpixel((fx + i, fy + j))
                p.px((fx + i, fy + j), [v * k for v in c[:3]])
    # a heavy brow over the sockets, the cheekbones high and sharp
    for i in range(2, fw - 2):
        p.px((fx + i, fy + 9), [v * 1.04 for v in BONE])
    for i in list(range(2, 8)) + list(range(fw - 8, fw - 2)):
        p.px((fx + i, fy + 18), [min(255, v * 1.06) for v in BONE])
        p.px((fx + i, fy + 19), BONE_SH)
    # the sockets: deep and black, slanting steeply down to the nose in a glare; in each, a dim
    # red light low and near the inner corner; black streaks running down from them like tears
    for side in (0, 1):
        for i in range(8):
            col = 2 + i if side == 0 else fw - 3 - i      # outer corner to inner
            top = 10 + (i * 5) // 7
            bottom = 15 + (i * 3) // 7
            for j in range(top, bottom + 1):
                p.px((fx + col, fy + j), VOID)
            p.px((fx + col, fy + top - 1), [v * 0.45 for v in BONE_SH])   # the brow's shadow
            p.px((fx + col, fy + bottom + 1), [v * 0.8 for v in BONE_SH])
        cx = fx + (6 if side == 0 else fw - 8)
        for (i, j) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            p.px((cx + i, fy + 14 + j), EYE, glow=True)
        for (i, j) in ((-1, 0), (2, 0), (0, -1), (1, -1)):
            p.px((cx + i, fy + 14 + j), EYE_HALO, glow=True)
        tx = fx + (5 if side == 0 else fw - 6)
        for j in range(17, 27):
            x = tx + (1 if (j > 21 and side == 0) else 0) - (1 if (j > 23 and side == 1) else 0)
            p.px((x, fy + j), [v * (0.25 + 0.04 * (j - 17)) for v in BONE])
    # the nose: a narrow black notch
    cx = fx + fw // 2
    for j, half in ((19, 0), (20, 1), (21, 1), (22, 1), (23, 0)):
        for i in range(-half - 1, half + 1):
            p.px((cx + i, fy + j), VOID)
    # long upper teeth under a thin, dark lip
    for i in range(5, fw - 5):
        p.px((fx + i, fy + 27), (60, 48, 50))
        for j in range(28, 32):
            p.px((fx + i, fy + j), (40, 28, 32) if i % 2 == 0 else [v * (1.0 - 0.06 * (j - 28)) for v in BONE])
    # a crack from her crown down through her left socket and across the cheek, and its branches;
    # and a shorter one above her right brow
    def crack(x, y, steps, drift, rnd_turn=0.35):
        for _ in range(steps):
            p.px((fx + x, fy + y), CRACK)
            if 0 <= x + 1 < fw:
                c = p.img.getpixel((fx + x + 1, fy + y))
                if c[:3] != VOID:
                    p.px((fx + x + 1, fy + y), [min(255, v * 1.05) for v in BONE])
            y += 1
            if p.rnd.random() < rnd_turn:
                x += drift if p.rnd.random() < 0.75 else -drift
            x = max(0, min(fw - 1, x))
            if y >= fh:
                break
    p.rnd.seed(41)
    crack(15, 0, 26, 1)
    crack(16, 6, 5, -1)
    crack(17, 20, 6, 1, 0.6)
    crack(6, 2, 7, -1)
    # the crack runs over the top of her skull too
    tx, ty, tw, th = f['top']
    for j in range(th):
        p.px((tx + 15 + (j % 3 == 0), ty + th - 1 - j), CRACK)
    # the sockets show from the side as a dark notch
    for k in ('right', 'left'):
        sx, sy, sw, sh = f[k]
        x0 = sx + sw - 2 if k == 'right' else sx
        for j in range(12, 17):
            p.px((x0, sy + j), VOID)
            p.px((x0 + (1 if k == 'right' else 0), sy + j), VOID)


def paint_jaw(p, f):
    """A long jaw narrowing to a pointed chin; a row of teeth along its top, two of them fangs."""
    bone(p, f)
    fx, fy, fw, fh = f['front']                       # 16 x 12
    for j in range(fh):
        for i in range(fw):
            edge = min(i, fw - 1 - i)
            if j >= 4 and edge < (j - 4) * 5 // 7 + 1:
                clear(p, fx + i, fy + j)
            elif j >= 4 and edge < (j - 4) * 5 // 7 + 2:
                p.px((fx + i, fy + j), BONE_SH)
    for i in range(1, fw - 1):
        for j in range(0, 3):
            p.px((fx + i, fy + j), (40, 28, 32) if i % 2 else [v * (1 - 0.05 * j) for v in BONE])
    for x in (3, fw - 5):                             # the fangs, long and pointed, black in the gaps
        for j in range(0, 6):
            p.px((fx + x, fy + j), BONE)
            if j < 4:
                p.px((fx + x + 1, fy + j), BONE_SH)
        p.px((fx + x - 1, fy + 3), (40, 28, 32))
    for k in ('right', 'left'):
        sx, sy, sw, sh = f[k]
        for j in range(5, sh):
            for d in range(min(sw, (j - 4) // 2)):
                clear(p, sx + d if k == 'right' else sx + sw - 1 - d, sy + j)
    p.face(f['bottom'], BONE_SH, 0.7, noise=3)


def paint_ribs(p, f):
    """Ribs over a black hollow, an ember of a heart glowing dim between them; one rib broken."""
    for k, r in f.items():
        fx, fy, fw, fh = r
        for j in range(fh):
            for i in range(fw):
                p.px((fx + i, fy + j), [v + p.rnd.randint(0, 4) for v in VOID])
    fx, fy, fw, fh = f['front']                       # 28 x 32
    cx = fw / 2 - 0.5
    # the heart's light, faint, spreading between the ribs
    for j in range(fh):
        for i in range(fw):
            d = math.hypot((i - cx - 2) / 1.2, j - 13)
            if d < 7:
                t = (1 - d / 7) ** 1.6
                p.px((fx + i, fy + j), [v * t for v in HEART], glow=True)
    # the sternum
    for j in range(1, 19):
        for i in (-1, 0, 1):
            p.px((fx + int(cx) + i, fy + j), [v * (1.0 if i == 0 else 0.85) for v in BONE])
    # ribs: curving out and down from it, thinner and shorter toward the bottom
    for n, y0 in enumerate((2, 7, 12, 17, 22, 26)):
        for side in (-1, 1):
            length = 13 - (n // 2)
            broken = (n == 2 and side == 1)
            for t in range(2, length):
                if broken and 6 <= t < 9:
                    continue
                x = int(cx + side * t)
                y = y0 + int(t * t * 0.035) + t // 4
                thick = 3 if n < 4 else 2
                for d in range(thick):
                    if 0 <= x < fw and 0 <= y + d < fh:
                        k = 1.05 if d == 0 else (0.82 if d == thick - 1 else 0.95)
                        p.px((fx + x, fy + y + d), [v * k for v in BONE])
            if broken:                                # a jagged end where it snapped
                p.px((fx + int(cx + 9), fy + y0 + 4), BONE)
                p.px((fx + int(cx + 9), fy + y0 + 5), BONE_SH)
    # the ribs wrap round her sides
    for k in ('right', 'left'):
        sx, sy, sw, sh = f[k]
        for y0 in (3, 8, 13, 18, 23):
            for i in range(sw):
                y = y0 + (sw - i) // 4 if k == 'right' else y0 + i // 4
                for d in range(2):
                    p.px((sx + i, sy + y + d), [v * (0.92 - 0.12 * d) for v in BONE])
    # the spine behind
    bx, by, bw, bh = f['back']
    for j in range(bh):
        for i in range(-2, 2):
            p.px((bx + bw // 2 + i, by + j), [v * (0.6 if j % 4 == 3 else 0.9) for v in BONE])


def paint_waist(p, f):
    """Her waist is nothing but spine in the dark, the shroud torn about it."""
    for k, r in f.items():
        p.face(r, VOID, 1.0, noise=2)
    fx, fy, fw, fh = f['front']                       # 20 x 12
    for j in range(fh):
        for i in range(fw // 2 - 2, fw // 2 + 2):
            p.px((fx + i, fy + j), [v * (0.55 if j % 4 == 3 else (1.0 if 0 < i - fw // 2 + 2 < 3 else 0.85)) for v in BONE])
        # the ragged edges of the shroud, closing in at the sides
        reach = 4 + int(2 * math.sin(j * 1.7)) + p.rnd.randint(0, 1)
        for i in range(reach):
            p.px((fx + i, fy + j), [v * 0.9 for v in SHROUD])
            p.px((fx + fw - 1 - i, fy + j), [v * 0.9 for v in SHROUD])
    for k in ('right', 'left', 'back'):
        cloth(p, f[k], SHROUD, LIGHT[k])


def paint_robe(p, f):
    """The shroud over her chest, open from the throat down in a V to show the ribs; edged in
    faded hemp, torn here and there."""
    for k, r in f.items():
        cloth(p, r, SHROUD, LIGHT[k], folds=7)
    fx, fy, fw, fh = f['front']                       # 36 x 36
    cx = fw / 2 - 0.5
    for j in range(fh):
        half = 12.5 - 9.5 * (j / (fh - 1)) + 0.8 * math.sin(j * 0.9)
        for i in range(fw):
            d = abs(i - cx)
            if d < half:
                clear(p, fx + i, fy + j)
            elif d < half + 2:
                p.px((fx + i, fy + j), [v * (0.85 + 0.12 * p.rnd.random()) for v in HEMP])
            elif d < half + 3:
                p.px((fx + i, fy + j), [v * 0.6 for v in SHROUD])
    # its shoulders, worn through over the bone
    tx, ty, tw, th = f['top']
    for j in range(th):
        for i in range(tw):
            if abs(i - tw / 2 + 0.5) < 6 and j > th / 3:
                clear(p, tx + i, ty + j)
    # rents in its sides and back
    for k in ('right', 'left', 'back'):
        sx, sy, sw, sh = f[k]
        for _ in range(2):
            x, y = sx + p.rnd.randint(2, sw - 3), sy + p.rnd.randint(8, sh - 6)
            for j in range(p.rnd.randint(3, 6)):
                p.px((x + (j // 3), y + j), [v * 0.4 for v in SHROUD_DK])
    # down her back, the shroud split over the spine
    bx, by, bw, bh = f['back']
    for j in range(bh):
        for i in (-1, 0):
            p.px((bx + bw // 2 + i, by + j), VOID)
    for k in ('right', 'left', 'back'):
        ragged(p, f[k], 3, dark=SHROUD_DK)


def paint_sleeve(p, f):
    for k, r in f.items():
        if k == 'bottom':
            fx, fy, fw, fh = r
            for j in range(fh):
                for i in range(fw):
                    clear(p, fx + i, fy + j)
            continue
        cloth(p, r, SHROUD, LIGHT[k], folds=5)
        if k != 'top':
            trim(p, r, HEMP, 3)
            stain(p, r, (60, 44, 40), 0.25)
            ragged(p, r, 12, phase=r[0] * 0.3, dark=SHROUD_DK)


def paint_skirt(p, f, base, depth, dirt, edge=None, top_clear=False):
    for k, r in f.items():
        if k == 'bottom':
            p.face(r, [v * 0.4 for v in base], 1.0, noise=2)
            continue
        if k == 'top':
            p.face(r, [v * 0.55 for v in base], 1.0, noise=3)
            continue
        cloth(p, r, base, LIGHT[k], folds=8)
        if edge:
            trim(p, r, edge, 3)
        stain(p, r, dirt, 0.4)
        # a few long rents down it
        fx, fy, fw, fh = r
        for _ in range(max(1, fw // 18)):
            x, y = fx + p.rnd.randint(3, fw - 4), fy + p.rnd.randint(fh // 4, fh // 2)
            for j in range(p.rnd.randint(fh // 4, fh // 2)):
                p.px((x + (j // 5) % 2, y + j), VOID)
        ragged(p, r, depth, phase=fx * 0.21, dark=[v * 0.6 for v in base])


def paint_box(p, style, f, dims):
    if style == 'skull':
        paint_skull(p, f)
    elif style == 'jaw':
        paint_jaw(p, f)
    elif style == 'hair_cap':
        hair(p, f)
        tx, ty, tw, th = f['top']                     # her parting
        for j in range(th):
            p.px((tx + tw // 2 - 3, ty + j), [v * 0.5 for v in HAIR])
    elif style == 'hair_long':
        hair(p, f, depth=22)
    elif style == 'strand_long':
        hair(p, f, depth=20, light=1.1)
    elif style == 'strand':
        hair(p, f, depth=14, light=1.1)
    elif style == 'veil_strand':
        hair(p, f, depth=18, light=1.15)
    elif style == 'hair_side':
        hair(p, f, depth=12)
    elif style == 'streamer':                         # white hemp, stained, frayed to rags
        for k, r in f.items():
            cloth(p, r, HEMP, LIGHT[k], folds=3, noise=5)
            if k not in ('top', 'bottom'):
                stain(p, r, (96, 84, 70), 0.3)
                ragged(p, r, 14, phase=r[0] * 0.7)
    elif style == 'tatter':
        for k, r in f.items():
            cloth(p, r, SHROUD, LIGHT[k] * 0.9, folds=3)
            if k not in ('top', 'bottom'):
                stain(p, r, (58, 44, 36), 0.4)
                ragged(p, r, 16, phase=r[0] * 0.5)
    elif style == 'band':
        bone(p, f, BONE_SH)
        # a ring of finger bones, lashed side by side
        for k, r in f.items():
            if k in ('top', 'bottom'):
                p.face(r, BONE_DK, 0.9, noise=3)
                continue
            fx, fy, fw, fh = r
            for i in range(fw):
                knuckle = i % 4 == 3
                for j in range(fh):
                    c = BONE_DK if knuckle else [v * (1.0 - 0.2 * abs(j - fh / 2) / fh) for v in BONE]
                    p.px((fx + i, fy + j), [v * LIGHT[k] for v in c])
            for i in range(fw):
                p.px((fx + i, fy), (40, 30, 30))
                p.px((fx + i, fy + fh - 1), (40, 30, 30))
    elif style in ('jewel', 'bead'):
        bone(p, f)
        tiny_skull(p, f['front'], glow_eyes=style == 'jewel')
    elif style == 'spike':
        bone(p, f, joints=max(1, dims[1] // 2), dark_end='top')
    elif style == 'stub':                             # snapped off: a jagged, dark break at its top
        bone(p, f, joints=1)
        for k in ('right', 'front', 'left', 'back'):
            fx, fy, fw, fh = f[k]
            for i in range(fw):
                for j in range(1 + (i * 5 % 3)):
                    clear(p, fx + i, fy + j)
        p.face(f['top'], (60, 50, 50), 1.0, noise=3)
    elif style == 'horn':
        bone(p, f, joints=2)
    elif style == 'ridge':
        bone(p, f)
        p.face(f['back'], (52, 44, 44), 1.0, noise=3)
        for k in ('right', 'left'):
            fx, fy, fw, fh = f[k]
            for j in range(fh):
                for i in range(fw):
                    t = (fw - 1 - i) / max(1, fw - 1) if k == 'right' else i / max(1, fw - 1)
                    if t > 0.6:
                        p.px((fx + i, fy + j), [v * (1.2 - t) for v in BONE])
    elif style == 'neck':
        bone(p, f, joints=3)
    elif style in ('bone', 'knob'):
        bone(p, f, joints=1 if dims[1] > 4 else 0)
    elif style == 'palm':
        bone(p, f)
        fx, fy, fw, fh = f['front']
        for i in range(0, fw, 3):
            p.px((fx + i, fy + fh - 2), BONE_DK)
    elif style == 'finger':
        bone(p, f, joints=1)
    elif style == 'claw':
        bone(p, f, dark_end='bottom')
    elif style == 'robe':
        paint_robe(p, f)
    elif style == 'ribs':
        paint_ribs(p, f)
    elif style == 'waist':
        paint_waist(p, f)
    elif style == 'sleeve':
        paint_sleeve(p, f)
    elif style == 'sash':
        for k, r in f.items():
            cloth(p, r, SASH, LIGHT[k], folds=3)
        fx, fy, fw, fh = f['front']                   # the knot, at her left
        for j in range(fh):
            for i in range(fw - 10, fw - 4):
                p.px((fx + i, fy + j), [v * (0.7 + 0.3 * ((i + j) % 3 == 0)) for v in SASH])
    elif style == 'sash_end':
        for k, r in f.items():
            cloth(p, r, SASH, LIGHT[k] * 0.95, folds=3)
            if k not in ('top', 'bottom'):
                ragged(p, r, 8, phase=r[0])
    elif style == 'skirt1':
        paint_skirt(p, f, SHROUD, 8, (40, 30, 30))
    elif style == 'skirt2':
        paint_skirt(p, f, GRAVE_RED, 14, (30, 16, 18), edge=HEMP_DK)
    elif style == 'skirt3':
        paint_skirt(p, f, SHROUD, 16, (58, 44, 36), edge=HEMP_DK)
    elif style in ('shroud', 'shroud_low'):
        for k, r in f.items():
            cloth(p, r, SHROUD, LIGHT[k] * (0.92 if k == 'back' else 1.0), folds=10)
            if k not in ('top', 'bottom') and style == 'shroud_low':
                trim(p, r, HEMP_DK, 3)
                stain(p, r, (58, 44, 36), 0.45)
                ragged(p, r, 22, phase=r[0] * 0.13, dark=SHROUD_DK)
        # rents through it
        for k in ('front', 'back'):
            fx, fy, fw, fh = f[k]
            for _ in range(3):
                x, y = fx + p.rnd.randint(3, fw - 4), fy + p.rnd.randint(4, fh // 2)
                for j in range(p.rnd.randint(5, 12)):
                    if j % 4 != 3:
                        clear(p, x + (j // 4), y + j)
    else:
        raise ValueError(style)


if __name__ == '__main__':
    p = Painter(13)
    slots = paint(PARTS, p, paint_box)
    p.img.save('src/main/resources/assets/jttw_engine/textures/entity/lady_white_bone.png')
    p.glow.save('src/main/resources/assets/jttw_engine/textures/entity/lady_white_bone_glow.png')
    emit(PARTS, slots, JAVA, 'lady')
    print('lady_white_bone', len(PARTS), 'parts,', len(slots), 'boxes')
